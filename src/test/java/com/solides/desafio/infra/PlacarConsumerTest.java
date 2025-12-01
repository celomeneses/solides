package com.solides.desafio.infra;

import com.rabbitmq.client.*;
import com.solides.desafio.infra.rabbitmq.PlacarConsumer;
import com.solides.desafio.infra.redis.RedisClientProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlacarConsumerTest {

    @Mock
    RedisClientProvider redisProvider;

    @Mock
    Jedis jedis;

    @Test
    void init_shouldDeclareQueue_and_consume_and_handleDelivery_shouldUseRedis() throws Exception {
        Connection connMock = mock(Connection.class);
        Channel channelMock = mock(Channel.class);

        when(redisProvider.getJedis()).thenReturn(jedis);

        AtomicReference<Consumer> captured = new AtomicReference<>();

        try (MockedConstruction<ConnectionFactory> mocked = mockConstruction(ConnectionFactory.class,
                (factoryMock, context) -> {
                    try {
                        when(factoryMock.newConnection()).thenReturn(connMock);
                        when(connMock.createChannel()).thenReturn(channelMock);
                    } catch (IOException | TimeoutException e) {
                        throw new RuntimeException(e);
                    }
                })) {

            // capture the consumer passed to basicConsume
            doAnswer(invocation -> {
                // args: queue, autoAck, consumer
                String queue = invocation.getArgument(0);
                boolean autoAck = invocation.getArgument(1);
                Consumer consumer = invocation.getArgument(2);
                // store consumer for later use
                captured.set(consumer);
                // return a consumer tag as basicConsume would
                return "ctag-1";
            }).when(channelMock).basicConsume(eq("placar_eventos"), eq(true), any(Consumer.class));

            PlacarConsumer consumerInstance = new PlacarConsumer(redisProvider);

            // call init which will use the mocked ConnectionFactory
            consumerInstance.init();

            verify(channelMock, times(1)).queueDeclare(eq("placar_eventos"), eq(true), eq(false), eq(false), isNull());
            verify(channelMock, times(1)).basicConsume(eq("placar_eventos"), eq(true), any(Consumer.class));

            // get captured consumer
            Consumer capturedConsumer = captured.get();
            assert capturedConsumer != null;

            // simulate delivery
            Envelope env = mock(Envelope.class);
            AMQP.BasicProperties props = mock(AMQP.BasicProperties.class);
            String message = "{\"hashId\":\"abc123\",\"lado\":\"casa\",\"pontosCasa\":1}";
            byte[] body = message.getBytes(StandardCharsets.UTF_8);

            // invoke handler
            capturedConsumer.handleDelivery("ctag", env, props, body);

            // verify redis usage
            verify(redisProvider, atLeastOnce()).getJedis();
            // in our PlacarConsumer example we used jedis.set("placar_eventos_last", msg)
            verify(jedis, atLeastOnce()).set(eq("placar_eventos_last"), eq(message));
        }
    }
}

package com.solides.desafio.infra;

import com.rabbitmq.client.*;
import com.solides.desafio.infra.rabbitmq.PlacarConsumer;
import com.solides.desafio.infra.redis.RedisClientProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for PlacarConsumer.
 * - Mocks construction of ConnectionFactory so we don't need a real RabbitMQ.
 * - Captures the Consumer passed to basicConsume and triggers handleDelivery to test Redis usage.
 */
@ExtendWith(MockitoExtension.class)
class PlacarConsumerTest {

    @Mock
    RedisClientProvider redisProvider;

    @Mock
    Jedis jedisMock;

    @Test
    void init_deveDeclararFila_e_consumir_e_handleDelivery_deveChamarRedis() throws Exception {
        Connection connMock = mock(Connection.class);
        Channel channelMock = mock(Channel.class);

        when(redisProvider.getJedis()).thenReturn(jedisMock);

        ArgumentCaptor<Consumer> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);

        try (MockedConstruction<ConnectionFactory> mocked = mockConstruction(ConnectionFactory.class,
                (factoryMock, context) -> {
                    when(factoryMock.newConnection()).thenReturn(connMock);

                    try {
                        when(connMock.createChannel()).thenReturn(channelMock);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })) {

            doReturn("ctag-1").when(channelMock).basicConsume(eq("placar_eventos"), eq(true), any(Consumer.class));

            PlacarConsumer consumerInstance = new PlacarConsumer();
            Field redisField = PlacarConsumer.class.getDeclaredField("redisProvider");
            redisField.setAccessible(true);
            redisField.set(consumerInstance, redisProvider);

            consumerInstance.init();

            verify(channelMock, times(1)).queueDeclare(eq("placar_eventos"), eq(true), eq(false), eq(false), isNull());

            reset(channelMock);
            doAnswer(invocation -> {
                Consumer captured = invocation.getArgument(2);
                return "ctag-1";
            }).when(channelMock).basicConsume(eq("placar_eventos"), eq(true), consumerCaptor.capture());

            consumerInstance.init();

            Consumer capturedConsumer = consumerCaptor.getValue();
            assertNotNull(capturedConsumer, "Esperava um Consumer capturado pelo basicConsume");

            Envelope env = mock(Envelope.class);
            AMQP.BasicProperties props = mock(AMQP.BasicProperties.class);

            String message = "{\"hashId\":\"abc123\",\"lado\":\"casa\",\"pontosCasa\":1,\"pontosVisitante\":0}";
            byte[] body = message.getBytes(StandardCharsets.UTF_8);

            capturedConsumer.handleDelivery("ctag", env, props, body);

            verify(redisProvider, atLeastOnce()).getJedis();

            verify(jedisMock, atLeastOnce()).close();
        }
    }
}


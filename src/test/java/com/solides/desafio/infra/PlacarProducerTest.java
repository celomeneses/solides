package com.solides.desafio.infra;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.solides.desafio.infra.rabbitmq.PlacarProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PlacarProducer.
 * - Mock construction of ConnectionFactory so we don't need a real RabbitMQ.
 * - Verify queueDeclare in init(), basicPublish in enviarEvento() and close() behavior.
 */
@ExtendWith(MockitoExtension.class)
class PlacarProducerTest {

    @Test
    void init_shouldDeclareQueue_and_enviarEvento_shouldPublish_and_close_shouldCloseResources() throws Exception {
        Connection connMock = mock(Connection.class);
        Channel channelMock = mock(Channel.class);

        try (MockedConstruction<ConnectionFactory> mocked = mockConstruction(ConnectionFactory.class,
                (factoryMock, context) -> {
                    when(factoryMock.newConnection()).thenReturn(connMock);
                    when(connMock.createChannel()).thenReturn(channelMock);
                })) {

            doNothing().when(channelMock).basicPublish(anyString(), anyString(), isNull(), any(byte[].class));

            PlacarProducer producer = new PlacarProducer();
            producer.init();

            verify(channelMock, times(1)).queueDeclare(eq("placar_eventos"), eq(true), eq(false), eq(false), isNull());

            String json = "{\"hashId\":\"abc123\",\"lado\":\"casa\",\"pontosCasa\":1}";
            producer.enviarEvento(json);

            ArgumentCaptor<byte[]> bodyCaptor = ArgumentCaptor.forClass(byte[].class);
            verify(channelMock, times(1)).basicPublish(eq(""), eq("placar_eventos"), isNull(), bodyCaptor.capture());

            String sent = new String(bodyCaptor.getValue(), StandardCharsets.UTF_8);
            assertEquals(json, sent);

            producer.close();
            verify(channelMock, times(1)).close();
            verify(connMock, times(1)).close();
        }
    }

    @Test
    void enviarEvento_shouldPropagateIOException_whenBasicPublishThrows() throws Exception {
        Connection connMock = mock(Connection.class);
        Channel channelMock = mock(Channel.class);

        try (MockedConstruction<ConnectionFactory> mocked = mockConstruction(ConnectionFactory.class,
                (factoryMock, context) -> {
                    when(factoryMock.newConnection()).thenReturn(connMock);
                    when(connMock.createChannel()).thenReturn(channelMock);
                })) {

            doThrow(new IOException("publish failed")).when(channelMock).basicPublish(anyString(), anyString(), isNull(), any(byte[].class));

            PlacarProducer producer = new PlacarProducer();
            producer.init();

            IOException ex = assertThrows(IOException.class, () -> producer.enviarEvento("{\"x\":1}"));
            assertTrue(ex.getMessage().contains("publish failed"));

            producer.close();
            verify(channelMock, times(1)).close();
            verify(connMock, times(1)).close();
        }
    }
}


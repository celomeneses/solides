package com.solides.desafio.infra.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * PlacarProducer robusto: init tolerante a falhas e toggle via property.
 */
@Component
public class PlacarProducer {
    private Connection connection;
    private Channel channel;

    @Value("${rabbit.enabled:true}")
    private boolean rabbitEnabled;

    @Value("${rabbit.host:rabbitmq}")
    private String rabbitHost;

    @Value("${rabbit.queue:placar_eventos}")
    private String queueName;

    @PostConstruct
    public void init() {
        if (!rabbitEnabled) {
            System.out.println("RabbitMQ disabled by configuration (rabbit.enabled=false). Skipping init.");
            return;
        }

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(rabbitHost);
            // se precisar, configure username/password aqui:
            // factory.setUsername(...); factory.setPassword(...);

            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.queueDeclare(queueName, true, false, false, null);
            System.out.println("PlacarProducer connected to RabbitMQ at " + rabbitHost);
        } catch (IOException | TimeoutException ex) {
            // NÃO lançar: apenas logar e manter application up.
            System.err.println("Aviso: não foi possível inicializar RabbitMQ (PlacarProducer). " +
                    "Eventos não serão enviados. Causa: " + ex.getMessage());
            // garantir que recursos parcialmente abertos sejam fechados
            safeCloseChannel();
            safeCloseConnection();
            channel = null;
            connection = null;
        } catch (Exception ex) {
            System.err.println("Erro inesperado ao inicializar PlacarProducer: " + ex.getMessage());
            safeCloseChannel();
            safeCloseConnection();
            channel = null;
            connection = null;
        }
    }

    /**
     * Tenta enviar evento. Se Rabbit não estiver disponível, apenas loga.
     * Se preferir falhar rápido, substitua o comportamento por lançar exceção.
     */
    public void enviarEvento(String json) throws IOException {
        if (!rabbitEnabled) {
            // comportamento: ignorar envios se desabilitado
            System.out.println("RabbitMQ disabled - evento descartado: " + json);
            return;
        }

        if (channel == null || connection == null) {
            // decidir comportamento: aqui apenas logamos — não interromper aplicação.
            System.err.println("Aviso: RabbitMQ não disponível. Evento descartado: " + json);
            return;
        }

        // channel.basicPublish pode lançar IOException
        channel.basicPublish("", queueName, null, json.getBytes(StandardCharsets.UTF_8));
    }

    @PreDestroy
    public void close() {
        safeCloseChannel();
        safeCloseConnection();
    }

    private void safeCloseChannel() {
        try {
            if (channel != null && channel.isOpen()) channel.close();
        } catch (Exception e) {
            // log e ignorar
            System.err.println("Erro ao fechar channel RabbitMQ: " + e.getMessage());
        } finally {
            channel = null;
        }
    }

    private void safeCloseConnection() {
        try {
            if (connection != null && connection.isOpen()) connection.close();
        } catch (Exception e) {
            System.err.println("Erro ao fechar connection RabbitMQ: " + e.getMessage());
        } finally {
            connection = null;
        }
    }
}

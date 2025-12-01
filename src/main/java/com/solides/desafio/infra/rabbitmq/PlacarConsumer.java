package com.solides.desafio.infra.rabbitmq;

import com.rabbitmq.client.*;
import com.solides.desafio.infra.redis.RedisClientProvider;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

@Component
@ConditionalOnProperty(name = "rabbit.enabled", havingValue = "true")
public class PlacarConsumer {

    private final RedisClientProvider redisProvider;

    // RabbitMQ resources
    private Connection connection;
    private Channel channel;

    @Value("${rabbit.host:rabbitmq}")
    private String rabbitHost;

    @Value("${rabbit.queue:placar_eventos}")
    private String queueName;

    public PlacarConsumer(RedisClientProvider redisProvider) {
        this.redisProvider = redisProvider;
    }

    @PostConstruct
    public void init() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(rabbitHost);

            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.queueDeclare(queueName, true, false, false, null);

            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                           AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String msg = new String(body, StandardCharsets.UTF_8);
                    try (Jedis jedis = redisProvider.getJedis()) {
                        jedis.set("placar_eventos_last", msg);
                    } catch (Exception e) {
                        System.err.println("Erro ao processar mensagem do RabbitMQ: " + e.getMessage());
                    }
                }
            };

            channel.basicConsume(queueName, true, consumer);
            System.out.println("PlacarConsumer conectado ao RabbitMQ em " + rabbitHost);
        } catch (IOException | TimeoutException ex) {
            // Não propagar - log e deixar a aplicação subir
            System.err.println("Aviso: não foi possível inicializar PlacarConsumer. RabbitMq host=" + rabbitHost
                    + ". Mensagem: " + ex.getMessage());
            safeCloseChannel();
            safeCloseConnection();
            channel = null;
            connection = null;
        } catch (Exception ex) {
            System.err.println("Erro inesperado em PlacarConsumer.init: " + ex.getMessage());
            safeCloseChannel();
            safeCloseConnection();
            channel = null;
            connection = null;
        }
    }

    @PreDestroy
    public void shutdown() {
        safeCloseChannel();
        safeCloseConnection();
    }

    private void safeCloseChannel() {
        try {
            if (channel != null && channel.isOpen()) channel.close();
        } catch (Exception e) {
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

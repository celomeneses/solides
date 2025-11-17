package com.solides.desafio.infra.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class PlacarProducer {
    private Connection connection;
    private Channel channel;

    @PostConstruct
    public void init() throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(System.getenv().getOrDefault("RABBIT_HOST","rabbitmq"));
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.queueDeclare("placar_eventos", true, false, false, null);
    }

    public void enviarEvento(String json) throws IOException {
        channel.basicPublish("", "placar_eventos", null, json.getBytes(StandardCharsets.UTF_8));
    }

    @PreDestroy
    public void close() throws Exception{
        channel.close(); connection.close();
    }
}

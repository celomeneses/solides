package com.solides.desafio.infra.rabbitmq;

import com.rabbitmq.client.*;
import com.solides.desafio.infra.redis.RedisClientProvider;
import jakarta.annotation.PostConstruct;
import redis.clients.jedis.Jedis;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Startup
@Singleton
public class PlacarConsumer {

    @Inject
    RedisClientProvider redisProvider;

    @PostConstruct
    public void init() throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(System.getenv().getOrDefault("RABBIT_HOST","rabbitmq"));
        Connection conn = factory.newConnection();
        Channel ch = conn.createChannel();
        ch.queueDeclare("placar_eventos", true, false, false, null);
        Consumer consumer = new DefaultConsumer(ch) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String msg = new String(body, StandardCharsets.UTF_8);
                try(Jedis jedis = redisProvider.getJedis()){
                }
            }
        };
        ch.basicConsume("placar_eventos", true, consumer);
    }
}

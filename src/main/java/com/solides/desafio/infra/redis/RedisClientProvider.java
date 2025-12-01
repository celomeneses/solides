package com.solides.desafio.infra.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

@Component
public class RedisClientProvider {
    private static final Logger log = LoggerFactory.getLogger(RedisClientProvider.class);

    public Jedis getJedis(){
        String host = System.getenv().getOrDefault("REDIS_HOST","127.0.0.1");
        int port = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT","6379"));
        int timeout = Integer.parseInt(System.getenv().getOrDefault("REDIS_TIMEOUT_MS","2000"));

        log.info("Tentando conectar Redis em {}:{} (timeout {}ms)", host, port, timeout);
        try {
            Jedis jedis = new Jedis(host, port, timeout);
            String pong = jedis.ping(); // valida conexão imediatamente
            log.info("Redis respondeu: {}", pong);
            return jedis;
        } catch (Exception e) {
            log.error("Erro conectando ao Redis em {}:{} - {}", host, port, e.getMessage(), e);
            throw e;
        }
    }
}

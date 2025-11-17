package com.solides.desafio.infra.redis;

import redis.clients.jedis.Jedis;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RedisClientProvider {
    public Jedis getJedis(){
        return new Jedis(System.getenv().getOrDefault("REDIS_HOST","redis"));
    }
}

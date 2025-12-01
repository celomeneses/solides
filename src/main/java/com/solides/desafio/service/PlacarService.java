package com.solides.desafio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.solides.desafio.infra.rabbitmq.PlacarProducer;
import com.solides.desafio.infra.redis.RedisClientProvider;
import com.solides.desafio.repository.PlacarRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class PlacarService {

    private final PlacarRepository placarRepository;
    private final PlacarProducer producer;
    private final RedisClientProvider redisProvider;
    private final ObjectMapper mapper = new ObjectMapper();

    public PlacarService(PlacarRepository placarRepository,
                         PlacarProducer producer,
                         RedisClientProvider redisProvider) {
        this.placarRepository = placarRepository;
        this.producer = producer;
        this.redisProvider = redisProvider;
    }

    public String iniciar(String jsonDados) {
        String res = placarRepository.iniciar(jsonDados);
        try {
            if (res != null) {
                JsonNode node = mapper.readTree(res);
                if (node.has("hash_id")) {
                    String hash = node.get("hash_id").asText();
                    try (var jedis = redisProvider.getJedis()) {
                        jedis.set(("placar:" + hash).getBytes(StandardCharsets.UTF_8),
                                jsonDados.getBytes(StandardCharsets.UTF_8));
                    } catch (Exception e) {
                        System.err.println("Aviso: não foi possível salvar no Redis: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {

        }
        return res;
    }

    public String pontuar(String hashId, String lado) {
        try {
            String chaveRedis = "placar:" + hashId;
            String dadosAtuais = null;

            // 1) Tenta buscar no Redis
            try (var jedis = redisProvider.getJedis()) {
                byte[] raw = jedis.get(chaveRedis.getBytes(StandardCharsets.UTF_8));
                if (raw != null) {
                    dadosAtuais = new String(raw, StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                System.err.println("Redis indisponível ao ler. CAUSA: " + e.getMessage());
                // seguimos adiante — fallback vai para DB
            }

            // 2) Fallback: buscar no DB se Redis falhou ou não tinha chave
            if (dadosAtuais == null) {
                Optional<String> fromDb = placarRepository.buscarDadosPorHash(hashId);
                if (fromDb.isEmpty())
                    throw new IllegalArgumentException("Placar não encontrado: " + hashId);

                dadosAtuais = fromDb.get();
            }

            // 3) Manipula JSON
            JsonNode root = mapper.readTree(dadosAtuais);

            int pontosCasa = root.path("time_da_casa").path("pontos").asInt(0);
            int pontosVisit = root.path("time_visitante").path("pontos").asInt(0);

            if ("casa".equalsIgnoreCase(lado))      pontosCasa++;
            else if ("visitante".equalsIgnoreCase(lado)) pontosVisit++;
            else throw new IllegalArgumentException("lado inválido. Use 'casa' ou 'visitante'.");

            ObjectNode novo = mapper.createObjectNode();

            ObjectNode casaNode = novo.putObject("time_da_casa");
            casaNode.put("pontos", pontosCasa);

            ObjectNode visitanteNode = novo.putObject("time_visitante");
            visitanteNode.put("pontos", pontosVisit);

            String fullPatch = novo.toString();

            // 4) Persiste no DB — sempre!
            String atualizado = placarRepository.atualizar(hashId, fullPatch);

            // 5) Publica evento async se possível
            try {
                var evento = mapper.createObjectNode()
                        .put("hashId", hashId)
                        .put("lado", lado)
                        .put("pontosCasa", pontosCasa)
                        .put("pontosVisitante", pontosVisit);

                producer.enviarEvento(mapper.writeValueAsString(evento));
            } catch (Exception e) {
                System.err.println("RabbitMQ indisponível ao publicar. CAUSA: " + e.getMessage());
                // ignora: não impede fluxo do sistema
            }

            // 6) Atualiza Redis como cache — sem quebrar se falhar!
            try (var jedis = redisProvider.getJedis()) {
                jedis.set(chaveRedis.getBytes(StandardCharsets.UTF_8),
                        atualizado.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                System.err.println("Redis indisponível ao escrever. CAUSA: " + e.getMessage());
            }

            return atualizado;

        } catch (IOException e) {
            throw new RuntimeException("Erro ao manipular JSON do placar", e);
        }
    }


    public Optional<String> buscar(String hashId) {
        String chave = "placar:" + hashId;
        try {
            try (var jedis = redisProvider.getJedis()) {
                byte[] raw = jedis.get(chave.getBytes(StandardCharsets.UTF_8));
                if (raw != null) {
                    return Optional.of(new String(raw, StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) {
            // ignora e busca no DB
        }
        return placarRepository.buscarDadosPorHash(hashId);
    }

    public void finalizar(String hashId) {
        placarRepository.finalizar(hashId);
        try (var jedis = redisProvider.getJedis()) {
            jedis.del(("placar:" + hashId).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            // log warning: não falha se Redis falhar
        }
    }
}
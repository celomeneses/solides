package com.solides.desafio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solides.desafio.infra.rabbitmq.PlacarProducer;
import com.solides.desafio.infra.redis.RedisClientProvider;
import com.solides.desafio.repository.PlacarRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@ApplicationScoped
public class PlacarService {

    @Inject
    PlacarRepository placarRepository;

    @Inject
    PlacarProducer producer; // envia mensagens para RabbitMQ

    @Inject
    RedisClientProvider redisProvider; // fornece Jedis

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Inicia um placar: chama a procedure, publica o estado inicial no Redis (opcional)
     * e retorna o JSON retornado pela procedure (string).
     */
    public String iniciar(String jsonDados) {
        String res = placarRepository.iniciar(jsonDados); // ex: {"hash_id":"abc123"}
        // tenta extrair o hash e armazenar o estado inicial no Redis (se procedure retornou dados)
        try {
            if (res != null) {
                JsonNode node = mapper.readTree(res);
                if (node.has("hash_id")) {
                    String hash = node.get("hash_id").asText();
                    // Salvar o payload inicial no redis para leitura rápida (chave: placar:{hash})
                    try (var jedis = redisProvider.getJedis()) {
                        jedis.set(("placar:" + hash).getBytes(StandardCharsets.UTF_8),
                                jsonDados.getBytes(StandardCharsets.UTF_8));
                    } catch (Exception e) {
                        // log warning - não impedimos criação se Redis falhar
                        System.err.println("Aviso: não foi possível salvar no Redis: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            // se parsing falhar, apenas retornar a string
        }
        return res;
    }

    /**
     * Pontua (incrementa) um time no placar identificado por hashId.
     * Aqui usamos uma estratégia simples: montar um patch JSON e chamar a procedure sp_atualiza_placar.
     * Também publica um evento em RabbitMQ para consumidores e atualiza o Redis localmente.
     */
    public String pontuar(String hashId, String lado /* "casa" ou "visitante" */) {
        try {
            // Buscar estado atual preferencialmente no Redis
            String chave = "placar:" + hashId;
            String dadosAtuais = null;
            try (var jedis = redisProvider.getJedis()) {
                byte[] raw = jedis.get(chave.getBytes(StandardCharsets.UTF_8));
                if (raw != null) dadosAtuais = new String(raw, StandardCharsets.UTF_8);
            } catch (Exception e) {
                // fallback para DB se Redis falhar
            }

            if (dadosAtuais == null) {
                Optional<String> fromDb = placarRepository.buscarDadosPorHash(hashId);
                if (fromDb.isEmpty()) throw new IllegalArgumentException("Placar não encontrado: " + hashId);
                dadosAtuais = fromDb.get();
            }

            // Parse dos dados e incremento
            JsonNode root = mapper.readTree(dadosAtuais);
            // Assumimos estrutura: { "time_da_casa": {"nome":"X","pontos":N}, "time_visitante": {...} }
            JsonNode casa = root.path("time_da_casa");
            JsonNode visitante = root.path("time_visitante");

            int pontosCasa = casa.has("pontos") ? casa.get("pontos").asInt() : 0;
            int pontosVisit = visitante.has("pontos") ? visitante.get("pontos").asInt() : 0;

            if ("casa".equalsIgnoreCase(lado)) pontosCasa += 1;
            else if ("visitante".equalsIgnoreCase(lado)) pontosVisit += 1;
            else throw new IllegalArgumentException("lado inválido. Use 'casa' ou 'visitante'.");

            // Monta patch JSON simples (depende da sua procedure aceitar merge)
            String patchJson = mapper.createObjectNode()
                    .putObject("time_da_casa").put("pontos", pontosCasa).toString();

            // Se preferir, montar patch completo:
            // {"time_da_casa":{"pontos":X},"time_visitante":{"pontos":Y}}
            String fullPatch = mapper.createObjectNode()
                    .putObject("time_da_casa").put("pontos", pontosCasa)
                    .putObject("time_visitante").put("pontos", pontosVisit)
                    .toString();

            // Atualiza via procedure (espera que sp_atualiza_placar retorne dados atualizados)
            String atualizado = placarRepository.atualizar(hashId, fullPatch);

            // Publica evento no RabbitMQ com os novos pontos
            var eventoNode = mapper.createObjectNode();
            eventoNode.put("hashId", hashId);
            eventoNode.put("lado", lado);
            eventoNode.put("pontosCasa", pontosCasa);
            eventoNode.put("pontosVisitante", pontosVisit);
            String eventoJson = mapper.writeValueAsString(eventoNode);
            try {
                producer.enviarEvento(eventoJson);
            } catch (Exception e) {
                // log e seguir: não é fatal para a atualização
                System.err.println("Erro ao publicar evento RabbitMQ: " + e.getMessage());
            }

            // Atualiza cache Redis com o estado atualizado
            try (var jedis = redisProvider.getJedis()) {
                jedis.set(("placar:" + hashId).getBytes(StandardCharsets.UTF_8),
                        atualizado.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                // log warning
            }

            return atualizado;
        } catch (IOException e) {
            throw new RuntimeException("Erro ao manipular JSON do placar", e);
        }
    }

    /**
     * Busca o placar: primeiro tenta o Redis; se não achar, busca no banco.
     */
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

    /**
     * Finaliza placar: chama a procedure e remove do Redis.
     */
    public void finalizar(String hashId) {
        placarRepository.finalizar(hashId);
        try (var jedis = redisProvider.getJedis()) {
            jedis.del(("placar:" + hashId).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            // log warning: não falha se Redis falhar
        }
    }
}


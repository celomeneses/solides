package com.solides.desafio.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solides.desafio.service.PlacarService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/placar")
public class PlacarController {

    private final PlacarService placarService;
    private final ObjectMapper mapper = new ObjectMapper();

    public PlacarController(PlacarService placarService) {
        this.placarService = placarService;
    }

    @PostMapping(value = "/iniciar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> iniciarPlacar(@RequestBody JsonNode payload) {
        if (payload == null || payload.isNull() || payload.isEmpty()) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body(mapper.createObjectNode().put("error","Payload inválido"));
        }
        try {
            String payloadStr = mapper.writeValueAsString(payload);
            String result = placarService.iniciar(payloadStr);
            if (result == null || result.isBlank()) {
                return ResponseEntity.status(500).contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.createObjectNode().put("error","Resposta inválida da service"));
            }
            JsonNode node = mapper.readTree(result);
            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(node);
        } catch (Exception ex) {
            return ResponseEntity.status(500).contentType(MediaType.APPLICATION_JSON)
                    .body(mapper.createObjectNode().put("error","Erro ao iniciar placar: " + ex.getMessage()));
        }
    }

    @PostMapping(value = "/pontuar/{hash_id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> pontuar(
            @PathVariable("hash_id") String hashId,
            @RequestParam(value = "lado", required = false) String ladoQuery,
            @RequestBody(required = false) JsonNode body
    ) {
        try {
            String lado = null;
            if (ladoQuery != null && !ladoQuery.isBlank()) {
                lado = ladoQuery;
            } else if (body != null && !body.isEmpty()) {
                if (body.has("lado")) lado = body.get("lado").asText();
                else if (body.has("side")) lado = body.get("side").asText();
            }

            if (lado == null || lado.isBlank()) {
                return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.createObjectNode().put("error","Parâmetro 'lado' é obrigatório (casa ou visitante)"));
            }

            String atualizado = placarService.pontuar(hashId, lado);
            if (atualizado == null) {
                return ResponseEntity.status(500).contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.createObjectNode().put("error","Resposta inválida da service"));
            }
            JsonNode node = mapper.readTree(atualizado);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(node);

        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON)
                    .body(mapper.createObjectNode().put("error", iae.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).contentType(MediaType.APPLICATION_JSON)
                    .body(mapper.createObjectNode().put("error","Erro ao pontuar: " + ex.getMessage()));
        }
    }

    @GetMapping(value = "/{hash_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> buscar(@PathVariable("hash_id") String hashId) {
        try {
            Optional<String> opt = placarService.buscar(hashId);
            if (opt.isPresent()) {
                JsonNode node = mapper.readTree(opt.get());
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(node);
            } else {
                return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.createObjectNode().put("error","Placar não encontrado"));
            }
        } catch (Exception ex) {
            return ResponseEntity.status(500).contentType(MediaType.APPLICATION_JSON)
                    .body(mapper.createObjectNode().put("error","Erro ao buscar placar: " + ex.getMessage()));
        }
    }

    @DeleteMapping("/{hash_id}")
    public ResponseEntity<?> finalizar(@PathVariable("hash_id") String hashId) {
        try {
            placarService.finalizar(hashId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON)
                    .body(mapper.createObjectNode().put("error", iae.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).contentType(MediaType.APPLICATION_JSON)
                    .body(mapper.createObjectNode().put("error","Erro ao finalizar placar: " + ex.getMessage()));
        }
    }
}

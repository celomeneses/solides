package com.solides.desafio.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solides.desafio.service.PlacarService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Optional;

@Path("/placar")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PlacarController {

    @Inject
    PlacarService placarService;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * POST /placar/iniciar
     * Body: JSON com dados iniciais (time_da_casa, time_visitante, etc)
     * Retorna: 201 com o JSON retornado pela procedure (ex: {"hash_id":"..."})
     */
    @POST
    @Path("/iniciar")
    public Response iniciarPlacar(String payload) {
        if (payload == null || payload.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Payload inválido\"}")
                    .build();
        }

        try {
            String result = placarService.iniciar(payload);
            return Response.status(Response.Status.CREATED).entity(result).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Erro ao iniciar placar: " + ex.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * POST /placar/pontuar/{hash_id}
     * Query param (preferencial): ?lado=casa  (ou visitante)
     * Body (alternativa): { "lado": "casa" }
     * Retorna: 200 com o placar atualizado (JSON) ou 400/404 conforme o caso
     */
    @POST
    @Path("/pontuar/{hash_id}")
    public Response pontuar(
            @PathParam("hash_id") String hashId,
            @QueryParam("lado") String ladoQuery,
            String body
    ) {
        try {
            String lado = null;

            // 1) Prioriza query param
            if (ladoQuery != null && !ladoQuery.isBlank()) {
                lado = ladoQuery;
            } else if (body != null && !body.isBlank()) {
                // 2) Tenta extrair do body JSON
                try {
                    JsonNode node = mapper.readTree(body);
                    if (node.has("lado")) lado = node.get("lado").asText();
                    else if (node.has("side")) lado = node.get("side").asText(); // fallback
                } catch (IOException e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("{\"error\":\"Body JSON inválido\"}")
                            .build();
                }
            }

            if (lado == null || lado.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"Parâmetro 'lado' é obrigatório (casa ou visitante)\"}")
                        .build();
            }

            String atualizado = placarService.pontuar(hashId, lado);
            return Response.ok(atualizado).build();

        } catch (IllegalArgumentException iae) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"" + iae.getMessage() + "\"}")
                    .build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Erro ao pontuar: " + ex.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * GET /placar/{hash_id}
     * Retorna o placar atual (primeiro do Redis; se não achar, do DB)
     */
    @GET
    @Path("/{hash_id}")
    public Response buscar(@PathParam("hash_id") String hashId) {
        try {
            Optional<String> opt = placarService.buscar(hashId);
            if (opt.isPresent()) {
                // já é uma string JSON; devolve como application/json
                return Response.ok(opt.get(), MediaType.APPLICATION_JSON).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Placar não encontrado\"}")
                        .build();
            }
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Erro ao buscar placar: " + ex.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * DELETE /placar/{hash_id}
     * Finaliza o placar (procedure) e remove do cache Redis (se existir)
     */
    @DELETE
    @Path("/{hash_id}")
    public Response finalizar(@PathParam("hash_id") String hashId) {
        try {
            placarService.finalizar(hashId);
            return Response.noContent().build();
        } catch (IllegalArgumentException iae) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"" + iae.getMessage() + "\"}")
                    .build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Erro ao finalizar placar: " + ex.getMessage() + "\"}")
                    .build();
        }
    }
}


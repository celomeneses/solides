package com.solides.desafio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.solides.desafio.service.PlacarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PlacarControllerTest {

    @Mock
    PlacarService placarService;

    MockMvc mvc;
    ObjectMapper mapper;

    @BeforeEach
    void setup() {
        mapper = new ObjectMapper();
        PlacarController controller = new PlacarController(placarService);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
    }

    @Test
    void iniciar_shouldReturn201_whenPayloadValid() throws Exception {
        ObjectNode payload = mapper.createObjectNode();
        payload.set("time_da_casa", mapper.createObjectNode().put("nome", "A").put("pontos", 0));
        payload.set("time_visitante", mapper.createObjectNode().put("nome", "B").put("pontos", 0));
        String response = "{\"hash_id\":\"abc123\"}";

        when(placarService.iniciar(anyString())).thenReturn(response);

        mvc.perform(post("/api/placar/iniciar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(content().json(response));
    }

    @Test
    void iniciar_shouldReturn400_whenPayloadEmpty() throws Exception {
        mvc.perform(post("/api/placar/iniciar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pontuar_shouldReturn200_whenQueryParamProvided() throws Exception {
        String updated = "{\"time_da_casa\":{\"pontos\":1},\"time_visitante\":{\"pontos\":0}}";
        when(placarService.pontuar(eq("abc123"), eq("casa"))).thenReturn(updated);

        mvc.perform(post("/api/placar/pontuar/abc123")
                        .param("lado", "casa"))
                .andExpect(status().isOk())
                .andExpect(content().json(updated));
    }

    @Test
    void pontuar_shouldReturn200_whenBodyContainsLado() throws Exception {
        String body = "{\"lado\":\"visitante\"}";
        String updated = "{\"time_da_casa\":{\"pontos\":1},\"time_visitante\":{\"pontos\":1}}";
        when(placarService.pontuar(eq("abc123"), eq("visitante"))).thenReturn(updated);

        mvc.perform(post("/api/placar/pontuar/abc123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().json(updated));
    }

    @Test
    void pontuar_shouldReturn400_whenLadoMissing() throws Exception {
        mvc.perform(post("/api/placar/pontuar/abc123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pontuar_shouldReturn404_whenServiceThrowsNotFound() throws Exception {
        when(placarService.pontuar(eq("nope"), eq("casa")))
                .thenThrow(new IllegalArgumentException("Placar não encontrado: nope"));

        mvc.perform(post("/api/placar/pontuar/nope")
                        .param("lado", "casa"))
                .andExpect(status().isNotFound());
    }

    @Test
    void buscar_shouldReturn200_whenFound() throws Exception {
        String json = "{\"time_da_casa\":{\"pontos\":1},\"time_visitante\":{\"pontos\":0}}";
        when(placarService.buscar("abc123")).thenReturn(Optional.of(json));

        mvc.perform(get("/api/placar/abc123"))
                .andExpect(status().isOk())
                .andExpect(content().json(json));
    }

    @Test
    void buscar_shouldReturn404_whenNotFound() throws Exception {
        when(placarService.buscar("notfound")).thenReturn(Optional.empty());

        mvc.perform(get("/api/placar/notfound"))
                .andExpect(status().isNotFound());
    }

    @Test
    void finalizar_shouldReturn204_whenOk() throws Exception {
        doNothing().when(placarService).finalizar("abc123");

        mvc.perform(delete("/api/placar/abc123"))
                .andExpect(status().isNoContent());
    }

    @Test
    void finalizar_shouldReturn404_whenNotFound() throws Exception {
        doThrow(new IllegalArgumentException("not found")).when(placarService).finalizar("nope");

        mvc.perform(delete("/api/placar/nope"))
                .andExpect(status().isNotFound());
    }
}
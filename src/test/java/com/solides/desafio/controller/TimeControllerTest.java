package com.solides.desafio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solides.desafio.domain.Time;
import com.solides.desafio.service.TimeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TimeController.class)
class TimeControllerTest {

    @Autowired MockMvc mvc;
    @MockBean TimeService service;
    @Autowired ObjectMapper mapper;

    @Test
    void criar_ok() throws Exception {
        Time t = new Time(); t.setNome("A"); t.setLogo("L");
        when(service.criar(any())).thenReturn(t);

        mvc.perform(post("/api/time")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(t)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("A"));
    }

    @Test
    void listar_ok() throws Exception {
        Time t1 = new Time(); t1.setNome("A");
        when(service.listar()).thenReturn(List.of(t1));

        mvc.perform(get("/api/time"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("A"));
    }
}
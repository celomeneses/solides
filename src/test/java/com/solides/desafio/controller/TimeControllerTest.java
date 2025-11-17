package com.solides.desafio.controller;

import com.solides.desafio.domain.Time;
import com.solides.desafio.service.TimeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TimeController covering all branches.
 */
@ExtendWith(MockitoExtension.class)
class TimeControllerTest {

    @Mock
    private TimeService service;

    @InjectMocks
    private TimeController controller;

    @Test
    void criar_deveRetornar201_eChamarService() {
        Time input = new Time();
        input.setNome("Time A");
        input.setLogo("logo");

        Time returned = new Time();
        returned.setId(1L);
        returned.setNome("Time A");
        returned.setLogo("logo");

        when(service.criar(input)).thenReturn(returned);

        Response resp = controller.criar(input);

        assertEquals(201, resp.getStatus());
        assertEquals(returned, resp.getEntity());
        verify(service, times(1)).criar(input);
    }

    @Test
    void listar_deveRetornarListaDoService() {
        Time t1 = new Time(); t1.setNome("A");
        Time t2 = new Time(); t2.setNome("B");

        when(service.listar()).thenReturn(List.of(t1, t2));

        List<Time> res = controller.listar();

        assertNotNull(res);
        assertEquals(2, res.size());
        assertSame(t1, res.get(0));
        verify(service, times(1)).listar();
    }

    @Test
    void buscar_deveRetornar200_quandoExistir() {
        Long id = 10L;
        Time found = new Time(); found.setId(id); found.setNome("X");

        when(service.buscar(id)).thenReturn(Optional.of(found));

        Response resp = controller.buscar(id);

        assertEquals(200, resp.getStatus());
        assertEquals(found, resp.getEntity());
        verify(service, times(1)).buscar(id);
    }

    @Test
    void buscar_deveRetornar404_quandoNaoExistir() {
        Long id = 999L;
        when(service.buscar(id)).thenReturn(Optional.empty());

        Response resp = controller.buscar(id);

        assertEquals(404, resp.getStatus());
        verify(service, times(1)).buscar(id);
    }

    @Test
    void atualizar_deveRetornar200_eChamarService() {
        Long id = 5L;
        Time input = new Time(); input.setNome("Novo");
        Time updated = new Time(); updated.setId(id); updated.setNome("Novo");

        when(service.atualizar(id, input)).thenReturn(updated);

        Response resp = controller.atualizar(id, input);

        assertEquals(200, resp.getStatus());
        assertEquals(updated, resp.getEntity());
        verify(service, times(1)).atualizar(id, input);
    }

    @Test
    void deletar_deveRetornar204_eChamarService() {
        Long id = 7L;

        // void method: default doNothing
        Response resp = controller.deletar(id);

        assertEquals(204, resp.getStatus());
        verify(service, times(1)).deletar(id);
    }
}


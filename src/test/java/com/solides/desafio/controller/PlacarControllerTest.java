package com.solides.desafio.controller;

import com.solides.desafio.service.PlacarService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para PlacarController cobrindo todos os caminhos de execução.
 */
@ExtendWith(MockitoExtension.class)
class PlacarControllerTest {

    @Mock
    private PlacarService placarService;

    @InjectMocks
    private PlacarController controller;

    // ---------- iniciarPlacar ----------

    @Test
    void iniciarPlacar_deveRetornar201_quandoPayloadValido() {
        String payload = "{\"time_da_casa\": {\"nome\":\"A\",\"pontos\":0}, \"time_visitante\": {\"nome\":\"B\",\"pontos\":0}}";
        String retornoProcedure = "{\"hash_id\":\"abc123\"}";

        when(placarService.iniciar(payload)).thenReturn(retornoProcedure);

        Response resp = controller.iniciarPlacar(payload);

        assertEquals(201, resp.getStatus());
        assertNotNull(resp.getEntity());
        assertTrue(resp.getEntity().toString().contains("abc123"));
        verify(placarService, times(1)).iniciar(payload);
    }

    @Test
    void iniciarPlacar_deveRetornar400_quandoPayloadVazio() {
        Response resp = controller.iniciarPlacar("");
        assertEquals(400, resp.getStatus());
        assertNotNull(resp.getEntity());
        verifyNoInteractions(placarService);
    }

    @Test
    void iniciarPlacar_deveRetornar500_quandoServiceLancarExcecao() {
        String payload = "{\"x\":1}";
        when(placarService.iniciar(payload)).thenThrow(new RuntimeException("erro interno"));

        Response resp = controller.iniciarPlacar(payload);

        assertEquals(500, resp.getStatus());
        String entity = resp.getEntity().toString();
        assertTrue(entity.contains("Erro ao iniciar placar"));
        assertTrue(entity.contains("erro interno"));
        verify(placarService, times(1)).iniciar(payload);
    }

    // ---------- pontuar ----------

    @Test
    void pontuar_deveRetornar200_quandoQueryParamLadoInformado() {
        String hash = "abc123";
        String ladoQuery = "casa";
        String resultadoAtualizado = "{\"time_da_casa\":{\"pontos\":1},\"time_visitante\":{\"pontos\":0}}";

        when(placarService.pontuar(hash, ladoQuery)).thenReturn(resultadoAtualizado);

        Response resp = controller.pontuar(hash, ladoQuery, null);

        assertEquals(200, resp.getStatus());
        assertEquals(resultadoAtualizado, resp.getEntity());
        verify(placarService, times(1)).pontuar(hash, ladoQuery);
    }

    @Test
    void pontuar_deveRetornar200_quandoBodyContemLado() {
        String hash = "abc123";
        String body = "{\"lado\":\"visitante\"}";
        String resultadoAtualizado = "{\"time_da_casa\":{\"pontos\":0},\"time_visitante\":{\"pontos\":1}}";

        when(placarService.pontuar(hash, "visitante")).thenReturn(resultadoAtualizado);

        Response resp = controller.pontuar(hash, null, body);

        assertEquals(200, resp.getStatus());
        assertEquals(resultadoAtualizado, resp.getEntity());
        verify(placarService, times(1)).pontuar(hash, "visitante");
    }

    @Test
    void pontuar_deveUsarFallbackSide_quandoBodyContemSide() {
        String hash = "abc123";
        String body = "{\"side\":\"casa\"}";
        String resultado = "{\"time_da_casa\":{\"pontos\":1}}";

        when(placarService.pontuar(hash, "casa")).thenReturn(resultado);

        Response resp = controller.pontuar(hash, null, body);

        assertEquals(200, resp.getStatus());
        assertEquals(resultado, resp.getEntity());
        verify(placarService, times(1)).pontuar(hash, "casa");
    }

    @Test
    void pontuar_deveRetornar400_quandoBodyJsonInvalido() {
        String hash = "abc123";
        String body = "{ invalid json ";

        Response resp = controller.pontuar(hash, null, body);

        assertEquals(400, resp.getStatus());
        String entity = resp.getEntity().toString();
        assertTrue(entity.contains("Body JSON inválido"));
        verifyNoInteractions(placarService);
    }

    @Test
    void pontuar_deveRetornar400_quandoLadoNaoInformado() {
        String hash = "abc123";
        Response resp = controller.pontuar(hash, null, null);
        assertEquals(400, resp.getStatus());
        assertNotNull(resp.getEntity());
        verifyNoInteractions(placarService);
    }

    @Test
    void pontuar_deveRetornar404_quandoServiceLancarIllegalArgumentException() {
        String hash = "nao-existe";
        when(placarService.pontuar(hash, "casa")).thenThrow(new IllegalArgumentException("Placar não encontrado"));

        Response resp = controller.pontuar(hash, "casa", null);

        assertEquals(404, resp.getStatus());
        String entity = resp.getEntity().toString();
        assertTrue(entity.contains("Placar não encontrado"));
        verify(placarService, times(1)).pontuar(hash, "casa");
    }

    @Test
    void pontuar_deveRetornar500_quandoServiceLancarExcecaoGenerica() {
        String hash = "abc123";
        when(placarService.pontuar(hash, "casa")).thenThrow(new RuntimeException("boom"));

        Response resp = controller.pontuar(hash, "casa", null);

        assertEquals(500, resp.getStatus());
        String entity = resp.getEntity().toString();
        assertTrue(entity.contains("Erro ao pontuar"));
        assertTrue(entity.contains("boom"));
        verify(placarService, times(1)).pontuar(hash, "casa");
    }

    // ---------- buscar ----------

    @Test
    void buscar_deveRetornar200_quandoPlacarExistir() {
        String hash = "abc123";
        String dados = "{\"time_da_casa\":{\"pontos\":1}}";

        when(placarService.buscar(hash)).thenReturn(Optional.of(dados));

        Response resp = controller.buscar(hash);

        assertEquals(200, resp.getStatus());
        assertEquals(dados, resp.getEntity());
        verify(placarService, times(1)).buscar(hash);
    }

    @Test
    void buscar_deveRetornar404_quandoPlacarNaoExistir() {
        String hash = "nao-existe";
        when(placarService.buscar(hash)).thenReturn(Optional.empty());

        Response resp = controller.buscar(hash);

        assertEquals(404, resp.getStatus());
        assertNotNull(resp.getEntity());
        verify(placarService, times(1)).buscar(hash);
    }

    @Test
    void buscar_deveRetornar500_quandoServiceLancarExcecao() {
        String hash = "abc123";
        when(placarService.buscar(hash)).thenThrow(new RuntimeException("falha DB"));

        Response resp = controller.buscar(hash);

        assertEquals(500, resp.getStatus());
        String entity = resp.getEntity().toString();
        assertTrue(entity.contains("Erro ao buscar placar"));
        assertTrue(entity.contains("falha DB"));
        verify(placarService, times(1)).buscar(hash);
    }

    // ---------- finalizar ----------

    @Test
    void finalizar_deveRetornar204_quandoSucesso() {
        String hash = "abc123";

        // comportamento padrão: método void não faz nada
        Response resp = controller.finalizar(hash);

        assertEquals(204, resp.getStatus());
        verify(placarService, times(1)).finalizar(hash);
    }

    @Test
    void finalizar_deveRetornar404_quandoServiceLancarIllegalArgumentException() {
        String hash = "nao-existe";
        doThrow(new IllegalArgumentException("Placar não encontrado")).when(placarService).finalizar(hash);

        Response resp = controller.finalizar(hash);

        assertEquals(404, resp.getStatus());
        String entity = resp.getEntity().toString();
        assertTrue(entity.contains("Placar não encontrado"));
        verify(placarService, times(1)).finalizar(hash);
    }

    @Test
    void finalizar_deveRetornar500_quandoServiceLancarExcecaoGenerica() {
        String hash = "abc123";
        doThrow(new RuntimeException("erro grave")).when(placarService).finalizar(hash);

        Response resp = controller.finalizar(hash);

        assertEquals(500, resp.getStatus());
        String entity = resp.getEntity().toString();
        assertTrue(entity.contains("Erro ao finalizar placar"));
        assertTrue(entity.contains("erro grave"));
        verify(placarService, times(1)).finalizar(hash);
    }
}




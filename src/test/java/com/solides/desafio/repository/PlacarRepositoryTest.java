package com.solides.desafio.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlacarRepositoryTest {

    @Mock
    EntityManager em;

    @Mock
    StoredProcedureQuery spQuery;

    @Mock
    Query nativeQuery;

    @InjectMocks
    PlacarRepository repo;

    @Test
    void iniciar_ok() {
        when(em.createStoredProcedureQuery("sp_inicia_placar")).thenReturn(spQuery);
        when(spQuery.registerStoredProcedureParameter(anyInt(), eq(String.class), eq(jakarta.persistence.ParameterMode.IN)))
                .thenReturn(spQuery);
        when(spQuery.setParameter(1, "json")).thenReturn(spQuery);
        when(spQuery.getSingleResult()).thenReturn("RESULT");

        String r = repo.iniciar("json");

        assertEquals("RESULT", r);
        verify(spQuery).execute();
    }

    @Test
    void atualizar_ok() {
        when(em.createStoredProcedureQuery("sp_atualiza_placar")).thenReturn(spQuery);
        when(spQuery.registerStoredProcedureParameter(anyInt(), eq(String.class), eq(jakarta.persistence.ParameterMode.IN)))
                .thenReturn(spQuery);
        when(spQuery.setParameter(1, "abc")).thenReturn(spQuery);
        when(spQuery.setParameter(2, "{x}")).thenReturn(spQuery);
        when(spQuery.getSingleResult()).thenReturn("OK");

        String r = repo.atualizar("abc", "{x}");

        assertEquals("OK", r);
        verify(spQuery).execute();
    }

    @Test
    void atualizar_nullResult() {
        when(em.createStoredProcedureQuery("sp_atualiza_placar")).thenReturn(spQuery);
        when(spQuery.registerStoredProcedureParameter(anyInt(), eq(String.class), eq(jakarta.persistence.ParameterMode.IN)))
                .thenReturn(spQuery);
        when(spQuery.setParameter(anyInt(), anyString())).thenReturn(spQuery);
        when(spQuery.getSingleResult()).thenReturn(null);

        String r = repo.atualizar("abc", "{x}");

        assertNull(r);
    }

    @Test
    void finalizar_ok() {
        when(em.createStoredProcedureQuery("sp_finaliza_placar")).thenReturn(spQuery);
        when(spQuery.registerStoredProcedureParameter(anyInt(), eq(String.class), eq(jakarta.persistence.ParameterMode.IN)))
                .thenReturn(spQuery);
        when(spQuery.setParameter(1, "abc")).thenReturn(spQuery);

        repo.finalizar("abc");

        verify(spQuery).execute();
    }

    @Test
    void buscarDadosPorHash_ok() {
        when(em.createNativeQuery("select dados from placar where hash_id = :h")).thenReturn(nativeQuery);
        when(nativeQuery.setParameter("h", "abc")).thenReturn(nativeQuery);
        when(nativeQuery.getSingleResult()).thenReturn("DATA");

        Optional<String> r = repo.buscarDadosPorHash("abc");

        assertTrue(r.isPresent());
        assertEquals("DATA", r.get());
    }

    @Test
    void buscarDadosPorHash_nullResult() {
        when(em.createNativeQuery("select dados from placar where hash_id = :h")).thenReturn(nativeQuery);
        when(nativeQuery.setParameter("h", "abc")).thenReturn(nativeQuery);
        when(nativeQuery.getSingleResult()).thenReturn(null);

        Optional<String> r = repo.buscarDadosPorHash("abc");

        assertTrue(r.isEmpty());
    }

    @Test
    void buscarDadosPorHash_noResult() {
        when(em.createNativeQuery("select dados from placar where hash_id = :h")).thenReturn(nativeQuery);
        when(nativeQuery.setParameter("h", "abc")).thenReturn(nativeQuery);
        when(nativeQuery.getSingleResult()).thenThrow(new NoResultException());

        Optional<String> r = repo.buscarDadosPorHash("abc");

        assertTrue(r.isEmpty());
    }
}
package com.solides.desafio.service;

import com.solides.desafio.domain.Time;
import com.solides.desafio.repository.TimeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.WebApplicationException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeServiceTest {

    @Mock
    TimeRepository repo;

    @InjectMocks
    TimeService service;

    @Test
    void criar_ok() {
        Time t = new Time();
        t.setNome("A");
        when(repo.findByNome("A")).thenReturn(Optional.empty());
        when(repo.save(t)).thenReturn(t);
        assertEquals(t, service.criar(t));
    }

    @Test
    void criar_nomeDuplicado() {
        Time t = new Time();
        t.setNome("A");
        when(repo.findByNome("A")).thenReturn(Optional.of(t));
        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> service.criar(t));
        assertEquals(400, ex.getResponse().getStatus());
    }

    @Test
    void listar_ok() {
        List<Time> lista = List.of(new Time(), new Time());
        when(repo.findAll()).thenReturn(lista);
        assertEquals(lista, service.listar());
    }

    @Test
    void buscar_ok() {
        Time t = new Time();
        when(repo.findById(1L)).thenReturn(Optional.of(t));
        assertEquals(Optional.of(t), service.buscar(1L));
    }

    @Test
    void buscar_naoEncontrado() {
        when(repo.findById(1L)).thenReturn(Optional.empty());
        assertEquals(Optional.empty(), service.buscar(1L));
    }

    @Test
    void atualizar_ok() {
        Time existente = new Time();
        existente.setNome("X");
        existente.setLogo("L1");

        Time novo = new Time();
        novo.setNome("Novo");
        novo.setLogo("L2");

        when(repo.findById(1L)).thenReturn(Optional.of(existente));

        Time res = service.atualizar(1L, novo);

        assertEquals("Novo", res.getNome());
        assertEquals("L2", res.getLogo());
    }

    @Test
    void atualizar_notFound() {
        when(repo.findById(1L)).thenReturn(Optional.empty());
        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> service.atualizar(1L, new Time()));
        assertEquals(404, ex.getResponse().getStatus());
    }

    @Test
    void deletar_ok() {
        Time t = new Time();
        when(repo.findById(1L)).thenReturn(Optional.of(t));
        service.deletar(1L);
        verify(repo, times(1)).delete(t);
    }

    @Test
    void deletar_naoExiste() {
        when(repo.findById(1L)).thenReturn(Optional.empty());
        service.deletar(1L);
        verify(repo, never()).delete(any());
    }
}


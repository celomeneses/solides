package com.solides.desafio.repository;

import com.solides.desafio.domain.Time;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeRepositoryTest {

    @Mock
    EntityManager em;

    @Mock
    TypedQuery<Time> typedQuery;

    @InjectMocks
    TimeRepository repo;

    @Test
    void save_ok() {
        Time t = new Time();
        Time r = repo.save(t);
        assertEquals(t, r);
        verify(em).persist(t);
    }

    @Test
    void findById_ok() {
        Time t = new Time();
        when(em.find(Time.class, 1L)).thenReturn(t);
        assertEquals(Optional.of(t), repo.findById(1L));
    }

    @Test
    void findById_empty() {
        when(em.find(Time.class, 1L)).thenReturn(null);
        assertEquals(Optional.empty(), repo.findById(1L));
    }

    @Test
    void delete_containsTrue() {
        Time t = new Time();
        when(em.contains(t)).thenReturn(true);
        repo.delete(t);
        verify(em).remove(t);
    }

    @Test
    void delete_containsFalse() {
        Time t = new Time();
        Time merged = new Time();
        when(em.contains(t)).thenReturn(false);
        when(em.merge(t)).thenReturn(merged);
        repo.delete(t);
        verify(em).remove(merged);
    }

    @Test
    void findAll_ok() {
        Time t1 = new Time();
        Time t2 = new Time();
        when(em.createQuery("from Time", Time.class)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(List.of(t1, t2));
        assertEquals(List.of(t1, t2), repo.findAll());
    }

    @Test
    void findByNome_ok() {
        Time t = new Time();
        when(em.createQuery("select t from Time t where t.nome=:n", Time.class)).thenReturn(typedQuery);
        when(typedQuery.setParameter("n", "A")).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenReturn(t);
        assertEquals(Optional.of(t), repo.findByNome("A"));
    }

    @Test
    void findByNome_empty() {
        when(em.createQuery("select t from Time t where t.nome=:n", Time.class)).thenReturn(typedQuery);
        when(typedQuery.setParameter("n", "A")).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenThrow(new RuntimeException());
        assertEquals(Optional.empty(), repo.findByNome("A"));
    }
}


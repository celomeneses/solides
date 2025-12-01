package com.solides.desafio.repository;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

import java.util.Optional;

@Repository
public class PlacarRepository {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public String iniciar(String json) {
        Object res = em.createNativeQuery("select sp_inicia_placar(:p)")
                .setParameter("p", json)
                .getSingleResult();
        return res != null ? res.toString() : null;
    }

    @Transactional
    public String atualizar(String hashId, String patchJson) {
        Object res = em.createNativeQuery("select sp_atualiza_placar(:h, :p)")
                .setParameter("h", hashId)
                .setParameter("p", patchJson)
                .getSingleResult();
        return res != null ? res.toString() : null;
    }

    @Transactional
    public void finalizar(String hashId) {
        try {
            em.createNativeQuery("select sp_finaliza_placar(:h)")
                    .setParameter("h", hashId)
                    .getSingleResult();
        } catch (NoResultException ignore) {
            // Ok: some drivers return no result for void functions
        }
    }

    @Transactional(readOnly = true)
    public Optional<String> buscarDadosPorHash(String hashId) {
        try {
            Object res = em.createNativeQuery("select dados from placar where hash_id = :h")
                    .setParameter("h", hashId)
                    .getSingleResult();
            return Optional.ofNullable(res != null ? res.toString() : null);
        } catch (NoResultException ex) {
            return Optional.empty();
        }
    }
}
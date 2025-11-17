package com.solides.desafio.repository;


import javax.enterprise.context.ApplicationScoped;
import javax.persistence.*;
import java.util.Optional;

@ApplicationScoped
public class PlacarRepository {

    @PersistenceContext
    EntityManager em;

    public String iniciar(String json){
        StoredProcedureQuery q = em.createStoredProcedureQuery("sp_inicia_placar");
        q.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
        q.setParameter(1, json);
        q.execute();
        return q.getSingleResult().toString();
    }

    public String atualizar(String hashId, String patchJson) {
        StoredProcedureQuery q = em.createStoredProcedureQuery("sp_atualiza_placar");
        q.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
        q.setParameter(1, hashId);
        q.setParameter(2, patchJson);
        q.execute();
        Object res = q.getSingleResult();
        return res != null ? res.toString() : null;
    }

    public void finalizar(String hashId) {
        StoredProcedureQuery q = em.createStoredProcedureQuery("sp_finaliza_placar");
        q.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
        q.setParameter(1, hashId);
        q.execute();
    }

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

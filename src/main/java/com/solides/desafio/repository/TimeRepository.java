package com.solides.desafio.repository;

import com.solides.desafio.domain.Time;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TimeRepository {

    @PersistenceContext
    EntityManager em;

    public Time save(Time t){
        em.persist(t); return t;
    }

    public Optional<Time> findById(Long id){
        return Optional.ofNullable(em.find(Time.class, id));
    }

    public void delete(Time t){
        em.remove(em.contains(t)?t:em.merge(t));
    }

    public List<Time> findAll(){
        return em.createQuery("from Time", Time.class).getResultList();
    }

    public Optional<Time> findByNome(String nome){
        try{
            Time t = em.createQuery("select t from Time t where t.nome=:n", Time.class).setParameter("n", nome).getSingleResult();
            return Optional.of(t);
        }catch(Exception e){ return Optional.empty(); }
    }
}

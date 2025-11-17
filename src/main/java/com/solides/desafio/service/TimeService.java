package com.solides.desafio.service;

import com.solides.desafio.domain.Time;
import com.solides.desafio.repository.TimeRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import java.util.*;


@ApplicationScoped
public class TimeService {
    @Inject
    TimeRepository repo;

    public Time criar(Time t){
        if(repo.findByNome(t.getNome()).isPresent())
            throw new WebApplicationException("Nome duplicado", 400);
        return repo.save(t);
    }


    public List<Time> listar(){ return repo.findAll(); }
    public Optional<Time> buscar(Long id){ return repo.findById(id); }
    public Time atualizar(Long id, Time t){
        Time existente = repo.findById(id).orElseThrow(() -> new WebApplicationException(404));
        existente.setNome(t.getNome());
        existente.setLogo(t.getLogo());
        return existente;
    }
    public void deletar(Long id){
        repo.findById(id).ifPresent(repo::delete);
    }
}

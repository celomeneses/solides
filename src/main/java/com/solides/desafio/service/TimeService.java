package com.solides.desafio.service;

import com.solides.desafio.domain.Time;
import com.solides.desafio.repository.TimeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TimeService {

    private final TimeRepository repo;

    public TimeService(TimeRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public Time criar(Time t){
        if (repo.findByNome(t.getNome()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome duplicado");
        }
        return repo.save(t);
    }

    public List<Time> listar(){
        return repo.findAll();
    }

    public Optional<Time> buscar(Long id){
        return repo.findById(id);
    }

    @Transactional
    public Time atualizar(Long id, Time t){
        Time existente = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Time não encontrado"));

        existente.setNome(t.getNome());
        existente.setLogo(t.getLogo());
        return existente;
    }

    @Transactional
    public void deletar(Long id){
        Time existente = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Time não encontrado"));

        repo.delete(existente);
    }
}

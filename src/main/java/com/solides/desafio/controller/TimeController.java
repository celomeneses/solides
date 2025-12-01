package com.solides.desafio.controller;

import com.solides.desafio.domain.Time;
import com.solides.desafio.service.TimeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/time")
public class TimeController {

    private final TimeService service;

    public TimeController(TimeService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Time> criar(@RequestBody Time t){
        Time created = service.criar(t);
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping
    public List<Time> listar(){ return service.listar(); }

    @GetMapping("/{id}")
    public ResponseEntity<Time> buscar(@PathVariable Long id){
        return service.buscar(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Time> atualizar(@PathVariable Long id, @RequestBody Time t){
        return ResponseEntity.ok(service.atualizar(id,t));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id){
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
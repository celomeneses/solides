package com.solides.desafio.domain;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "placar")
public class Placar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="hash_id", unique=true)
    private String hashId;

    @Column(columnDefinition="jsonb")
    private String dados;

    private String status;
}

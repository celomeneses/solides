package com.solides.desafio.domain;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "time")
public class Time {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nome;

    @Column(columnDefinition = "text")
    private String logo;

}

package com.esprit.microservice.ms_job_board.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Salle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nom requis")
    @Size(max = 100, message = "Nom trop long")
    private String nom;

    @Positive(message = "Prix doit être positif")
    private double prix;

    @Min(value = 1, message = "Capacité minimale 1")
    private int capacite;

    @Enumerated(EnumType.STRING)
    private StatutSalle status = StatutSalle.DISPONIBLE;

    private String imagePath;

    @OneToMany(mappedBy = "salle", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Reservation> reservations;
}
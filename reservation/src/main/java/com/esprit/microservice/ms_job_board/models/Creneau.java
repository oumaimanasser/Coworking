package com.esprit.microservice.ms_job_board.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Creneau {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Debut requis")
    private LocalDateTime debut;

    @NotNull(message = "Fin requise")
    private LocalDateTime fin;

    @NotNull(message = "Salle requise")
    @ManyToOne
    @JoinColumn(name = "salle_id")
    private Salle salle;
}
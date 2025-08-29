package com.esprit.microservice.ms_job_board.models;

import jakarta.persistence.*;
import lombok.*;

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

    private LocalDateTime debut;
    private LocalDateTime fin;

    @ManyToOne
    @JoinColumn(name = "salle_id")
    private Salle salle;
}

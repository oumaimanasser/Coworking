package com.esprit.microservice.ms_job_board.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int nombrePersonnes;
    private String clientName;
    @Column(nullable = false)
    private String clientEmail;
    @ManyToOne
    @JoinColumn(name = "salle_id")
    private Salle salle;
    private LocalDateTime dateReservation;
    @ManyToOne
    @JoinColumn(name = "creneau_id")
    private Creneau creneau;
    @Enumerated(EnumType.STRING)
    private PaiementStatus paiementStatus = PaiementStatus.EN_ATTENTE;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status = ReservationStatus.PENDING;
}
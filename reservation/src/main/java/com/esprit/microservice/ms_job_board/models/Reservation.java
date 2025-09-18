
        package com.esprit.microservice.ms_job_board.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Min(value = 1, message = "Nombre de personnes doit être au moins 1")
    @Max(value = 1000, message = "Nombre de personnes trop élevé")
    private int nombrePersonnes;

    @NotBlank(message = "Nom du client requis")
    @Size(max = 50, message = "Nom trop long")
    private String clientName;

    @NotBlank(message = "Email du client requis")
    @Email(message = "Email invalide")
    @Column(nullable = false)
    private String clientEmail;

    @NotNull(message = "Salle requise")
    @ManyToOne
    @JoinColumn(name = "salle_id")
    private Salle salle;

    private LocalDateTime dateReservation;

    @NotNull(message = "Creneau requis")
    @ManyToOne
    @JoinColumn(name = "creneau_id")
    private Creneau creneau;

    @Enumerated(EnumType.STRING)
    private PaiementStatus paiementStatus = PaiementStatus.EN_ATTENTE;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status = ReservationStatus.PENDING;
}

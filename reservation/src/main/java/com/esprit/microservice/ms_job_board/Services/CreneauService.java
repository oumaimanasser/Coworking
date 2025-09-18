package com.esprit.microservice.ms_job_board.Services;

import com.esprit.microservice.ms_job_board.Repositories.CreneauRepository;
import com.esprit.microservice.ms_job_board.Repositories.ReservationRepository;
import com.esprit.microservice.ms_job_board.models.Creneau;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CreneauService {
    private final CreneauRepository repo;
    private final ReservationRepository reservationRepo;

    public CreneauService(CreneauRepository repo, ReservationRepository reservationRepo) {
        this.repo = repo;
        this.reservationRepo = reservationRepo;
    }

    public Creneau ajouter(Creneau creneau) {
        return repo.save(creneau);
    }

    public void supprimer(Long id) {
        repo.deleteById(id);
    }

    public List<Creneau> listerTous() {
        return repo.findAll();
    }

    public List<Creneau> rechercherDisponibles() {
        return repo.findByDebutAfter(LocalDateTime.now())
                .stream()
                .filter(c -> {
                    int reservationsCount = reservationRepo.countByCreneau(c);
                    return reservationsCount < c.getSalle().getCapacite();
                })
                .toList();
    }
}

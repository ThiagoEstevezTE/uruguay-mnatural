package uy.edu.um.proyectotic2026.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import uy.edu.um.proyectotic2026.entities.ActivityInstance;

import java.util.Optional;

public interface ActivityInstanceRepository extends JpaRepository<ActivityInstance, Long> {

    // Trae la fecha y la bloquea hasta que termine la transaccion (SELECT ... FOR UPDATE).
    // Si dos turistas reservan la misma fecha a la vez, el segundo espera para calcular el cupo.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ActivityInstance> findLockedById(Long id);

    boolean existsByCancellationReasonId(Long reasonId);
}

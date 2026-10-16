package uy.edu.um.proyectotic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.um.proyectotic2026.entities.Booking;
import uy.edu.um.proyectotic2026.entities.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByCode(String code);

    Optional<Booking> findByCode(String code);

    List<Booking> findByTouristEmailOrderByCreatedAtDesc(String email);

    List<Booking> findByInstanceId(Long instanceId);

    // Reservas de todas las actividades de un prestador, por fecha de la actividad
    List<Booking> findByInstanceActivityBusinessProviderEmailOrderByInstanceStartDateTimeAsc(String providerEmail);

    // Pendientes que ya vencieron (para cancelarlas automaticamente)
    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime date);
}

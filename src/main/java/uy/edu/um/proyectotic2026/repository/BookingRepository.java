package uy.edu.um.proyectotic2026.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import uy.edu.um.proyectotic2026.entities.Booking;
import uy.edu.um.proyectotic2026.entities.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByCode(String code);

    Optional<Booking> findByCode(String code);

    // Trae la reserva y la bloquea hasta que termine la transaccion (SELECT ... FOR UPDATE).
    // Si llegan dos pagos de la misma reserva a la vez, el segundo espera a que termine el primero.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Booking> findLockedById(Long id);

    // Las listas traen la valoracion de cada reserva en la misma consulta (si no, seria una consulta por reserva)
    @EntityGraph(attributePaths = {"rating", "refund"})
    List<Booking> findByTouristEmailOrderByCreatedAtDesc(String email);

    List<Booking> findByInstanceId(Long instanceId);

    // Las reservas de una fecha, bloqueadas: si el turista cancela justo cuando el prestador cancela la fecha,
    // uno espera al otro y no se devuelve dos veces
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Booking> findLockedByInstanceId(Long instanceId);

    // Reservas de todas las actividades de un prestador, por fecha de la actividad
    @EntityGraph(attributePaths = {"rating", "refund"})
    List<Booking> findByInstanceActivityBusinessProviderEmailOrderByInstanceStartDateTimeAsc(String providerEmail);

    // Pendientes que ya vencieron (para cancelarlas automaticamente).
    // Tambien se bloquean: asi no se cancela una reserva mientras se esta pagando.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime date);
}

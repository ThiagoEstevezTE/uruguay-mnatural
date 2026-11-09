package uy.edu.um.proyectotic2026.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// INSTANCIA_ACTIVIDAD del MER: una fecha y hora concreta en la que se hace una actividad
@Entity
@Table(name = "activity_instances")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private InstanceStatus status = InstanceStatus.SCHEDULED;

    // Opcional: null = sin cupo (Acta 1, punto 4.3)
    private Integer maxCapacity;

    private LocalDateTime cancellationDate;

    // Por que la cancelo el prestador (lo elige de la lista que carga el administrador)
    @ManyToOne
    @JoinColumn(name = "cancellation_reason_id")
    private CancellationReason cancellationReason;

    public boolean isFuture() {
        return startDateTime.isAfter(LocalDateTime.now());
    }

    // Hasta que hora se puede reservar, segun los minutos limite de la actividad
    public LocalDateTime getBookingDeadline() {
        return startDateTime.minusMinutes(activity.getBookingLimitMinutes());
    }
}

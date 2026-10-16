package uy.edu.um.proyectotic2026.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// RESERVA del MER
@Entity
@Table(name = "bookings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "instance_id", nullable = false)
    private ActivityInstance instance;

    @ManyToOne
    @JoinColumn(name = "tourist_id", nullable = false)
    private Tourist tourist;

    // Numero de reserva que se le da al turista (y con el que se arma el QR)
    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private int participants;

    // Observaciones para el prestador (Acta 1, punto 4.4)
    @Column(length = 1000)
    private String observations;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Hasta cuando se puede pagar (2 horas despues de crearla)
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    private CancelledBy cancelledBy;

    private LocalDateTime cancellationDate;

    // Cuando el prestador registro la asistencia
    private LocalDateTime checkinDate;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    @OrderBy("date ASC")
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @OneToOne(mappedBy = "booking")
    private Rating rating;

    public Activity getActivity() {
        return instance.getActivity();
    }
}

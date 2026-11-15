package uy.edu.um.proyectotic2026.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// REEMBOLSO del MER: la plata que se le devuelve al turista cuando se cancela una reserva pagada.
// Si cancela el turista, se devuelve el porcentaje que fijo el prestador en la actividad;
// si cancela el prestador, el 100 %.
@Entity
@Table(name = "refunds")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Una reserva tiene como mucho un reembolso (unique: una doble cancelacion no puede devolver dos veces)
    @OneToOne
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    // El pago aprobado que se devuelve
    @ManyToOne
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private int percentage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status;

    // Identificador que devuelve el servicio de pagos
    private String externalRefundId;

    private String message;

    @Builder.Default
    @Column(nullable = false)
    private int attempts = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime refundedAt;
}

package uy.edu.um.proyectotic2026.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// PAGO del MER: cada intento de pago queda registrado, salga bien o mal
@Entity
@Table(name = "payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    // Identificador que devuelve el servicio de pagos externo (en Mercado Pago, el de la orden)
    private String externalTransactionId;

    // En Mercado Pago, el pago adentro de la orden: hace falta para devolver una parte
    private String externalPaymentId;

    // Quien lo cobro: la devolucion se le pide al mismo
    @Enumerated(EnumType.STRING)
    private PaymentProvider provider;

    // De la tarjeta solo se guardan la marca y los ultimos 4 numeros
    private String cardBrand;

    private String cardLastDigits;

    private String message;

    @Column(nullable = false)
    private LocalDateTime date;
}

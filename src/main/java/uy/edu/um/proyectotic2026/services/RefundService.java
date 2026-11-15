package uy.edu.um.proyectotic2026.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyectotic2026.entities.*;
import uy.edu.um.proyectotic2026.exceptions.PaymentUnavailableException;
import uy.edu.um.proyectotic2026.repository.RefundRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

// Devoluciones de plata cuando se cancela una reserva pagada (pedido del cliente):
//   - si cancela el turista, se devuelve el porcentaje que fijo el prestador en la actividad;
//   - si cancela el prestador, se devuelve el 100 %.
// La devolucion se le pide al mismo servicio que cobro (Mercado Pago o el simulador).
@Service
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

    // Despues de tantos intentos fallidos se deja de reintentar solo (queda en el log para revisarlo a mano)
    static final int MAX_ATTEMPTS = 12;

    @Autowired
    private RefundRepository refundRepo;

    @Autowired
    private MercadoPagoClient mercadoPago;

    @Autowired
    private PaymentGatewayClient paymentGateway;

    // Porcentaje que corresponde segun quien cancela
    public int refundPercentage(Booking booking, CancelledBy who) {
        return who == CancelledBy.PROVIDER ? 100 : booking.getActivity().getTouristRefundPercentage();
    }

    // Lo que se le devolveria al turista si cancela ahora (para avisarle antes de que confirme)
    public BigDecimal touristRefundAmount(Booking booking) {
        return amountFor(booking.getTotalAmount(), booking.getActivity().getTouristRefundPercentage());
    }

    // Se llama al cancelar. Si la reserva no estaba pagada o el porcentaje es 0, no hay nada que devolver.
    @Transactional
    public Refund refundCancelledBooking(Booking booking, CancelledBy who) {
        Payment payment = booking.getApprovedPayment();
        if (payment == null || booking.getRefund() != null)
            return null;

        int percentage = refundPercentage(booking, who);
        BigDecimal amount = amountFor(payment.getAmount(), percentage);
        if (amount.signum() <= 0)
            return null;

        Refund refund = Refund.builder()
                .booking(booking)
                .payment(payment)
                .amount(amount)
                .percentage(percentage)
                .status(RefundStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        booking.setRefund(refund);
        tryRefund(refund);
        return refundRepo.save(refund);
    }

    // Si el servicio de pagos no respondio, se vuelve a intentar cada 5 minutos
    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    @Transactional
    public void retryPendingRefunds() {
        for (Refund refund : refundRepo.findByStatus(RefundStatus.PENDING)) {
            if (refund.getAttempts() < MAX_ATTEMPTS) {
                tryRefund(refund);
                refundRepo.save(refund);
            }
        }
    }

    private void tryRefund(Refund refund) {
        Payment payment = refund.getPayment();
        boolean total = refund.getAmount().compareTo(payment.getAmount()) >= 0;
        refund.setAttempts(refund.getAttempts() + 1);
        try {
            String refundId = chargedByMercadoPago(payment)
                    ? mercadoPago.refund(payment.getExternalTransactionId(), payment.getExternalPaymentId(),
                            refund.getAmount(), total)
                    : paymentGateway.refund(payment.getExternalTransactionId(), refund.getAmount());

            refund.setStatus(RefundStatus.REFUNDED);
            refund.setExternalRefundId(refundId);
            refund.setRefundedAt(LocalDateTime.now());
            refund.setMessage("Devolución aprobada");
        } catch (PaymentUnavailableException e) {
            // La reserva queda cancelada igual; la devolucion se reintenta sola
            log.warn("Devolucion pendiente de la reserva {} (intento {}): {}",
                    refund.getBooking().getCode(), refund.getAttempts(), e.getMessage());
            refund.setMessage(e.getMessage());
        }
    }

    // Pagos anteriores a guardar el proveedor: los del simulador tienen ids que empiezan con "txn_"
    private boolean chargedByMercadoPago(Payment payment) {
        if (payment.getProvider() != null)
            return payment.getProvider() == PaymentProvider.MERCADO_PAGO;
        return payment.getExternalTransactionId() != null && !payment.getExternalTransactionId().startsWith("txn_");
    }

    private BigDecimal amountFor(BigDecimal total, int percentage) {
        return total.multiply(BigDecimal.valueOf(percentage))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}

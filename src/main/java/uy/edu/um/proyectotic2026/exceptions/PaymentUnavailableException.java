package uy.edu.um.proyectotic2026.exceptions;

// El servicio de pagos externo no respondio o fallo
public class PaymentUnavailableException extends Exception {

    public PaymentUnavailableException(String msg) {
        super(msg);
    }
}

package uy.edu.um.proyectotic2026.entities;

public enum RefundStatus {

    REFUNDED,   // el servicio de pagos devolvio la plata
    PENDING     // el servicio no respondio: se reintenta solo cada 5 minutos

}

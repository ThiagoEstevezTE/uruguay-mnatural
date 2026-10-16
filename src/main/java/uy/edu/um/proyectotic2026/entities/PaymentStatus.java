package uy.edu.um.proyectotic2026.entities;

public enum PaymentStatus {

    APPROVED,
    REJECTED,   // el servicio de pagos lo rechazo (por ejemplo, sin fondos)
    ERROR       // el servicio de pagos no respondio

}

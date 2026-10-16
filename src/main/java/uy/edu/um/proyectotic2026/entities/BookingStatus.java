package uy.edu.um.proyectotic2026.entities;

// Estados acordados en el Acta 1, punto 4.8
public enum BookingStatus {

    PENDING,     // generada, esperando el pago (maximo 2 horas)
    CONFIRMED,   // pago aprobado
    CANCELLED,   // la cancelo el turista o el prestador, o vencio sin pagar
    COMPLETED    // el prestador registro la asistencia el dia de la actividad

}

package uy.edu.um.proyectotic2026.entities;

import jakarta.persistence.*;
import lombok.*;

// Cada persona que va a la actividad con una reserva: el prestador sabe quien viene y su edad
@Entity
@Table(name = "booking_participants")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 20)
    private String document;

    @Column(nullable = false)
    private int age;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}

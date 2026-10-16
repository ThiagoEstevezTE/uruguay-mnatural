package uy.edu.um.proyectotic2026.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// VALORACION del MER: una por reserva, despues de la actividad
@Entity
@Table(name = "ratings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    // De 1 a 5
    @Column(nullable = false)
    private int score;

    @Column(length = 1000)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime date;
}

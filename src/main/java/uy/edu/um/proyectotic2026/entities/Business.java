package uy.edu.um.proyectotic2026.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "businesses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @ManyToOne
    @JoinColumn(name = "locality_id", nullable = false)
    private Locality locality;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    private String address;

    private String phone;

    private String email;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private BusinessStatus status = BusinessStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime requestDate;

    private LocalDateTime resolutionDate;

    private String rejectionReason;
}
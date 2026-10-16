package uy.edu.um.proyectotic2026.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyectotic2026.entities.Booking;
import uy.edu.um.proyectotic2026.entities.BookingStatus;
import uy.edu.um.proyectotic2026.entities.Rating;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.repository.RatingRepository;

import java.time.LocalDateTime;
import java.util.List;

// Comentarios y valoraciones de 1 a 5 (letra y Acta 1, punto 4.9)
@Service
public class RatingService {

    @Autowired
    private RatingRepository ratingRepo;

    @Autowired
    private BookingService bookingService;

    // Se puede valorar si la reserva se pago (o se registro la asistencia),
    // la actividad ya paso y todavia no la valoro
    public boolean canRate(Booking booking) {
        return (booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.COMPLETED)
                && booking.getInstance().getStartDateTime().isBefore(LocalDateTime.now())
                && booking.getRating() == null;
    }

    @Transactional
    public Rating rate(Long bookingId, String touristEmail, Integer score, String comment)
            throws EntityNotFoundException, InvalidDataException {

        Booking booking = bookingService.getBookingOfTourist(bookingId, touristEmail);

        if (!canRate(booking))
            throw new InvalidDataException("Solo se puede valorar una actividad en la que participaste, una vez");
        if (score == null || score < 1 || score > 5)
            throw new InvalidDataException("La valoración tiene que ser de 1 a 5");

        Rating rating = Rating.builder()
                .booking(booking)
                .score(score)
                .comment(comment == null || comment.isBlank() ? null : comment.trim())
                .date(LocalDateTime.now())
                .build();
        booking.setRating(rating);
        return ratingRepo.save(rating);
    }

    public List<Rating> getRatingsOfActivity(Long activityId) {
        return ratingRepo.findByBookingInstanceActivityIdOrderByDateDesc(activityId);
    }

    // Promedio con un decimal, o null si todavia no tiene valoraciones
    public Double getAverage(List<Rating> ratings) {
        if (ratings.isEmpty())
            return null;
        double average = ratings.stream().mapToInt(Rating::getScore).average().orElse(0);
        return Math.round(average * 10) / 10.0;
    }
}

package uy.edu.um.proyectotic2026.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyectotic2026.entities.Booking;
import uy.edu.um.proyectotic2026.entities.Rating;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.repository.BookingRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Valoraciones, con las reservas de ejemplo del DataLoader
@SpringBootTest
@Transactional
class RatingServiceTest {

    @Autowired
    private RatingService ratingService;

    @Autowired
    private BookingRepository bookingRepo;

    // Reserva confirmada de una actividad que ya paso y todavia no se valoro
    private Booking pastBooking() {
        return bookingRepo.findByCode("UM-KAY4K2").orElseThrow();
    }

    @Test
    void ratesAPastActivity() throws Exception {
        Rating rating = ratingService.rate(pastBooking().getId(), "turista@demo.uy", 5, " ¡Excelente! ");

        assertEquals(5, rating.getScore());
        assertEquals("¡Excelente!", rating.getComment());
    }

    @Test
    void cannotRateTwice() throws Exception {
        Long id = pastBooking().getId();
        ratingService.rate(id, "turista@demo.uy", 4, null);

        assertThrows(InvalidDataException.class, () -> ratingService.rate(id, "turista@demo.uy", 5, null));
    }

    @Test
    void scoreMustBeFromOneToFive() {
        Long id = pastBooking().getId();
        assertThrows(InvalidDataException.class, () -> ratingService.rate(id, "turista@demo.uy", 6, null));
        assertThrows(InvalidDataException.class, () -> ratingService.rate(id, "turista@demo.uy", 0, null));
    }

    @Test
    void cannotRateBeforeTheActivity() {
        Booking future = bookingRepo.findByCode("UM-PROX7A").orElseThrow();
        assertTrue(future.getInstance().getStartDateTime().isAfter(LocalDateTime.now()));

        assertThrows(InvalidDataException.class, () -> ratingService.rate(future.getId(), "turista@demo.uy", 5, null));
    }

    @Test
    void averageHasOneDecimal() {
        List<Rating> ratings = List.of(Rating.builder().score(5).build(), Rating.builder().score(4).build(),
                Rating.builder().score(4).build());

        assertEquals(4.3, ratingService.getAverage(ratings));
        assertNull(ratingService.getAverage(List.of()));
    }
}

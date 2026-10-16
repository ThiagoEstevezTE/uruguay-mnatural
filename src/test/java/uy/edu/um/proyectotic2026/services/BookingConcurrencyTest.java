package uy.edu.um.proyectotic2026.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uy.edu.um.proyectotic2026.dto.ChargeResponseDTO;
import uy.edu.um.proyectotic2026.entities.*;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.repository.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// Dos pedidos al mismo tiempo (doble clic o dos turistas a la vez).
// Sin @Transactional: cada llamada tiene su propia transaccion, como en la app real.
@SpringBootTest
class BookingConcurrencyTest {

    private static final String TOURIST = "turista@demo.uy";

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ActivityRepository activityRepo;

    @Autowired
    private ActivityInstanceRepository instanceRepo;

    // Repositorio real, pero en un test se le agrega una demora para provocar el choque
    @MockitoSpyBean
    private BookingRepository bookingRepo;

    @MockitoBean
    private PaymentGatewayClient paymentGateway;

    private ActivityInstance instance;

    @BeforeEach
    void setUp() {
        Activity trekking = activityRepo.findAll().stream()
                .filter(a -> a.getName().startsWith("Trekking")).findFirst().orElseThrow();
        instance = instanceRepo.save(ActivityInstance.builder()
                .activity(trekking).startDateTime(LocalDateTime.now().plusDays(5)).build());
    }

    // Se borra lo creado para no molestar a los otros tests
    @AfterEach
    void cleanUp() {
        bookingRepo.deleteAll(bookingRepo.findByInstanceId(instance.getId()));
        instanceRepo.deleteById(instance.getId());
    }

    // Corre las dos tareas a la vez y devuelve lo que paso con cada una (el resultado o la excepcion)
    private List<Object> runTogether(Callable<?> first, Callable<?> second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (Callable<?> task : List.of(first, second)) {
            futures.add(pool.submit(() -> {
                start.await();
                return task.call();
            }));
        }
        start.countDown();
        List<Object> results = new ArrayList<>();
        for (Future<?> future : futures) {
            try {
                results.add(future.get(20, TimeUnit.SECONDS));
            } catch (ExecutionException e) {
                results.add(e.getCause());
            }
        }
        pool.shutdown();
        return results;
    }

    @Test
    void doubleClickOnPayChargesOnlyOnce() throws Exception {
        // El servicio de pagos tarda un poco, como uno real
        when(paymentGateway.charge(anyString(), any(), anyString())).thenAnswer(call -> {
            Thread.sleep(300);
            return new ChargeResponseDTO("approved", "txn_1", "Pago aprobado");
        });
        Booking booking = bookingService.createBooking(instance.getId(), TOURIST, 1, null);

        List<Object> results = runTogether(
                () -> bookingService.payBooking(booking.getId(), TOURIST, "4242424242424242"),
                () -> bookingService.payBooking(booking.getId(), TOURIST, "4242424242424242"));

        verify(paymentGateway, times(1)).charge(anyString(), any(), anyString());
        assertEquals(1, results.stream().filter(r -> r instanceof Booking).count());
        assertEquals(1, results.stream().filter(r -> r instanceof InvalidDataException).count());
        assertEquals(BookingStatus.CONFIRMED, bookingRepo.findById(booking.getId()).orElseThrow().getStatus());
    }

    @Test
    void twoBookingsAtTheSameTimeDoNotExceedCapacity() throws Exception {
        instance.setMaxCapacity(1);
        instanceRepo.save(instance);
        // Entre contar los lugares y guardar se genera el numero de reserva: ahi se espera un poco,
        // asi las dos reservas cuentan los lugares antes de que se guarde alguna
        doAnswer(call -> {
            Thread.sleep(300);
            return false;
        }).when(bookingRepo).existsByCode(anyString());

        List<Object> results = runTogether(
                () -> bookingService.createBooking(instance.getId(), TOURIST, 1, null),
                () -> bookingService.createBooking(instance.getId(), TOURIST, 1, null));

        assertEquals(1, results.stream().filter(r -> r instanceof Booking).count());
        assertEquals(1, results.stream().filter(r -> r instanceof InvalidDataException).count());
    }
}

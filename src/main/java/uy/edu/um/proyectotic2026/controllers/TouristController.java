package uy.edu.um.proyectotic2026.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uy.edu.um.proyectotic2026.entities.ActivityInstance;
import uy.edu.um.proyectotic2026.dto.MercadoPagoPaymentDTO;
import uy.edu.um.proyectotic2026.entities.Booking;
import uy.edu.um.proyectotic2026.entities.BookingStatus;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.exceptions.PaymentUnavailableException;
import uy.edu.um.proyectotic2026.services.BookingService;
import uy.edu.um.proyectotic2026.services.QrCodeService;
import uy.edu.um.proyectotic2026.services.RatingService;

import java.security.Principal;

// Lo que hace el turista con su cuenta: reservar, pagar, ver, cancelar y valorar
@Controller
@RequestMapping("/tourist")
public class TouristController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RatingService ratingService;

    @Autowired
    private QrCodeService qrCodeService;

    // ---------- Reservar ----------

    @GetMapping("/book/{instanceId}")
    public String bookForm(@PathVariable("instanceId") Long instanceId, Model model, RedirectAttributes redirect)
            throws EntityNotFoundException {
        try {
            ActivityInstance instance = bookingService.getInstanceToBook(instanceId);
            model.addAttribute("instance", instance);
            model.addAttribute("activity", instance.getActivity());
            return "tourist/book";
        } catch (InvalidDataException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }

    @PostMapping("/book/{instanceId}")
    public String book(@PathVariable("instanceId") Long instanceId,
                       @RequestParam(value = "participants", required = false) Integer participants,
                       @RequestParam(value = "observations", required = false) String observations,
                       Principal principal, Model model) throws EntityNotFoundException {
        try {
            Booking booking = bookingService.createBooking(instanceId, principal.getName(), participants, observations);
            return "redirect:/tourist/bookings/" + booking.getId() + "/pay";
        } catch (InvalidDataException e) {
            // Se vuelve al formulario con el error y lo que habia escrito
            model.addAttribute("error", e.getMessage());
            model.addAttribute("participants", participants);
            model.addAttribute("observations", observations);
            try {
                ActivityInstance instance = bookingService.getInstanceToBook(instanceId);
                model.addAttribute("instance", instance);
                model.addAttribute("activity", instance.getActivity());
                return "tourist/book";
            } catch (InvalidDataException ex) {
                return "redirect:/";
            }
        }
    }

    // ---------- Pagar ----------

    @Value("${payment.provider:simulator}")
    private String paymentProvider;

    // La Public Key se puede mostrar en la pantalla; el Access Token, nunca.
    @Value("${mercadopago.public-key:}")
    private String mercadoPagoPublicKey;

    private boolean usingMercadoPago() {
        return "mercadopago".equals(paymentProvider);
    }

    @GetMapping("/bookings/{id}/pay")
    public String payForm(@PathVariable("id") Long id, Model model, Principal principal)
            throws EntityNotFoundException {
        model.addAttribute("booking", bookingService.getBookingOfTourist(id, principal.getName()));
        model.addAttribute("usingMercadoPago", usingMercadoPago());
        model.addAttribute("mercadoPagoPublicKey", mercadoPagoPublicKey);
        return "tourist/pay";
    }

    @PostMapping("/bookings/{id}/pay")
    public String pay(@PathVariable("id") Long id, @RequestParam("cardNumber") String cardNumber,
                      Principal principal, RedirectAttributes redirect) throws EntityNotFoundException {
        // Con Mercado Pago prendido, el simulador no puede confirmar reservas
        if (usingMercadoPago()) {
            redirect.addFlashAttribute("error", "El pago se hace con el formulario de Mercado Pago");
            return "redirect:/tourist/bookings/" + id + "/pay";
        }
        try {
            Booking booking = bookingService.payBooking(id, principal.getName(), cardNumber);
            redirect.addFlashAttribute("message", "¡Pago aprobado! Tu reserva está confirmada. "
                    + "Te enviamos la confirmación por email con el número de reserva " + booking.getCode() + ".");
            return "redirect:/tourist/bookings/" + id;
        } catch (InvalidDataException | PaymentUnavailableException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/tourist/bookings/" + id + "/pay";
        }
    }

    @PostMapping("/bookings/{id}/pay-mp")
    public String payWithMercadoPago(@PathVariable("id") Long id, @ModelAttribute MercadoPagoPaymentDTO data,
                                     Principal principal, RedirectAttributes redirect)
            throws EntityNotFoundException {
        try {
            Booking booking = bookingService.payBookingWithMercadoPago(id, principal.getName(), data);
            redirect.addFlashAttribute("message", "¡Pago aprobado! Tu reserva está confirmada. "
                    + "Número de reserva: " + booking.getCode() + ".");
            return "redirect:/tourist/bookings/" + id;
        } catch (InvalidDataException | PaymentUnavailableException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/tourist/bookings/" + id + "/pay";
        }
    }

    // ---------- Mis reservas ----------

    @GetMapping("/bookings")
    public String bookings(Model model, Principal principal) {
        model.addAttribute("bookings", bookingService.getBookingsOfTourist(principal.getName()));
        return "tourist/bookings";
    }

    @GetMapping("/bookings/{id}")
    public String bookingDetail(@PathVariable("id") Long id, Model model, Principal principal)
            throws EntityNotFoundException {
        Booking booking = bookingService.getBookingOfTourist(id, principal.getName());
        model.addAttribute("booking", booking);
        model.addAttribute("canCancel", bookingService.canTouristCancel(booking));
        model.addAttribute("canRate", ratingService.canRate(booking));
        return "tourist/booking";
    }

    // Imagen del QR con el numero de reserva. Solo para reservas confirmadas o realizadas.
    @GetMapping(value = "/bookings/{id}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public byte[] qrCode(@PathVariable("id") Long id, Principal principal) throws Exception {
        Booking booking = bookingService.getBookingOfTourist(id, principal.getName());
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.COMPLETED)
            throw new EntityNotFoundException("La reserva todavía no está confirmada");
        return qrCodeService.generatePng(booking.getCode(), 240);
    }

    @PostMapping("/bookings/{id}/cancel")
    public String cancel(@PathVariable("id") Long id, Principal principal, RedirectAttributes redirect) {
        try {
            bookingService.cancelByTourist(id, principal.getName());
            redirect.addFlashAttribute("message", "Cancelaste la reserva.");
        } catch (InvalidDataException | EntityNotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tourist/bookings/" + id;
    }

    @PostMapping("/bookings/{id}/rate")
    public String rate(@PathVariable("id") Long id,
                       @RequestParam(value = "score", required = false) Integer score,
                       @RequestParam(value = "comment", required = false) String comment,
                       Principal principal, RedirectAttributes redirect) {
        try {
            ratingService.rate(id, principal.getName(), score, comment);
            redirect.addFlashAttribute("message", "¡Gracias por tu valoración!");
        } catch (InvalidDataException | EntityNotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tourist/bookings/" + id;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(EntityNotFoundException e, Model model) {
        model.addAttribute("error", e.getMessage());
        return "error/not-found";
    }
}

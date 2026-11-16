package uy.edu.um.proyectotic2026.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uy.edu.um.proyectotic2026.dto.ActivityDTO;
import uy.edu.um.proyectotic2026.dto.BusinessDTO;
import uy.edu.um.proyectotic2026.entities.Activity;
import uy.edu.um.proyectotic2026.entities.ActivityPhoto;
import uy.edu.um.proyectotic2026.entities.Booking;
import uy.edu.um.proyectotic2026.entities.Business;
import uy.edu.um.proyectotic2026.entities.BusinessStatus;
import uy.edu.um.proyectotic2026.entities.Provider;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.services.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

// Espacio de gestion del prestador
@Controller
@RequestMapping("/provider")
public class ProviderWebController {

    @Autowired
    private ProviderService providerService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private ActivityPhotoService photoService;

    @Autowired
    private LocationService locationService;

    @Autowired
    private ActivityTypeService typeService;

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BusinessService businessService;

    @Autowired
    private CancellationReasonService reasonService;

    @GetMapping
    public String home(Model model, Principal principal) throws EntityNotFoundException {
        // principal.getName() es el email de quien inicio sesion
        Provider provider = providerService.getByEmail(principal.getName());
        model.addAttribute("provider", provider);
        model.addAttribute("businesses", provider.getBusinesses());
        model.addAttribute("activities", activityService.getActivitiesOfProvider(principal.getName()));
        model.addAttribute("canPublish", !getApprovedBusinesses(provider).isEmpty());
        return "provider/home";
    }

    // ---------- Emprendimiento ----------

    @GetMapping("/businesses/{id}/edit")
    public String editBusiness(@PathVariable("id") Long id, Model model, Principal principal)
            throws EntityNotFoundException {

        model.addAttribute("form", BusinessDTO.from(businessService.getBusinessOfProvider(id, principal.getName())));
        model.addAttribute("businessId", id);
        model.addAttribute("localitiesByDepartment", locationService.getLocalitiesByDepartment());
        return "provider/business-form";
    }

    @PostMapping("/businesses/{id}/edit")
    public String updateBusiness(@PathVariable("id") Long id, @ModelAttribute("form") BusinessDTO form,
                                 Model model, Principal principal, RedirectAttributes redirect)
            throws EntityNotFoundException {
        try {
            businessService.updateBusiness(id, form, principal.getName());
            redirect.addFlashAttribute("message", "Cambios guardados.");
            return "redirect:/provider";
        } catch (InvalidDataException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("businessId", id);
            model.addAttribute("localitiesByDepartment", locationService.getLocalitiesByDepartment());
            return "provider/business-form";
        }
    }

    // ---------- Actividades ----------

    @GetMapping("/activities/new")
    public String newActivity(Model model, Principal principal, RedirectAttributes redirect)
            throws EntityNotFoundException {

        Provider provider = providerService.getByEmail(principal.getName());
        List<Business> approved = getApprovedBusinesses(provider);
        if (approved.isEmpty()) {
            redirect.addFlashAttribute("error", "Para publicar actividades tu emprendimiento tiene que estar aprobado.");
            return "redirect:/provider";
        }

        ActivityDTO form = new ActivityDTO();
        form.setBusinessId(approved.get(0).getId());
        form.setLocalityId(approved.get(0).getLocality().getId());
        model.addAttribute("form", form);
        addActivityFormData(model, provider);
        return "provider/activity-form";
    }

    @PostMapping("/activities/new")
    public String createActivity(@ModelAttribute("form") ActivityDTO form, Model model, Principal principal,
                                 @RequestParam(value = "photos", required = false) List<MultipartFile> files,
                                 RedirectAttributes redirect) throws EntityNotFoundException {
        try {
            // Las fotos se controlan antes de crear la actividad: si alguna esta mal, no se crea nada
            List<ActivityPhoto> photos = photoService.check(files);
            Activity activity = activityService.createActivity(form, principal.getName());
            photoService.replacePhotos(activity, photos);
            redirect.addFlashAttribute("message", "Actividad creada. Ahora agregale fechas y horarios.");
            return "redirect:/provider/activities/" + activity.getId();
        } catch (InvalidDataException | EntityNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            addActivityFormData(model, providerService.getByEmail(principal.getName()));
            return "provider/activity-form";
        }
    }

    @GetMapping("/activities/{id}")
    public String activityDetail(@PathVariable("id") Long id, Model model, Principal principal)
            throws EntityNotFoundException {

        model.addAttribute("activity", activityService.getActivityOfProvider(id, principal.getName()));
        model.addAttribute("reasons", reasonService.getAll());
        return "provider/activity";
    }

    @GetMapping("/activities/{id}/edit")
    public String editActivity(@PathVariable("id") Long id, Model model, Principal principal)
            throws EntityNotFoundException {

        Activity activity = activityService.getActivityOfProvider(id, principal.getName());
        model.addAttribute("form", ActivityDTO.from(activity));
        model.addAttribute("activityId", id);
        addActivityFormData(model, providerService.getByEmail(principal.getName()));
        return "provider/activity-form";
    }

    @PostMapping("/activities/{id}/edit")
    public String updateActivity(@PathVariable("id") Long id, @ModelAttribute("form") ActivityDTO form,
                                 @RequestParam(value = "photos", required = false) List<MultipartFile> files,
                                 Model model, Principal principal, RedirectAttributes redirect)
            throws EntityNotFoundException {
        try {
            List<ActivityPhoto> photos = photoService.check(files);
            Activity activity = activityService.updateActivity(id, form, principal.getName());
            photoService.replacePhotos(activity, photos);
            redirect.addFlashAttribute("message", "Cambios guardados.");
            return "redirect:/provider/activities/" + id;
        } catch (InvalidDataException | EntityNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("activityId", id);
            addActivityFormData(model, providerService.getByEmail(principal.getName()));
            return "provider/activity-form";
        }
    }

    // ---------- Fechas y horarios ----------

    @PostMapping("/activities/{id}/instances")
    public String addInstance(@PathVariable("id") Long id,
                              @RequestParam(value = "start", required = false)
                              @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime start,
                              @RequestParam(value = "maxCapacity", required = false) Integer maxCapacity,
                              Principal principal, RedirectAttributes redirect) {
        try {
            activityService.addInstance(id, start, maxCapacity, principal.getName());
            redirect.addFlashAttribute("message", "Fecha agregada.");
        } catch (InvalidDataException | EntityNotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/provider/activities/" + id;
    }

    @PostMapping("/instances/{id}/cancel")
    public String cancelInstance(@PathVariable("id") Long instanceId,
                                 @RequestParam("activityId") Long activityId,
                                 @RequestParam(value = "reasonId", required = false) Long reasonId,
                                 Principal principal, RedirectAttributes redirect) {
        try {
            int cancelled = bookingService.cancelInstance(instanceId, principal.getName(), reasonId);
            redirect.addFlashAttribute("message", "Fecha cancelada. Se cancelaron " + cancelled + " reservas.");
        } catch (InvalidDataException | EntityNotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/provider/activities/" + activityId;
    }

    // ---------- Reservas ----------

    // Reservas de todas las actividades del prestador, con las observaciones del turista (Acta 1, punto 4.4)
    @GetMapping("/bookings")
    public String bookings(Model model, Principal principal) {
        model.addAttribute("bookings", bookingService.getBookingsOfProvider(principal.getName()));
        return "provider/bookings";
    }

    // Registrar asistencia con el numero de reserva (el que esta en el QR)
    @PostMapping("/bookings/check-in")
    public String checkIn(@RequestParam("code") String code, Principal principal, RedirectAttributes redirect) {
        try {
            Booking booking = bookingService.checkIn(code, principal.getName());
            redirect.addFlashAttribute("message", "Asistencia registrada: " + booking.getTourist().getFullName()
                    + " (" + booking.getParticipants() + " personas) en " + booking.getActivity().getName() + ".");
        } catch (InvalidDataException | EntityNotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/provider/bookings";
    }

    // Si el service tira EntityNotFoundException (por ejemplo, una actividad de otro prestador),
    // se muestra la pagina de error con el mensaje
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(EntityNotFoundException e, Model model) {
        model.addAttribute("error", e.getMessage());
        return "error/not-found";
    }

    private List<Business> getApprovedBusinesses(Provider provider) {
        return provider.getBusinesses().stream()
                .filter(b -> b.getStatus() == BusinessStatus.APPROVED)
                .toList();
    }

    // Datos para los desplegables y casillas del formulario de actividad
    private void addActivityFormData(Model model, Provider provider) {
        model.addAttribute("businesses", getApprovedBusinesses(provider));
        model.addAttribute("localitiesByDepartment", locationService.getLocalitiesByDepartment());
        model.addAttribute("types", typeService.getAll());
        model.addAttribute("equipmentList", equipmentService.getAll());
    }
}

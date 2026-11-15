package uy.edu.um.proyectotic2026.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyectotic2026.dto.ActivityDTO;
import uy.edu.um.proyectotic2026.entities.*;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

// Lo que hace el prestador con sus actividades y sus fechas
@Service
public class ActivityService {

    @Autowired
    private ActivityRepository activityRepo;

    @Autowired
    private ActivityInstanceRepository instanceRepo;

    @Autowired
    private BusinessRepository businessRepo;

    @Autowired
    private LocalityRepository localityRepo;

    @Autowired
    private ActivityTypeRepository typeRepo;

    @Autowired
    private EquipmentRepository equipmentRepo;

    public List<Activity> getActivitiesOfProvider(String providerEmail) {
        return activityRepo.findByBusinessProviderEmailOrderByNameAsc(providerEmail);
    }

    // Una actividad del prestador logueado. Si es de otro prestador, es como si no existiera.
    public Activity getActivityOfProvider(Long activityId, String providerEmail) throws EntityNotFoundException {
        Activity activity = activityRepo.findById(activityId)
                .orElseThrow(() -> new EntityNotFoundException("Actividad no encontrada"));
        if (!activity.getBusiness().getProvider().getEmail().equals(providerEmail))
            throw new EntityNotFoundException("Actividad no encontrada");
        return activity;
    }

    @Transactional
    public Activity createActivity(ActivityDTO dto, String providerEmail)
            throws InvalidDataException, EntityNotFoundException {

        validate(dto);
        Business business = getApprovedBusiness(dto.getBusinessId(), providerEmail);

        Activity activity = Activity.builder().business(business).build();
        copyData(dto, activity);
        return activityRepo.save(activity);
    }

    @Transactional
    public Activity updateActivity(Long activityId, ActivityDTO dto, String providerEmail)
            throws InvalidDataException, EntityNotFoundException {

        validate(dto);
        Activity activity = getActivityOfProvider(activityId, providerEmail);
        activity.setBusiness(getApprovedBusiness(dto.getBusinessId(), providerEmail));
        copyData(dto, activity);
        return activityRepo.save(activity);
    }

    @Transactional
    public ActivityInstance addInstance(Long activityId, LocalDateTime start, Integer maxCapacity, String providerEmail)
            throws InvalidDataException, EntityNotFoundException {

        Activity activity = getActivityOfProvider(activityId, providerEmail);

        if (start == null)
            throw new InvalidDataException("Indicá la fecha y hora de inicio");
        if (!start.isAfter(LocalDateTime.now()))
            throw new InvalidDataException("La fecha tiene que ser futura");
        if (maxCapacity != null && maxCapacity < 1)
            throw new InvalidDataException("El cupo tiene que ser de al menos 1 persona");

        ActivityInstance instance = ActivityInstance.builder()
                .startDateTime(start)
                .maxCapacity(maxCapacity)
                .build();
        activity.addInstance(instance);
        return instanceRepo.save(instance);
    }

    // Solo el emprendimiento propio y aprobado puede publicar actividades (Acta 1, punto 4.2)
    private Business getApprovedBusiness(Long businessId, String providerEmail)
            throws EntityNotFoundException, InvalidDataException {

        if (businessId == null)
            throw new InvalidDataException("Elegí el emprendimiento");

        Business business = businessRepo.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException("Emprendimiento no encontrado"));

        if (!business.getProvider().getEmail().equals(providerEmail))
            throw new EntityNotFoundException("Emprendimiento no encontrado");

        if (business.getStatus() != BusinessStatus.APPROVED)
            throw new InvalidDataException("El emprendimiento todavía no fue aprobado por Uruguay Mnatural");

        return business;
    }

    // Pasa los datos del formulario a la entidad
    private void copyData(ActivityDTO dto, Activity activity) throws EntityNotFoundException {

        Locality locality = localityRepo.findById(dto.getLocalityId())
                .orElseThrow(() -> new EntityNotFoundException("Localidad no encontrada"));

        activity.setName(dto.getName().trim());
        activity.setDescription(dto.getDescription());
        activity.setLocality(locality);
        activity.setDurationMinutes(dto.getDurationMinutes());
        activity.setPrice(dto.getPrice());
        activity.setMinimumAge(dto.getMinimumAge());
        activity.setBookingLimitMinutes(dto.getBookingLimitMinutes() == null ? 0 : dto.getBookingLimitMinutes());
        activity.setMaxParticipantsPerBooking(dto.getMaxParticipantsPerBooking());
        activity.setAllowsCancellation(dto.isAllowsCancellation());
        activity.setCancellationMinHours(dto.isAllowsCancellation() ? dto.getCancellationMinHours() : null);
        activity.setRefundPercentage(dto.isAllowsCancellation() ? dto.getRefundPercentage() : null);
        activity.setPublished(dto.isPublished());

        activity.setTypes(new HashSet<>(typeRepo.findAllById(dto.getTypeIds())));

        // El equipamiento se vuelve a cargar entero (orphanRemoval borra el que se saco)
        activity.getEquipment().clear();
        for (Equipment item : equipmentRepo.findAllById(dto.getRequiredEquipmentIds()))
            activity.addEquipment(item, false);
        for (Equipment item : equipmentRepo.findAllById(dto.getProvidedEquipmentIds()))
            activity.addEquipment(item, true);
    }

    private void validate(ActivityDTO dto) throws InvalidDataException {

        if (isBlank(dto.getName()) || isBlank(dto.getDescription()) || dto.getLocalityId() == null
                || dto.getDurationMinutes() == null || dto.getPrice() == null)
            throw new InvalidDataException("Faltan datos obligatorios de la actividad");

        if (dto.getDurationMinutes() <= 0)
            throw new InvalidDataException("La duración tiene que ser mayor a cero");

        if (dto.getPrice().compareTo(BigDecimal.ZERO) < 0)
            throw new InvalidDataException("El precio no puede ser negativo");

        if (dto.getMinimumAge() != null && dto.getMinimumAge() < 0)
            throw new InvalidDataException("La edad mínima no puede ser negativa");

        if (dto.getBookingLimitMinutes() != null && dto.getBookingLimitMinutes() < 0)
            throw new InvalidDataException("Los minutos límite para reservar no pueden ser negativos");

        if (dto.getMaxParticipantsPerBooking() != null && dto.getMaxParticipantsPerBooking() < 1)
            throw new InvalidDataException("El máximo de personas por reserva tiene que ser de al menos 1");

        if (dto.getTypeIds() == null || dto.getTypeIds().isEmpty())
            throw new InvalidDataException("Elegí al menos un tipo de actividad");

        if (dto.isAllowsCancellation() && (dto.getCancellationMinHours() == null || dto.getCancellationMinHours() < 0))
            throw new InvalidDataException("Indicá con cuántas horas de anticipación se puede cancelar");

        // El prestador decide de antemano cuanto se devuelve si cancela el turista
        if (dto.isAllowsCancellation() && (dto.getRefundPercentage() == null
                || dto.getRefundPercentage() < 0 || dto.getRefundPercentage() > 100))
            throw new InvalidDataException("Indicá qué porcentaje se le devuelve al turista si cancela (de 0 a 100)");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

package uy.edu.um.proyectotic2026.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uy.edu.um.proyectotic2026.entities.Activity;
import uy.edu.um.proyectotic2026.entities.ActivityEquipment;
import uy.edu.um.proyectotic2026.entities.ActivityType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// Datos del formulario para crear o editar una actividad
@Getter
@Setter
@NoArgsConstructor

public class ActivityDTO {

    private Long businessId;
    private String name;
    private String description;
    private Long localityId;
    private Integer durationMinutes;
    private BigDecimal price;
    private Integer minimumAge;
    private Integer bookingLimitMinutes = 0;
    private Integer maxParticipantsPerBooking;
    private boolean allowsCancellation;
    private Integer cancellationMinHours;
    private Integer refundPercentage;
    private boolean published = true;

    private List<Long> typeIds = new ArrayList<>();

    // Equipamiento que tiene que traer el turista y equipamiento que da el prestador
    private List<Long> requiredEquipmentIds = new ArrayList<>();
    private List<Long> providedEquipmentIds = new ArrayList<>();

    // Para editar: carga el formulario con los datos actuales de la actividad
    public static ActivityDTO from(Activity activity) {
        ActivityDTO dto = new ActivityDTO();
        dto.setBusinessId(activity.getBusiness().getId());
        dto.setName(activity.getName());
        dto.setDescription(activity.getDescription());
        dto.setLocalityId(activity.getLocality().getId());
        dto.setDurationMinutes(activity.getDurationMinutes());
        dto.setPrice(activity.getPrice());
        dto.setMinimumAge(activity.getMinimumAge());
        dto.setBookingLimitMinutes(activity.getBookingLimitMinutes());
        dto.setMaxParticipantsPerBooking(activity.getMaxParticipantsPerBooking());
        dto.setAllowsCancellation(activity.isAllowsCancellation());
        dto.setCancellationMinHours(activity.getCancellationMinHours());
        dto.setRefundPercentage(activity.getRefundPercentage());
        dto.setPublished(activity.isPublished());

        for (ActivityType type : activity.getTypes())
            dto.getTypeIds().add(type.getId());

        for (ActivityEquipment item : activity.getEquipment()) {
            if (item.isProvidedByProvider())
                dto.getProvidedEquipmentIds().add(item.getEquipment().getId());
            else
                dto.getRequiredEquipmentIds().add(item.getEquipment().getId());
        }
        return dto;
    }
}

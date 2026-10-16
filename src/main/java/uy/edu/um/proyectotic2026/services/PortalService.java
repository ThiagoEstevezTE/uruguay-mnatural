package uy.edu.um.proyectotic2026.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uy.edu.um.proyectotic2026.dto.ActivitySearchDTO;
import uy.edu.um.proyectotic2026.entities.*;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.repository.ActivityRepository;

import java.util.List;

// Lo que ve cualquier visitante, sin necesidad de registrarse
@Service
public class PortalService {

    @Autowired
    private ActivityRepository activityRepo;

    // Busca entre las actividades visibles que tienen alguna fecha proxima.
    // Cada filtro se aplica solo si el visitante lo completo.
    public List<Activity> searchActivities(ActivitySearchDTO search) {
        return getVisibleActivities().stream()
                .filter(a -> !a.getUpcomingInstances().isEmpty())
                .filter(a -> matchesKeyword(a, search.getKeyword()))
                .filter(a -> search.getDepartmentId() == null
                        || a.getLocality().getDepartment().getId().equals(search.getDepartmentId()))
                .filter(a -> search.getTypeId() == null
                        || a.getTypes().stream().anyMatch(t -> t.getId().equals(search.getTypeId())))
                .filter(a -> search.getMaxPrice() == null || a.getPrice().compareTo(search.getMaxPrice()) <= 0)
                .filter(a -> search.getDate() == null || a.getUpcomingInstances().stream()
                        .anyMatch(i -> i.getStartDateTime().toLocalDate().equals(search.getDate())))
                .toList();
    }

    // Las proximas actividades para la portada, ordenadas por la fecha mas cercana
    public List<Activity> getFeaturedActivities(int amount) {
        return getVisibleActivities().stream()
                .filter(a -> !a.getUpcomingInstances().isEmpty())
                .sorted((a, b) -> a.getUpcomingInstances().get(0).getStartDateTime()
                        .compareTo(b.getUpcomingInstances().get(0).getStartDateTime()))
                .limit(amount)
                .toList();
    }

    // Detalle publico: si la actividad esta oculta o su emprendimiento no esta aprobado, no existe
    public Activity getPublicActivity(Long id) throws EntityNotFoundException {
        Activity activity = activityRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Actividad no encontrada"));
        if (!activity.isPublished() || activity.getBusiness().getStatus() != BusinessStatus.APPROVED)
            throw new EntityNotFoundException("Actividad no encontrada");
        return activity;
    }

    private List<Activity> getVisibleActivities() {
        return activityRepo.findByPublishedTrueAndBusinessStatusOrderByNameAsc(BusinessStatus.APPROVED);
    }

    private boolean matchesKeyword(Activity activity, String keyword) {
        if (keyword == null || keyword.trim().isEmpty())
            return true;
        String text = (activity.getName() + " " + activity.getDescription() + " "
                + activity.getLocality().getName() + " " + activity.getBusiness().getName()).toLowerCase();
        return text.contains(keyword.trim().toLowerCase());
    }
}

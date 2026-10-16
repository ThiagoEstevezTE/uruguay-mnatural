package uy.edu.um.proyectotic2026.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uy.edu.um.proyectotic2026.dto.ActivitySearchDTO;
import uy.edu.um.proyectotic2026.entities.*;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.repository.ActivityRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

// Lo que ve cualquier visitante, sin necesidad de registrarse
@Service
public class PortalService {

    @Autowired
    private ActivityRepository activityRepo;

    @Autowired
    private RatingService ratingService;

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

    // ---------- Para la API publica ----------

    // Actividades con alguna fecha programada en ese dia, en un departamento o localidad (opcionales)
    public List<Activity> getActivitiesOnDate(LocalDate date, Long departmentId, Long localityId) {
        return getVisibleActivities().stream()
                .filter(a -> departmentId == null || a.getLocality().getDepartment().getId().equals(departmentId))
                .filter(a -> localityId == null || a.getLocality().getId().equals(localityId))
                .filter(a -> a.getInstances().stream().anyMatch(i -> i.getStatus() == InstanceStatus.SCHEDULED
                        && i.getStartDateTime().toLocalDate().equals(date)))
                .toList();
    }

    // Las 3 actividades mejor puntuadas de un departamento, con las valoraciones hechas en ese mes
    public List<Activity> getTopRated(Long departmentId, YearMonth month) {
        return getVisibleActivities().stream()
                .filter(a -> a.getLocality().getDepartment().getId().equals(departmentId))
                .filter(a -> getMonthAverage(a, month) != null)
                .sorted(Comparator.comparing((Activity a) -> getMonthAverage(a, month)).reversed())
                .limit(3)
                .toList();
    }

    public Double getMonthAverage(Activity activity, YearMonth month) {
        List<Rating> ratings = ratingService.getRatingsOfActivity(activity.getId()).stream()
                .filter(r -> YearMonth.from(r.getDate()).equals(month))
                .toList();
        return ratingService.getAverage(ratings);
    }

    public Double getAverage(Activity activity) {
        return ratingService.getAverage(ratingService.getRatingsOfActivity(activity.getId()));
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

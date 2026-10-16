package uy.edu.um.proyectotic2026.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import uy.edu.um.proyectotic2026.dto.ActivitySearchDTO;
import uy.edu.um.proyectotic2026.entities.Activity;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.services.ActivityTypeService;
import uy.edu.um.proyectotic2026.services.LocationService;
import uy.edu.um.proyectotic2026.services.PortalService;

// Portal publico para turistas: portada, buscador y detalle de cada actividad
@Controller
public class IndexController {

    @Autowired
    private PortalService portalService;

    @Autowired
    private LocationService locationService;

    @Autowired
    private ActivityTypeService typeService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("search", new ActivitySearchDTO());
        model.addAttribute("featured", portalService.getFeaturedActivities(6));
        addSearchFormData(model);
        return "index";
    }

    // Los filtros llegan en la URL: /activities?departmentId=14&typeId=3...
    @GetMapping("/activities")
    public String activities(@ModelAttribute("search") ActivitySearchDTO search, Model model) {
        model.addAttribute("activities", portalService.searchActivities(search));
        addSearchFormData(model);
        return "activities";
    }

    @GetMapping("/activities/{id}")
    public String activityDetail(@PathVariable("id") Long id, Model model) throws EntityNotFoundException {
        Activity activity = portalService.getPublicActivity(id);
        model.addAttribute("activity", activity);
        model.addAttribute("instances", activity.getUpcomingInstances());
        return "activity";
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(EntityNotFoundException e, Model model) {
        model.addAttribute("error", e.getMessage());
        return "error/not-found";
    }

    private void addSearchFormData(Model model) {
        model.addAttribute("departments", locationService.getDepartments());
        model.addAttribute("types", typeService.getAll());
    }
}

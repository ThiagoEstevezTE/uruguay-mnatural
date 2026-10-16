package uy.edu.um.proyectotic2026.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uy.edu.um.proyectotic2026.entities.Business;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.services.ActivityTypeService;
import uy.edu.um.proyectotic2026.services.BusinessService;
import uy.edu.um.proyectotic2026.services.EquipmentService;

import java.security.Principal;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private BusinessService businessService;

    @Autowired
    private ActivityTypeService typeService;

    @Autowired
    private EquipmentService equipmentService;

    // Inicio del administrador: la bandeja de solicitudes pendientes
    @GetMapping
    public String home(Model model) {
        model.addAttribute("pending", businessService.getPendingBusinesses());
        return "admin/home";
    }

    @GetMapping("/businesses")
    public String businesses(Model model) {
        model.addAttribute("businesses", businessService.getAllBusinesses());
        return "admin/businesses";
    }

    @PostMapping("/businesses/{id}/approve")
    public String approve(@PathVariable("id") Long id, Principal principal, RedirectAttributes redirect) {
        try {
            Business business = businessService.approveBusiness(id, principal.getName());
            redirect.addFlashAttribute("message", "Aprobaste " + business.getName() + ". Ya puede publicar actividades.");
        } catch (EntityNotFoundException | InvalidDataException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/businesses/{id}/reject")
    public String reject(@PathVariable("id") Long id, @RequestParam("reason") String reason,
                         Principal principal, RedirectAttributes redirect) {
        try {
            Business business = businessService.rejectBusiness(id, principal.getName(), reason);
            redirect.addFlashAttribute("message", "Rechazaste la solicitud de " + business.getName() + ".");
        } catch (EntityNotFoundException | InvalidDataException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }

    // ---------- Tipos de actividad ----------

    @GetMapping("/activity-types")
    public String activityTypes(Model model) {
        model.addAttribute("items", typeService.getAll());
        addCatalogInfo(model, "activity-types", "Tipos de actividad", "Ej.: Senderismo");
        return "admin/catalog";
    }

    @PostMapping("/activity-types")
    public String createActivityType(@RequestParam("name") String name, RedirectAttributes redirect) {
        try {
            typeService.create(name);
            redirect.addFlashAttribute("message", "Tipo de actividad agregado.");
        } catch (InvalidDataException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/activity-types";
    }

    @PostMapping("/activity-types/{id}")
    public String updateActivityType(@PathVariable("id") Long id, @RequestParam("name") String name,
                                     RedirectAttributes redirect) {
        try {
            typeService.update(id, name);
            redirect.addFlashAttribute("message", "Tipo de actividad modificado.");
        } catch (InvalidDataException | EntityNotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/activity-types";
    }

    @PostMapping("/activity-types/{id}/delete")
    public String deleteActivityType(@PathVariable("id") Long id, RedirectAttributes redirect) {
        try {
            typeService.delete(id);
            redirect.addFlashAttribute("message", "Tipo de actividad eliminado.");
        } catch (InvalidDataException | EntityNotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/activity-types";
    }

    // ---------- Equipamiento ----------

    @GetMapping("/equipment")
    public String equipment(Model model) {
        model.addAttribute("items", equipmentService.getAll());
        addCatalogInfo(model, "equipment", "Equipamiento", "Ej.: Chaleco salvavidas");
        return "admin/catalog";
    }

    @PostMapping("/equipment")
    public String createEquipment(@RequestParam("name") String name, RedirectAttributes redirect) {
        try {
            equipmentService.create(name);
            redirect.addFlashAttribute("message", "Equipamiento agregado.");
        } catch (InvalidDataException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/equipment";
    }

    @PostMapping("/equipment/{id}")
    public String updateEquipment(@PathVariable("id") Long id, @RequestParam("name") String name,
                                  RedirectAttributes redirect) {
        try {
            equipmentService.update(id, name);
            redirect.addFlashAttribute("message", "Equipamiento modificado.");
        } catch (InvalidDataException | EntityNotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/equipment";
    }

    @PostMapping("/equipment/{id}/delete")
    public String deleteEquipment(@PathVariable("id") Long id, RedirectAttributes redirect) {
        try {
            equipmentService.delete(id);
            redirect.addFlashAttribute("message", "Equipamiento eliminado.");
        } catch (InvalidDataException | EntityNotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/equipment";
    }

    // Las dos pantallas de ABM usan la misma plantilla (admin/catalog.html)
    private void addCatalogInfo(Model model, String path, String title, String placeholder) {
        model.addAttribute("path", path);
        model.addAttribute("title", title);
        model.addAttribute("placeholder", placeholder);
    }
}

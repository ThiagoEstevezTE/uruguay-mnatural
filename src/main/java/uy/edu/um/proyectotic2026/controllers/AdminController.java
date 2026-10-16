package uy.edu.um.proyectotic2026.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uy.edu.um.proyectotic2026.entities.Business;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.services.BusinessService;

import java.security.Principal;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private BusinessService businessService;

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
}

package uy.edu.um.proyectotic2026.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uy.edu.um.proyectotic2026.dto.ProviderRegistrationDTO;
import uy.edu.um.proyectotic2026.dto.TouristRegistrationDTO;
import uy.edu.um.proyectotic2026.entities.ProviderType;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.services.LocationService;
import uy.edu.um.proyectotic2026.services.ProviderService;
import uy.edu.um.proyectotic2026.services.TouristService;

// Formularios de registro. Usan los mismos services que la API REST.
@Controller
@RequestMapping("/register")
public class RegisterController {

    @Autowired
    private TouristService touristService;

    @Autowired
    private ProviderService providerService;

    @Autowired
    private LocationService locationService;

    @GetMapping("/tourist")
    public String touristForm(Model model) {
        model.addAttribute("form", new TouristRegistrationDTO());
        return "register/tourist";
    }

    @PostMapping("/tourist")
    public String registerTourist(@ModelAttribute("form") TouristRegistrationDTO form, Model model,
                                  RedirectAttributes redirect) {
        try {
            touristService.registerTourist(form);
            redirect.addFlashAttribute("message", "¡Listo! Ya podés ingresar con tu email y contraseña.");
            return "redirect:/login";
        } catch (InvalidDataException e) {
            // Se vuelve a mostrar el formulario con lo que habia escrito y el error
            model.addAttribute("error", e.getMessage());
            return "register/tourist";
        }
    }

    @GetMapping("/provider")
    public String providerForm(Model model) {
        model.addAttribute("form", new ProviderRegistrationDTO());
        addProviderFormData(model);
        return "register/provider";
    }

    @PostMapping("/provider")
    public String registerProvider(@ModelAttribute("form") ProviderRegistrationDTO form, Model model,
                                   RedirectAttributes redirect) {
        try {
            providerService.registerProvider(form);
            redirect.addFlashAttribute("message",
                    "Recibimos tu solicitud. Un administrador la va a revisar; mientras tanto ya podés ingresar.");
            return "redirect:/login";
        } catch (InvalidDataException | EntityNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            addProviderFormData(model);
            return "register/provider";
        }
    }

    // Datos para los desplegables del formulario de prestador
    private void addProviderFormData(Model model) {
        model.addAttribute("localitiesByDepartment", locationService.getLocalitiesByDepartment());
        model.addAttribute("providerTypes", ProviderType.values());
    }
}

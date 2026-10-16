package uy.edu.um.proyectotic2026.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // Despues de iniciar sesion, cada rol va a su propio espacio
    @GetMapping("/panel")
    public String panel(Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        if (role.equals("ROLE_ADMIN"))
            return "redirect:/admin";
        if (role.equals("ROLE_PROVIDER"))
            return "redirect:/provider";
        return "redirect:/";
    }
}

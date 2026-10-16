package uy.edu.um.proyectotic2026.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/provider")
public class ProviderWebController {

    @GetMapping
    public String home() {
        return "provider/home";
    }
}

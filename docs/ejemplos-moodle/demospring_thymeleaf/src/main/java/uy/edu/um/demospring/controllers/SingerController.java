package uy.edu.um.demospring.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import uy.edu.um.demospring.entities.Singer;
import uy.edu.um.demospring.services.SingerService;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/singer")
public class SingerController {

    @Autowired
    private SingerService singerService;
    @GetMapping("/")
    public String SingerMain()
    {
        return "singermain";
    }

    @GetMapping("/singerall")
    public String singerAll(Model model)
    {
        List<Singer> todosLosCantantes = singerService.todosLosCantantes();
        if(todosLosCantantes.isEmpty())
            model.addAttribute("mensaje", "No hay elementos");
        model.addAttribute("singers",todosLosCantantes);
        return "singerall";
    }

    @GetMapping("/singerdetail/{id}")
    public String singerDetil(Model model, @PathVariable("id") Long id){

        Optional<Singer> result = singerService.findById(id);
        model.addAttribute("mensaje", null);
        if(result.isEmpty())
            model.addAttribute("mensaje", "Cantante no encontrado");
        else
            model.addAttribute("singer", result.get());
        return "singerdetail";
    }

}

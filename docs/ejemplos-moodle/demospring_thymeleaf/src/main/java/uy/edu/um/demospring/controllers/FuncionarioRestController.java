package uy.edu.um.demospring.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.edu.um.demospring.entities.Funcionario;
import uy.edu.um.demospring.services.FuncionarioService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/funcionario")
public class FuncionarioRestController {

    @Autowired
    private FuncionarioService funcService;
    @GetMapping("/")
    public ResponseEntity<List<Funcionario>> getAll()
    {
        return ResponseEntity.ok(funcService.getAll());
    }

    @PostMapping("/new")
    public ResponseEntity<Funcionario> addFuncionario(@RequestParam int nro, @RequestParam String name)
    {
        Funcionario funcNuevo = funcService.addFuncionario(nro, name);
        return ResponseEntity.ok(funcNuevo);
    }

    @PostMapping("/new2")
    public ResponseEntity<Funcionario> addFuncionario(@RequestBody Funcionario funcionario)
    {
        Funcionario funcNuevo = funcService.addFuncionario(funcionario.getNroFunc(), funcionario.getName());
        return ResponseEntity.ok(funcNuevo);
    }



}

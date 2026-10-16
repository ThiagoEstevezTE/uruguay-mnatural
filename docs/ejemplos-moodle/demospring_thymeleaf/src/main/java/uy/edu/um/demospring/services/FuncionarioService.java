package uy.edu.um.demospring.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uy.edu.um.demospring.entities.Funcionario;
import uy.edu.um.demospring.repository.FuncionarioRepository;

import java.util.List;
import java.util.Optional;

@Service
public class FuncionarioService {

    @Autowired
    private FuncionarioRepository funcRepo;

    public Funcionario getByNro(int nroFunc){
        Optional<Funcionario> result = funcRepo.findByNroFunc(nroFunc);
        if(result.isPresent())
            return result.get();
        return null;
    }

    public List<Funcionario> getAll()
    {
        return funcRepo.findAll();
    }

    public Funcionario addFuncionario(int nroFunc, String nombre)
    {
        //hacer controles de datos
        Funcionario unNuevoFuncionario = Funcionario.builder()
                .nroFunc(nroFunc)
                .name(nombre)
                .build();
        return funcRepo.save(unNuevoFuncionario);
    }

}

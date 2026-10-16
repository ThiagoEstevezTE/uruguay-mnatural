package uy.edu.um.demospring;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Component;
import uy.edu.um.demospring.entities.Album;
import uy.edu.um.demospring.entities.Funcionario;
import uy.edu.um.demospring.entities.Singer;
import uy.edu.um.demospring.exceptions.InvalidDataException;
import uy.edu.um.demospring.repository.SingerRepository;
import uy.edu.um.demospring.services.AlbumService;
import uy.edu.um.demospring.services.FuncionarioService;
import uy.edu.um.demospring.services.SingerService;

@SpringBootApplication
@Component
@EnableJpaRepositories(basePackages = "uy.edu.um.demospring.repository")
@EntityScan(basePackages = "uy.edu.um.demospring.repository")
public class DemospringApplication
{

    @Autowired
    private FuncionarioService funcService;

    public static void main(String[] args)
    {
        ApplicationContext ctx = SpringApplication.run(DemospringApplication.class, args);
        DemospringApplication app = ctx.getBean(DemospringApplication.class);
        app.runInCommandLine();
    }

    public void runInCommandLine()
    {
        Funcionario funcionario = funcService.addFuncionario(100, "Nicolas");
        System.out.println(funcionario);

    }

}

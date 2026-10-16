/*
 * NO LICENCE 
 * Author: Ing. Nicolás Navarro Gutérrez
 */
package uy.edu.um.demospring.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 *
 * @author Nicolás Navarro Gutiérrez
 */
@Controller
public class IndexController
{
    @GetMapping
    public String index()
    {
        return "index";
    }
}

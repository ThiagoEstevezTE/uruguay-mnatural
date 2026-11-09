package uy.edu.um.proyectotic2026.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uy.edu.um.proyectotic2026.entities.Department;
import uy.edu.um.proyectotic2026.entities.Locality;
import uy.edu.um.proyectotic2026.repository.DepartmentRepository;
import uy.edu.um.proyectotic2026.repository.LocalityRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LocationService {

    @Autowired
    private DepartmentRepository departmentRepo;

    @Autowired
    private LocalityRepository localityRepo;

    public List<Department> getDepartments() {
        return departmentRepo.findAllByOrderByNameAsc();
    }

    // Departamento -> sus localidades, ordenado alfabeticamente. Se usa en los desplegables.
    public Map<String, List<Locality>> getLocalitiesByDepartment() {
        Map<String, List<Locality>> result = new LinkedHashMap<>();
        for (Locality locality : localityRepo.findAllWithDepartment())
            result.computeIfAbsent(locality.getDepartment().getName(), name -> new ArrayList<>()).add(locality);
        return result;
    }
}

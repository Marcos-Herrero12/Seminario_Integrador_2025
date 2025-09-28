package com.seminario.pasantias.service;

import com.seminario.pasantias.entity.Carrera;
import com.seminario.pasantias.persistence.CarreraMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Optional;

@Service
public class CarreraService {
    private final CarreraMapper carreraMapper;

    @Autowired
    public CarreraService(CarreraMapper carreraMapper) {
        this.carreraMapper = carreraMapper;
    }

    public List<Carrera> getAllCarreras() {
        return carreraMapper.findAll();
    }

    public Optional<Carrera> getCarreraByNombre(String nombre) {
        return carreraMapper.findByNombre(nombre);
    }

    public void createCarrera(Carrera carrera) {
        carreraMapper.insert(carrera);
    }

    public void updateCarrera(Carrera carrera) {
        carreraMapper.update(carrera);
    }

    public void deleteCarrera(Integer id) {
        carreraMapper.delete(id);
    }

    public void deleteCarreraByNombre(String nombre) {
        carreraMapper.deleteByNombre(nombre);
    }
}
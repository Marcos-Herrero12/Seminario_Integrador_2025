package com.seminario.pasantias;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Empresa {
    private Integer idEmpresa;
    private String nombre;
    private String ciudad;
    private String direccion;
    private String emailContacto;
    private String modalidad;
}
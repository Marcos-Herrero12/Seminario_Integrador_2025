package com.seminario.pasantias.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class Empresa {
    private Integer idEmpresa;
    private String nombre;
    private String ciudad;
    private String calle;
    private Integer nroCalle;
    private String piso;
    private String departamento;
    private String barrio;
    private String email;
    private String cuit;
    private String razonSocial;
    private Integer idUsuario;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private List<Contacto> contacto;
    
    // Constructor para compatibilidad con código existente (sin nuevos campos)
    public Empresa(Integer idEmpresa, String nombre, String ciudad, String calle, 
                   Integer nroCalle, String piso, String departamento, String barrio,
                   String email, String cuit, String razonSocial, List<Contacto> contacto, Integer idUsuario) {
        this.idEmpresa = idEmpresa;
        this.nombre = nombre;
        this.ciudad = ciudad;
        this.calle = calle;
        this.nroCalle = nroCalle;
        this.piso = piso;
        this.departamento = departamento;
        this.barrio = barrio;
        this.email = email;
        this.cuit = cuit;
        this.razonSocial = razonSocial;
        this.contacto = contacto;
        // Valores por defecto para nuevos campos
        this.activo = true;
        this.fechaCreacion = LocalDateTime.now();
        this.idUsuario = null;
    }
}


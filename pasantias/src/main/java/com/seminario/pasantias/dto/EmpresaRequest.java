package com.seminario.pasantias.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaRequest {
	private String nombre;
	private String ciudad;
	private String direccion;
	private String emailContacto;
	private String cuit;
	private String razonSocial;
}

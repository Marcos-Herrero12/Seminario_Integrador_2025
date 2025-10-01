package com.seminario.pasantias.controller;

import com.seminario.pasantias.entity.Empresa;
import com.seminario.pasantias.dto.EmpresaRequest;
import com.seminario.pasantias.service.EmpresaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/empresas")
public class EmpresaController {
	private final EmpresaService empresaService;

	@Autowired
	public EmpresaController(EmpresaService empresaService) {
		this.empresaService = empresaService;
	}

	@GetMapping
	public List<Empresa> getAllEmpresas() {
		return empresaService.getAllEmpresas();
	}

	@GetMapping("/{id}")
	public Empresa getEmpresaById(@PathVariable Integer id) {
		return empresaService.getEmpresaById(id);
	}

	@PostMapping
	public void createEmpresa(@RequestBody EmpresaRequest request) {
		Empresa empresa = new Empresa(null, request.getNombre(), request.getCiudad(), request.getDireccion(), request.getEmailContacto(), request.getCuit(), request.RazonSocial());
		empresaService.createEmpresa(empresa);
	}

	@PutMapping("/{id}")
	public void updateEmpresa(@PathVariable Integer id, @RequestBody EmpresaRequest request) {
		Empresa empresa = new Empresa(id, request.getNombre(), request.getCiudad(), request.getDireccion(), request.getEmailContacto(), request.getCuit(), request.RazonSocial());
		empresaService.updateEmpresa(empresa);
	}

	@DeleteMapping("/{id}")
	public void deleteEmpresa(@PathVariable Integer id) {
		empresaService.deleteEmpresa(id);
	}
}

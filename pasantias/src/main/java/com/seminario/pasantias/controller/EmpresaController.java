package com.seminario.pasantias.controller;

import com.seminario.pasantias.entity.Empresa;
import com.seminario.pasantias.dto.EmpresaRequest;
import com.seminario.pasantias.response.GenericResponse;
import com.seminario.pasantias.service.EmpresaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;
import java.util.Map;

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

	@GetMapping("/consultarEmpresas")
	public Object consultarEmpresas(@RequestParam(required = false) String cuit, 
									@RequestParam(required = false) String nombre) {
		// Si no se proporcionan parámetros, devolver todas las empresas
		if ((cuit == null || cuit.isEmpty()) && (nombre == null || nombre.isEmpty())) {
			return empresaService.getAllEmpresas();
		}
		
		// Si se proporciona CUIT, buscar por CUIT (más específico)
		if (cuit != null && !cuit.isEmpty()) {
			Optional<Empresa> empresa = empresaService.getEmpresaByCuit(cuit);
			if (empresa.isPresent()) {
				return List.of(empresa.get());
			} else {
				return Optional.empty();
			}
		}
		
		// Si se proporciona nombre, buscar por nombre
		if (nombre != null && !nombre.isEmpty()) {
			List<Empresa> empresas = empresaService.getEmpresasByNombre(nombre);
			if (!empresas.isEmpty()) {
				return empresas;
			} else {
				return Optional.empty();
			}
		}
		
		return Optional.empty();
	}

	@PostMapping("/crearEmpresa")
	public GenericResponse crearEmpresa(@RequestBody EmpresaRequest request) {
		GenericResponse response = new GenericResponse();
		
		// Validaciones de campos obligatorios
		if (request.getNombre() == null || request.getNombre().isEmpty()) {
			response.setCode(-1);
			response.setMessage("El nombre es obligatorio");
			return response;
		}
		if (request.getRazonSocial() == null || request.getRazonSocial().isEmpty()) {
			response.setCode(-1);
			response.setMessage("La razón social es obligatoria");
			return response;
		}
		if (request.getCuit() == null || request.getCuit().isEmpty()) {
			response.setCode(-1);
			response.setMessage("El CUIT es obligatorio");
			return response;
		}
		if (request.getCiudad() == null || request.getCiudad().isEmpty()) {
			response.setCode(-1);
			response.setMessage("La ciudad es obligatoria");
			return response;
		}
		if (request.getCalle() == null || request.getCalle().isEmpty()) {
			response.setCode(-1);
			response.setMessage("La calle es obligatoria");
			return response;
		}
		if (request.getContacto() == null || request.getContacto().isEmpty()) {
			response.setCode(-1);
			response.setMessage("La lista de contactos es obligatoria");
			return response;
		}
		
		// Validar cada contacto
		for (int i = 0; i < request.getContacto().size(); i++) {
			var contacto = request.getContacto().get(i);
			if (contacto.getNombre() == null || contacto.getNombre().isEmpty()) {
				response.setCode(-1);
				response.setMessage("El nombre del contacto " + (i + 1) + " es obligatorio");
				return response;
			}
			if (contacto.getApellido() == null || contacto.getApellido().isEmpty()) {
				response.setCode(-1);
				response.setMessage("El apellido del contacto " + (i + 1) + " es obligatorio");
				return response;
			}
			if (contacto.getEmailResponsable() == null || contacto.getEmailResponsable().isEmpty()) {
				response.setCode(-1);
				response.setMessage("El email del responsable del contacto " + (i + 1) + " es obligatorio");
				return response;
			}
		}
		
		try {
			Empresa empresa = new Empresa(null, request.getNombre(), request.getCiudad(), 
				request.getCalle(), request.getNroCalle(), request.getPiso(),
				request.getDepartamento(), request.getBarrio(), request.getEmail(), 
				request.getCuit(), request.getRazonSocial(), request.getContacto(),null);
			empresaService.createEmpresaWithContactos(empresa);
			response.setCode(0);
			response.setMessage(null);
		} catch (Exception e) {
			response.setCode(-1);
			response.setMessage(e.getMessage());
		}
		return response;
	}

	@PostMapping
	public void createEmpresa(@RequestBody EmpresaRequest request) {
		Empresa empresa = new Empresa(null, request.getNombre(), request.getCiudad(), 
			request.getCalle(), request.getNroCalle(), request.getPiso(), 
			request.getDepartamento(), request.getBarrio(), request.getEmail(),
			request.getCuit(), request.getRazonSocial(), request.getContacto(),null);
		empresaService.createEmpresa(empresa);
	}

	@PutMapping("/{id}")
	public void updateEmpresa(@PathVariable Integer id, @RequestBody EmpresaRequest request) {
		Empresa empresa = new Empresa(id, request.getNombre(), request.getCiudad(), 
			request.getCalle(), request.getNroCalle(), request.getPiso(), 
			request.getDepartamento(), request.getBarrio(), request.getEmail(), 
			request.getCuit(), request.getRazonSocial(), request.getContacto(), null);
		empresaService.updateEmpresa(empresa);
	}

	@PostMapping("/deleteEmpresa")
	public GenericResponse deleteEmpresa(@RequestBody Map<String, String> request) {
		GenericResponse response = new GenericResponse();
		
		// Validación de campo obligatorio
		String cuit = request.get("cuit");
		if (cuit == null || cuit.isEmpty()) {
			response.setCode(-1);
			response.setMessage("El CUIT es obligatorio");
			return response;
		}
		
		try {
			empresaService.deleteEmpresaByCuit(cuit);
			response.setCode(0);
			response.setMessage(null);
		} catch (Exception e) {
			response.setCode(-1);
			response.setMessage(e.getMessage());
		}
		return response;
	}

	@DeleteMapping("/{id}")
	public void deleteEmpresa(@PathVariable Integer id) {
		empresaService.deleteEmpresa(id);
	}
}

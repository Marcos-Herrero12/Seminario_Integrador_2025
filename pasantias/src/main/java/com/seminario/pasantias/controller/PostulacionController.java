package com.seminario.pasantias.controller;

import com.seminario.pasantias.dto.request.PostulacionRequestDTO;
import com.seminario.pasantias.dto.response.PostulacionResponseDTO;
import com.seminario.pasantias.service.PostulacionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/postulaciones")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"},
             methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
             allowedHeaders = "*")
public class PostulacionController {

    @Autowired
    private PostulacionService postulacionService;


    @PostMapping("/registrarPostulacion")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ESTUDIANTE')")
    public ResponseEntity<?> registrarPostulacion(@Valid @RequestBody PostulacionRequestDTO request) {
        try {
            System.out.println("========================================");
            System.out.println("POSTULACION - Iniciando registro de postulación");
            System.out.println("Estudiante ID: " + request.getIdEstudiante());
            System.out.println("Pasantia ID: " + request.getIdPasantia());
            System.out.println("Fecha Postulación: " + request.getFechaPostulacion());
            System.out.println("Estado: " + request.getEstado());
            System.out.println("========================================");
            
            PostulacionResponseDTO postulacion = postulacionService.crearPostulacion(request);

            System.out.println("========================================");
            System.out.println("POSTULACION - Registro exitoso");
            System.out.println("Postulación ID: " + postulacion.getIdPostulacion());
            System.out.println("Estudiante: " + postulacion.getNombreEstudiante() + " " + postulacion.getApellidoEstudiante());
            System.out.println("Pasantia: " + postulacion.getTituloPasantia());
            System.out.println("Estado: " + postulacion.getEstado());
            System.out.println("Fecha Postulación: " + postulacion.getFechaPostulacion());
            System.out.println("========================================");

            Map<String, Object> response = new HashMap<>();
            response.put("codigo", 0);
            response.put("mensaje", "Postulación registrada exitosamente");
            response.put("data", postulacion);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            System.out.println("========================================");
            System.out.println("POSTULACION - Error al registrar postulación");
            System.out.println("Estudiante ID: " + request.getIdEstudiante());
            System.out.println("Pasantia ID: " + request.getIdPasantia());
            System.out.println("Error: " + e.getMessage());
            System.out.println("========================================");
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("codigo", -1);
            errorResponse.put("mensaje", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/consultarPostulaciones")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ESTUDIANTE')")
    public ResponseEntity<?> consultarPostulaciones() {
        try {
            System.out.println(":::::: AGUSTIN ENTRA A CONSULTAR POSTULACION ::::::");
            List<PostulacionResponseDTO> postulaciones = postulacionService.consultarPostulaciones();

            Map<String, Object> response = new HashMap<>();
            response.put("codigo", 0);
            response.put("mensaje", "Postulaciones encontradas");
            response.put("data", postulaciones);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("codigo", -1);
            errorResponse.put("mensaje", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    @GetMapping("/misPostulaciones")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> misPostulaciones() {
        List<PostulacionResponseDTO> data = postulacionService.consultarMisPostulaciones();

        Map<String, Object> response = new HashMap<>();
        response.put("codigo", 0);
        response.put("data", data);
        response.put("mensaje", "Postulaciones encontradas");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/porPasantia/{pasantiaId}")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> obtenerPostulacionPorPasantia(@PathVariable Integer pasantiaId) {
        try {
            java.util.Optional<PostulacionResponseDTO> postulacionOpt = postulacionService.obtenerPostulacionPorPasantia(pasantiaId);

            Map<String, Object> response = new HashMap<>();
            if (postulacionOpt.isPresent()) {
                response.put("codigo", 0);
                response.put("mensaje", "Postulación encontrada");
                response.put("data", postulacionOpt.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("codigo", 0);
                response.put("mensaje", "No se encontró postulación para esta pasantía");
                response.put("data", null);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("codigo", -1);
            errorResponse.put("mensaje", e.getMessage());
            errorResponse.put("data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/pasantia/{pasantiaId}")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ESTUDIANTE', 'EMPRESA')")
    public ResponseEntity<Map<String, Object>> obtenerTodasPostulacionesPorPasantia(@PathVariable Integer pasantiaId) {
        try {
            List<PostulacionResponseDTO> postulaciones = postulacionService.obtenerTodasPostulacionesPorPasantia(pasantiaId);

            Map<String, Object> response = new HashMap<>();
            response.put("codigo", 0);
            response.put("mensaje", "Postulaciones encontradas: " + postulaciones.size());
            response.put("data", postulaciones);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("codigo", -1);
            errorResponse.put("mensaje", "Error al obtener postulaciones: " + e.getMessage());
            errorResponse.put("data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> obtenerPostulacionPorId(@PathVariable Integer id) {
        try {
            com.seminario.pasantias.dto.response.PostulacionDetalleDTO postulacion = postulacionService.obtenerPostulacionPorId(id);

            Map<String, Object> response = new HashMap<>();
            response.put("codigo", 0);
            response.put("mensaje", "Postulación encontrada");
            response.put("data", postulacion);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("codigo", -1);
            errorResponse.put("mensaje", e.getMessage());
            errorResponse.put("data", null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("codigo", -1);
            errorResponse.put("mensaje", "Error al obtener la postulación: " + e.getMessage());
            errorResponse.put("data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/postulacionesMiEmpresa")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('EMPRESA')")
    public ResponseEntity<Map<String, Object>> obtenerPostulacionesMiEmpresa() {
        try {
            List<PostulacionResponseDTO> postulaciones = postulacionService.obtenerPostulacionesMiEmpresa();

            Map<String, Object> response = new HashMap<>();
            response.put("codigo", 0);
            response.put("mensaje", "Postulaciones encontradas");
            response.put("data", postulaciones);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("codigo", -1);
            errorResponse.put("mensaje", "Error al obtener postulaciones: " + e.getMessage());
            errorResponse.put("data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}

package com.seminario.pasantias.controller;

import com.seminario.pasantias.dto.request.ActualizarEstadoPasantiaDTO;
import com.seminario.pasantias.dto.request.PasantiaRequestDTO;
import com.seminario.pasantias.dto.response.PasantiaDetalleDTO;
import com.seminario.pasantias.dto.response.PasantiaResponseDTO;
import com.seminario.pasantias.entity.EstadoPasantia;
import com.seminario.pasantias.security.SecurityService;
import com.seminario.pasantias.service.PasantiaService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestionar las operaciones relacionadas con Pasantías.
 * 
 * <p>Este controlador maneja las siguientes operaciones:
 * <ul>
 *   <li>Obtener todas las pasantías</li>
 *   <li>Obtener pasantías publicadas (endpoint público)</li>
 *   <li>Obtener detalles de una pasantía por ID</li>
 *   <li>Registrar una nueva pasantía</li>
 *   <li>Aprobar una pasantía (solo ADMINISTRADOR)</li>
 *   <li>Finalizar una pasantía (solo ADMINISTRADOR)</li>
 * </ul>
 * 
 * <p>Todas las respuestas incluyen el header Content-Type con charset UTF-8
 * para garantizar la correcta codificación de caracteres especiales.
 * 
 * @author Sistema de Pasantías
 * @version 1.0
 */
@RestController
@RequestMapping("/pasantias")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"},
             allowedHeaders = "*")
public class PasantiaController {

    /** Servicio que contiene la lógica de negocio para pasantías */
    @Autowired
    private PasantiaService pasantiaService;

    /** Servicio que maneja la seguridad y validación de permisos */
    @Autowired
    private SecurityService securityService;

    /**
     * Obtiene todas las pasantías del sistema.
     * 
     * <p>Este endpoint requiere autenticación y retorna todas las pasantías
     * independientemente de su estado. Los usuarios solo verán las pasantías
     * según sus permisos definidos en el servicio de seguridad.
     * 
     * @return ResponseEntity con lista de PasantiaResponseDTO y código HTTP 200 (OK)
     * @see PasantiaResponseDTO
     */
    @GetMapping(produces = "application/json;charset=UTF-8")
    public ResponseEntity<List<PasantiaResponseDTO>> getAllPasantias() {
        List<PasantiaResponseDTO> pasantias = pasantiaService.obtenerTodasLasPasantias();
        return ResponseEntity.ok()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body(pasantias);
    }

    /**
     * Obtiene todas las pasantías con estado PUBLICADA.
     * 
     * <p>Este es un endpoint público que no requiere autenticación.
     * Retorna únicamente las pasantías que han sido aprobadas y publicadas,
     * permitiendo que cualquier usuario pueda ver las pasantías disponibles.
     * 
     * @return ResponseEntity con lista de PasantiaResponseDTO (solo PUBLICADAS) 
     *         y código HTTP 200 (OK)
     * @see PasantiaResponseDTO
     * @see EstadoPasantia#PUBLICADA
     */
    @GetMapping("/publicadas")
    public ResponseEntity<List<PasantiaResponseDTO>> getPasantiasPublicadas() {
        List<PasantiaResponseDTO> pasantias = pasantiaService.obtenerPasantiasPublicadas();
        return ResponseEntity.ok()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body(pasantias);
    }

    /**
     * Obtiene los detalles completos de una pasantía por su ID.
     * 
     * <p>Este endpoint es público y no requiere autenticación. Retorna información
     * detallada de la pasantía incluyendo datos de la empresa asociada, carreras
     * relacionadas y todos los campos disponibles.
     * 
     * @param id ID único de la pasantía a consultar
     * @return ResponseEntity con PasantiaDetalleDTO y código HTTP 200 (OK) si existe,
     *         o código HTTP 404 (NOT_FOUND) si no se encuentra,
     *         o código HTTP 500 (INTERNAL_SERVER_ERROR) en caso de error
     * @see PasantiaDetalleDTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPasantiaById(@PathVariable Integer id) {
        try {
            // Obtener los detalles completos de la pasantía
            PasantiaDetalleDTO pasantia = pasantiaService.obtenerPasantiaPorId(id);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(pasantia);
        } catch (IllegalArgumentException e) {
            // La pasantía no existe
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("codigo", -1);
            errorResponse.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            // Error inesperado del servidor
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("codigo", -1);
            errorResponse.put("mensaje", "Error al obtener la pasantía: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Registra una nueva pasantía en el sistema.
     * 
     * <p>Este endpoint requiere autenticación y valida que el usuario tenga permisos
     * para crear pasantías para la empresa especificada. La pasantía se crea con
     * estado PENDIENTE_DE_APROBACION y debe ser aprobada por un ADMINISTRADOR
     * antes de ser publicada.
     * 
     * <p>El proceso incluye:
     * <ul>
     *   <li>Validación de permisos del usuario autenticado</li>
     *   <li>Validación de existencia de la empresa</li>
     *   <li>Validación de existencia de las carreras asociadas</li>
     *   <li>Validación de fechas (fecha de caducidad no puede ser anterior a hoy)</li>
     *   <li>Inserción en la base de datos</li>
     *   <li>Asociación con las carreras especificadas</li>
     * </ul>
     * 
     * @param request DTO con los datos de la pasantía a registrar.
     *                Debe incluir: título, puesto, ciudad, modalidad, empresa, etc.
     *                Las validaciones se realizan mediante @Valid
     * @return ResponseEntity con código HTTP 201 (CREATED) y el objeto PasantiaResponseDTO
     *         creado si la operación es exitosa,
     *         o código HTTP 403 (FORBIDDEN) si el usuario no tiene permisos,
     *         o código HTTP 400 (BAD_REQUEST) si los datos son inválidos,
     *         o código HTTP 500 (INTERNAL_SERVER_ERROR) en caso de error
     * @throws SecurityException si el usuario no tiene permisos para crear pasantías
     *                          para la empresa especificada
     * @throws IllegalArgumentException si los datos proporcionados son inválidos
     *                                  (empresa no existe, carrera no existe, fechas inválidas)
     * @see PasantiaRequestDTO
     * @see PasantiaResponseDTO
     * @see EstadoPasantia#PENDIENTE_DE_APROBACION
     */
    @PostMapping("/registrar")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'EMPRESA')")
    public ResponseEntity<?> registrarPasantia(@Valid @RequestBody PasantiaRequestDTO request) {
        try {
            // Validar que el usuario autenticado tiene permiso para crear pasantía para esta empresa
            System.out.println("MARCOS ID EMPRESA " + request.getIdEmpresa());
            securityService.validarPermisoCrearPasantia(request.getIdEmpresa());
            
            // Crear la pasantía en la base de datos
            PasantiaResponseDTO pasantia = pasantiaService.crearPasantia(request);
            
            // Construir respuesta exitosa
            Map<String, Object> response = new HashMap<>();
            response.put("codigo", 0);
            response.put("mensaje", "Pasantía registrada exitosamente");
            response.put("data", pasantia);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(response);
        } catch (SecurityException | AccessDeniedException e) {
            // Usuario no tiene permisos para realizar esta acción
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("codigo", -1);
            errorResponse.put("mensaje", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        } catch (IllegalArgumentException e) {
            // Datos inválidos (empresa no existe, carrera no existe, fechas inválidas, etc.)
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("codigo", -1);
            errorResponse.put("mensaje", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            // Error inesperado del servidor
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("codigo", -1);
            errorResponse.put("mensaje", "Error al registrar la pasantía: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Aprueba y publica una pasantía cambiando su estado a PUBLICADA.
     * 
     * <p>Este endpoint está restringido exclusivamente a usuarios con rol ADMINISTRADOR.
     * Solo las pasantías en estado PENDIENTE_DE_APROBACION pueden ser aprobadas.
     * Una vez aprobada, la pasantía queda visible públicamente en el endpoint
     * GET /pasantias/publicadas.
     * 
     * <p>El proceso incluye:
     * <ul>
     *   <li>Validación de que el usuario es ADMINISTRADOR</li>
     *   <li>Validación de que la pasantía existe</li>
     *   <li>Validación de que la pasantía está en estado PENDIENTE_DE_APROBACION</li>
     *   <li>Actualización del estado a PUBLICADA</li>
     * </ul>
     * 
     * @param id ID único de la pasantía a aprobar
     * @return ResponseEntity con código HTTP 200 (OK) y el objeto PasantiaResponseDTO
     *         actualizado si la operación es exitosa,
     *         o código HTTP 403 (FORBIDDEN) si el usuario no es ADMINISTRADOR,
     *         o código HTTP 400 (BAD_REQUEST) si la pasantía no existe o no puede
     *         ser aprobada (estado inválido),
     *         o código HTTP 500 (INTERNAL_SERVER_ERROR) en caso de error
     * @throws SecurityException si el usuario no tiene rol ADMINISTRADOR
     * @throws IllegalArgumentException si la pasantía no existe
     * @throws IllegalStateException si la pasantía no está en estado PENDIENTE_DE_APROBACION
     * @see EstadoPasantia#PUBLICADA
     * @see EstadoPasantia#PENDIENTE_DE_APROBACION
     */
    @PutMapping("/{id}/aprobar")
    public ResponseEntity<?> aprobarPasantia(@PathVariable Integer id) {
        try {
            // Validar que el usuario es ADMINISTRADOR
            securityService.validarEsAdministrador();
            
            // Crear DTO para actualizar estado a PUBLICADA
            ActualizarEstadoPasantiaDTO estadoDTO = new ActualizarEstadoPasantiaDTO();
            estadoDTO.setEstado(EstadoPasantia.PUBLICADA);
            
            // Actualizar el estado de la pasantía en la base de datos
            PasantiaResponseDTO pasantia = pasantiaService.actualizarEstado(id, estadoDTO);
            
            // Construir respuesta exitosa
            Map<String, Object> response = new HashMap<>();
            response.put("codigo", 0);
            response.put("mensaje", "Pasantía aprobada y publicada exitosamente");
            response.put("data", pasantia);
            
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(response);
        } catch (SecurityException | AccessDeniedException e) {
            // Usuario no es ADMINISTRADOR
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("codigo", -1);
            errorResponse.put("mensaje", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        } catch (IllegalArgumentException e) {
            // La pasantía no existe
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("codigo", -1);
            errorResponse.put("mensaje", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (IllegalStateException e) {
            // La pasantía no está en un estado válido para ser aprobada
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("codigo", -1);
            errorResponse.put("mensaje", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            // Error inesperado del servidor
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("codigo", -1);
            errorResponse.put("mensaje", "Error al aprobar la pasantía: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Finaliza una pasantía cambiando su estado a FINALIZADA.
     * 
     * <p>Este endpoint está restringido exclusivamente a usuarios con rol ADMINISTRADOR.
     * Solo las pasantías en estado PUBLICADA pueden ser finalizadas. Una vez finalizada,
     * la pasantía ya no aparecerá en el listado de pasantías publicadas y no podrá
     * recibir nuevas postulaciones.
     * 
     * <p>El proceso incluye:
     * <ul>
     *   <li>Validación de que el usuario es ADMINISTRADOR</li>
     *   <li>Validación de que la pasantía existe</li>
     *   <li>Validación de que la pasantía está en estado PUBLICADA</li>
     *   <li>Actualización del estado a FINALIZADA</li>
     * </ul>
     * 
     * @param id ID único de la pasantía a finalizar
     * @return ResponseEntity con código HTTP 200 (OK) y el objeto PasantiaResponseDTO
     *         actualizado si la operación es exitosa,
     *         o código HTTP 403 (FORBIDDEN) si el usuario no es ADMINISTRADOR,
     *         o código HTTP 400 (BAD_REQUEST) si la pasantía no existe o no puede
     *         ser finalizada (estado inválido),
     *         o código HTTP 500 (INTERNAL_SERVER_ERROR) en caso de error
     * @throws SecurityException si el usuario no tiene rol ADMINISTRADOR
     * @throws IllegalArgumentException si la pasantía no existe
     * @throws IllegalStateException si la pasantía no está en estado PUBLICADA
     * @see EstadoPasantia#FINALIZADA
     * @see EstadoPasantia#PUBLICADA
     */
    @PutMapping("/{id}/finalizar")
    public ResponseEntity<?> finalizarPasantia(@PathVariable Integer id) {
        try {
            // Validar que el usuario es ADMINISTRADOR
            securityService.validarEsAdministrador();
            
            // Crear DTO para actualizar estado a FINALIZADA
            ActualizarEstadoPasantiaDTO estadoDTO = new ActualizarEstadoPasantiaDTO();
            estadoDTO.setEstado(EstadoPasantia.FINALIZADA);
            
            // Actualizar el estado de la pasantía en la base de datos
            PasantiaResponseDTO pasantia = pasantiaService.actualizarEstado(id, estadoDTO);
            
            // Construir respuesta exitosa
            Map<String, Object> response = new HashMap<>();
            response.put("codigo", 0);
            response.put("mensaje", "Pasantía finalizada exitosamente");
            response.put("data", pasantia);
            
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(response);
        } catch (SecurityException | AccessDeniedException e) {
            // Usuario no es ADMINISTRADOR
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("codigo", -1);
            errorResponse.put("mensaje", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        } catch (IllegalArgumentException e) {
            // La pasantía no existe
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("codigo", -1);
            errorResponse.put("mensaje", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (IllegalStateException e) {
            // La pasantía no está en un estado válido para ser finalizada
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("codigo", -1);
            errorResponse.put("mensaje", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            // Error inesperado del servidor
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("codigo", -1);
            errorResponse.put("mensaje", "Error al finalizar la pasantía: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/misPasantias")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('EMPRESA')")
    public ResponseEntity<Map<String, Object>> obtenerMisPasantias() {
        try {
            List<PasantiaResponseDTO> pasantias = pasantiaService.obtenerMisPasantias();

            Map<String, Object> response = new HashMap<>();
            response.put("codigo", 0);
            response.put("mensaje", "Pasantías encontradas");
            response.put("data", pasantias);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("codigo", -1);
            errorResponse.put("mensaje", "Error al obtener pasantías: " + e.getMessage());
            errorResponse.put("data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}

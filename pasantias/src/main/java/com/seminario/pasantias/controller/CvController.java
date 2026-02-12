package com.seminario.pasantias.controller;

import com.seminario.pasantias.dto.CvDto;
import com.seminario.pasantias.entity.Cv;
import com.seminario.pasantias.entity.Estudiante;
import com.seminario.pasantias.response.GenericResponse;
import com.seminario.pasantias.service.CvService;
import com.seminario.pasantias.service.EstudianteService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/cvs")
@CrossOrigin(
        origins = {
                "http://localhost:3000",
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:4173",
                "http://127.0.0.1:4173"
        },
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
        allowedHeaders = "*")
public class CvController {

    @Autowired
    private CvService cvService;

    @Autowired
    private EstudianteService estudianteService;

    @PostMapping("/subirCV")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ESTUDIANTE')")
    public GenericResponse subirCv(@RequestParam("file") MultipartFile file, @RequestParam("idEstudiante") Integer idEstudiante) {
        GenericResponse response = new GenericResponse();
        try {
            if (!"application/pdf".equals(file.getContentType())) {
                response.setCode(-1);
                response.setMessage("Solo se permiten archivos PDF.");
                return response;
            }
            cvService.subirCv(file, idEstudiante);
            response.setCode(0);
            response.setMessage("CV subido exitosamente.");
            return response;
        } catch (IOException e) {
            response.setCode(-1);
            response.setMessage("Error al leer el archivo: " + e.getMessage());
            return response;
        } catch (Exception e) {
            response.setCode(-1);
            response.setMessage("Error al subir el CV: " + e.getMessage());
            return response;
        }
    }

    @GetMapping("/getCV")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ESTUDIANTE')")
    public ResponseEntity<List<CvDto>> listarCvs(@RequestParam("idEstudiante") Integer idEstudiante) {
        List<CvDto> cvs = cvService.listarCvsPorEstudiante(idEstudiante);
        return new ResponseEntity<>(cvs, HttpStatus.OK);
    }

    @GetMapping("/descargarCV/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ESTUDIANTE')")
    public ResponseEntity<byte[]> descargarCv(@PathVariable Integer id) {
        Optional<Cv> cvOpt = cvService.descargarCv(id);
        if (cvOpt.isPresent()) {
            Cv cv = cvOpt.get();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + cv.getNombreArchivo() + "\"")
                    .body(cv.getDatosCv());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/eliminarCV/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ESTUDIANTE')")
    public GenericResponse eliminarCv(@PathVariable Integer id) {
        GenericResponse response = new GenericResponse();
        try {
            cvService.eliminarCv(id);
            response.setCode(0);
            response.setMessage("CV eliminado exitosamente.");
            return response;
        } catch (Exception e) {
            response.setCode(-1);
            response.setMessage("Error al eliminar el CV: " + e.getMessage());
            return response;
        }
    }
}

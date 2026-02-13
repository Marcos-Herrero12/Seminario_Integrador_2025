package com.seminario.pasantias.service;

import com.seminario.pasantias.dto.request.*;
import com.seminario.pasantias.dto.response.*;
import com.seminario.pasantias.entity.*;
import com.seminario.pasantias.persistence.*;
import com.seminario.pasantias.util.PasantiaMapperUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class PasantiaService {

    private final com.seminario.pasantias.persistence.PasantiaMapper pasantiaMapper;
    private final EmpresaMapper empresaMapper;
    private final CarreraMapper carreraMapper;
    private final PostulacionMapper postulacionMapper;
    private final PasantiaMapperUtil mapperUtil;
    private final UsuarioService usuarioService;

    @Autowired
    public PasantiaService(
            com.seminario.pasantias.persistence.PasantiaMapper pasantiaMapper,
            EmpresaMapper empresaMapper,
            CarreraMapper carreraMapper,
            PostulacionMapper postulacionMapper,
            PasantiaMapperUtil mapperUtil,
            UsuarioService usuarioService) {
        this.pasantiaMapper = pasantiaMapper;
        this.empresaMapper = empresaMapper;
        this.carreraMapper = carreraMapper;
        this.postulacionMapper = postulacionMapper;
        this.mapperUtil = mapperUtil;
        this.usuarioService = usuarioService;
    }

    /**
     * Crear una nueva pasantía
     */
    public PasantiaResponseDTO crearPasantia(PasantiaRequestDTO request) {
        // Validar que la empresa existe
        Empresa empresa = empresaMapper.findById(request.getIdEmpresa());
        System.out.println("MARCOS " + empresa.toString());
        if (empresa == null) {
            throw new IllegalArgumentException("La empresa con ID " + request.getIdEmpresa() + " no existe");
        }

        // Validar que las carreras existen
        if (request.getIdsCarreras() != null && !request.getIdsCarreras().isEmpty()) {
            for (Integer carreraId : request.getIdsCarreras()) {
                Carrera carrera = carreraMapper.findById(carreraId);
                if (carrera == null) {
                    throw new IllegalArgumentException("La carrera con ID " + carreraId + " no existe");
                }
            }
        }

        // Validar fechas
        if (request.getFechaCaducidad() != null && request.getFechaCaducidad().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de caducidad no puede ser anterior a hoy");
        }

        // Convertir DTO a Entity
        Pasantia pasantia = mapperUtil.requestDtoToEntity(request);
        pasantia.setFechaPublicacion(LocalDate.now());
        pasantia.setEstado(EstadoPasantia.PENDIENTE_DE_APROBACION);

        // Setear la empresa
        pasantia.setEmpresa(empresa);

        // Insertar en BD
        pasantiaMapper.insert(pasantia);

        // Si hay carreras, asociarlas
        if (request.getIdsCarreras() != null && !request.getIdsCarreras().isEmpty()) {
            for (Integer carreraId : request.getIdsCarreras()) {
                pasantiaMapper.insertPasantiaCarrera(pasantia.getIdPasantia(), carreraId);
            }
        }

        // Retornar con relaciones cargadas
        return mapperUtil.entityToResponseDto(
                pasantiaMapper.findByIdWithRelations(pasantia.getIdPasantia()).orElseThrow()
        );
    }

    /**
     * Actualizar una pasantía existente
     */
    public PasantiaResponseDTO actualizarPasantia(Integer id, PasantiaRequestDTO request) {
        // Validar que la pasantía exists
        Optional<Pasantia> pasantiaOpt = pasantiaMapper.findById(id);
        if (pasantiaOpt.isEmpty()) {
            throw new IllegalArgumentException("Pasantía no encontrada con ID: " + id);
        }
        Pasantia pasantiaExistente = pasantiaOpt.get();

        // Solo se pueden modificar pasantías en ciertos estados
        if (pasantiaExistente.getEstado() == EstadoPasantia.FINALIZADA ||
            pasantiaExistente.getEstado() == EstadoPasantia.DADA_DE_BAJA) {
            throw new IllegalStateException("No se puede modificar una pasantía en estado " + pasantiaExistente.getEstado());
        }

        // Validar que la empresa existe
        if (request.getIdEmpresa() != null) {
            Empresa empresa = empresaMapper.findById(request.getIdEmpresa());
            if (empresa == null) {
                throw new IllegalArgumentException("Empresa no encontrada");
            }
            pasantiaExistente.setEmpresa(empresa);
        }

        // Actualizar campos
        mapperUtil.updateEntityFromRequestDto(request, pasantiaExistente);
        pasantiaMapper.update(pasantiaExistente);

        // Actualizar carreras si se especificaron
        if (request.getIdsCarreras() != null) {
            // Eliminar asociaciones existentes
            pasantiaMapper.deletePasantiaCarrerasByPasantiaId(id);
            
            // Insertar nuevas asociaciones
            for (Integer carreraId : request.getIdsCarreras()) {
                Carrera carrera = carreraMapper.findById(carreraId);
                if (carrera == null) {
                    throw new IllegalArgumentException("Carrera no encontrada: " + carreraId);
                }
                pasantiaMapper.insertPasantiaCarrera(id, carreraId);
            }
        }

        // Retornar actualizada
        Optional<Pasantia> actualizadaOpt = pasantiaMapper.findByIdWithRelations(id);
        return mapperUtil.entityToResponseDto(actualizadaOpt.orElseThrow());
    }

    /**
     * Cambiar el estado de una pasantía
     */
    public PasantiaResponseDTO actualizarEstado(Integer id, ActualizarEstadoPasantiaDTO request) {
        Pasantia pasantia = pasantiaMapper.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pasantía no encontrada con ID: " + id));

        // Validar transiciones de estado permitidas
        validarTransicionEstado(pasantia.getEstado(), request.getEstado());

        pasantia.setEstado(request.getEstado());
        pasantiaMapper.update(pasantia);

        return mapperUtil.entityToResponseDto(
                pasantiaMapper.findByIdWithRelations(id).orElseThrow()
        );
    }

    /**
     * Obtener pasantía por ID con toda la información
     */
    @Transactional(readOnly = true)
    public PasantiaDetalleDTO obtenerPasantiaPorId(Integer id) {
        Pasantia pasantia = pasantiaMapper.findByIdWithRelations(id)
                .orElseThrow(() -> new IllegalArgumentException("Pasantía no encontrada con ID: " + id));

        return mapperUtil.entityToDetalleDto(pasantia);
    }

    /**
     * Buscar pasantías con filtros y paginación
     */
    @Transactional(readOnly = true)
    public PaginaDTO<PasantiaResponseDTO> buscarPasantias(PasantiaFiltroDTO filtro) {
        // Validar paginación
        if (filtro.getPagina() != null && filtro.getPagina() < 0) {
            filtro.setPagina(0);
        }
        if (filtro.getTamanio() != null && (filtro.getTamanio() < 1 || filtro.getTamanio() > 100)) {
            filtro.setTamanio(20);
        }

        // Buscar con filtros
        List<Pasantia> pasantias = pasantiaMapper.findWithFilters(filtro);
        long total = pasantiaMapper.countWithFilters(filtro);

        // Convertir a DTOs
        List<PasantiaResponseDTO> dtos = pasantias.stream()
                .map(mapperUtil::entityToResponseDto)
                .collect(Collectors.toList());

        // Crear página
        PaginaDTO<PasantiaResponseDTO> pagina = new PaginaDTO<>();
        pagina.setContenido(dtos);
        pagina.setTotalElementos(total);
        pagina.setPaginaActual(filtro.getPagina() != null ? filtro.getPagina() : 0);
        pagina.setTamanioPagina(filtro.getTamanio() != null ? filtro.getTamanio() : 20);
        pagina.setTotalPaginas((int) Math.ceil((double) total / pagina.getTamanioPagina()));

        return pagina;
    }

    /**
     * Obtener pasantías de una empresa
     */
    @Transactional(readOnly = true)
    public List<PasantiaResponseDTO> obtenerPasantiasPorEmpresa(Integer empresaId) {
        List<Pasantia> pasantias = pasantiaMapper.findByEmpresaId(empresaId);
        return pasantias.stream()
                .map(mapperUtil::entityToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtener pasantías por carrera
     */
    @Transactional(readOnly = true)
    public List<PasantiaResponseDTO> obtenerPasantiasPorCarrera(Integer carreraId) {
        List<Pasantia> pasantias = pasantiaMapper.findByCarreraId(carreraId);
        return pasantias.stream()
                .map(mapperUtil::entityToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtener todas las pasantías
     */
    @Transactional(readOnly = true)
    public List<PasantiaResponseDTO> obtenerTodasLasPasantias() {
        List<Pasantia> pasantias = pasantiaMapper.findAll();
        return pasantias.stream()
                .map(mapperUtil::entityToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtener pasantías publicadas (disponibles para postular)
     */
    @Transactional(readOnly = true)
    public List<PasantiaResponseDTO> obtenerPasantiasPublicadas() {
        List<Pasantia> pasantias = pasantiaMapper.findByEstado("PUBLICADA");
        return pasantias.stream()
                .map(mapperUtil::entityToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Eliminar pasantía (soft delete)
     */
    public void eliminarPasantia(Integer id) {
        Optional<Pasantia> pasantiaOpt = pasantiaMapper.findById(id);
        if (pasantiaOpt.isEmpty()) {
            throw new IllegalArgumentException("Pasantía no encontrada con ID: " + id);
        }
        Pasantia pasantia = pasantiaOpt.get();

        // Validar que no tenga postulaciones activas
        Integer postulacionesActivas = postulacionMapper.countByPasantiaIdAndEstadoNot(
                id, 
                "FINALIZADA"
        );

        if (postulacionesActivas > 0) {
            throw new IllegalStateException(
                    "No se puede eliminar la pasantía porque tiene " + postulacionesActivas + " postulaciones activas"
            );
        }

        pasantia.setEstado(EstadoPasantia.DADA_DE_BAJA);
        pasantiaMapper.update(pasantia);
    }

    /**
     * Validar transiciones de estado permitidas
     */
    private void validarTransicionEstado(EstadoPasantia estadoActual, EstadoPasantia nuevoEstado) {
        switch (estadoActual) {
            case PENDIENTE_DE_APROBACION:
                if (nuevoEstado != EstadoPasantia.PUBLICADA && nuevoEstado != EstadoPasantia.DADA_DE_BAJA) {
                    throw new IllegalStateException(
                            "Desde PENDIENTE_DE_APROBACION solo se puede pasar a PUBLICADA o DADA_DE_BAJA"
                    );
                }
                break;
            case PUBLICADA:
                if (nuevoEstado != EstadoPasantia.FINALIZADA && 
                    nuevoEstado != EstadoPasantia.DADA_DE_BAJA &&
                    nuevoEstado != EstadoPasantia.EXPIRADA) {
                    throw new IllegalStateException(
                            "Desde PUBLICADA solo se puede pasar a FINALIZADA, DADA_DE_BAJA o EXPIRADA"
                    );
                }
                break;
            case FINALIZADA:
            case DADA_DE_BAJA:
            case EXPIRADA:
                throw new IllegalStateException(
                        "No se puede cambiar el estado de una pasantía en estado " + estadoActual
                );
        }
    }

    /**
     * Obtener pasantías de la empresa autenticada
     */
    @Transactional(readOnly = true)
    public List<PasantiaResponseDTO> obtenerMisPasantias() {
        // Recuperar usuario autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        // Buscar el usuario en BD
        Optional<Usuario> usuarioOpt = usuarioService.findByUsername(username);
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado");
        }
        Usuario usuario = usuarioOpt.get();

        // Buscar la empresa asociada al usuario
        Empresa empresa = empresaMapper.findByIdUsuario(usuario.getIdUsuario());
        if (empresa == null) {
            throw new RuntimeException("Empresa no encontrada para el usuario");
        }

        // Buscar pasantías de la empresa
        List<Pasantia> pasantias = pasantiaMapper.findByEmpresaId(empresa.getIdEmpresa());
        return pasantias.stream()
                .map(mapperUtil::entityToResponseDto)
                .collect(Collectors.toList());
    }
}

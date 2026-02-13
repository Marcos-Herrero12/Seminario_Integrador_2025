package com.seminario.pasantias.service;

import com.seminario.pasantias.dto.request.*;
import com.seminario.pasantias.dto.response.*;
import com.seminario.pasantias.entity.*;
import com.seminario.pasantias.persistence.PostulacionMapper;
import com.seminario.pasantias.persistence.EstudianteMapper;
import com.seminario.pasantias.persistence.PasantiaMapper;
import com.seminario.pasantias.persistence.EmpresaMapper;
import com.seminario.pasantias.util.PostulacionMapperUtil;
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
public class PostulacionService {

    private final PostulacionMapper postulacionMapper;
    private final EstudianteMapper estudianteMapper;
    private final PasantiaMapper pasantiaMapper;
    private final EmpresaMapper empresaMapper;
    private final PostulacionMapperUtil mapperUtil;
    private final UsuarioService usuarioService;
    private final NotificacionService notificacionService;

    @Autowired
    public PostulacionService(
            PostulacionMapper postulacionMapper,
            EstudianteMapper estudianteMapper,
            PasantiaMapper pasantiaMapper,
            EmpresaMapper empresaMapper,
            PostulacionMapperUtil mapperUtil, 
            UsuarioService usuarioService,
            NotificacionService notificacionService) {
        this.postulacionMapper = postulacionMapper;
        this.estudianteMapper = estudianteMapper;
        this.pasantiaMapper = pasantiaMapper;
        this.empresaMapper = empresaMapper;
        this.mapperUtil = mapperUtil;
        this.usuarioService = usuarioService;
        this.notificacionService = notificacionService;
    }

    /**
     * Crear una nueva postulación
     */
    public PostulacionResponseDTO crearPostulacion(PostulacionRequestDTO request) {
        // Validar que el estudiante existe
        Optional<Estudiante> estudianteOpt = estudianteMapper.findById(request.getIdEstudiante());
        System.out.println("MARCOS ESTUDIANTE: " + estudianteOpt.toString());
        if (estudianteOpt.isEmpty()) {
            throw new IllegalArgumentException("El estudiante con ID " + request.getIdEstudiante() + " no existe");
        }

        // Validar que la pasantía existe
        Optional<Pasantia> pasantiaOpt = pasantiaMapper.findById(request.getIdPasantia());
        if (pasantiaOpt.isEmpty()) {
            throw new IllegalArgumentException("La pasantía con ID " + request.getIdPasantia() + " no existe");
        }

        Pasantia pasantia = pasantiaOpt.get();
        System.out.println("AGUSTIN PASANTIA " + pasantia.toString() );
        // Verificar que la pasantía esté en estado PUBLICADA
        if (pasantia.getEstado() != EstadoPasantia.PUBLICADA) {
            throw new IllegalStateException("La pasantía no está disponible para postulaciones. Estado actual: " + pasantia.getEstado());
        }

        // Verificar que no haya caducado
        if (pasantia.getFechaCaducidad() != null && pasantia.getFechaCaducidad().isBefore(LocalDate.now())) {
            throw new IllegalStateException("La pasantía ha caducado");
        }

        // Verificar que el estudiante no haya postulado ya a esta pasantía
        boolean yaPostulo = postulacionMapper.existsByEstudianteAndPasantia(
                request.getIdEstudiante(), 
                request.getIdPasantia()
        );
        System.out.println("AGUSTIN YA POSTULO? " + yaPostulo);
        if (yaPostulo) {
            throw new IllegalStateException("El estudiante ya tiene una postulación para esta pasantía");
        }

        // Convertir DTO a Entity
        Postulacion postulacion = mapperUtil.requestDtoToEntity(request);
        postulacion.setPasantia(pasantia);
        postulacion.setEstudiante(estudianteOpt.get());

        // Insertar en BD
        postulacionMapper.insert(postulacion);
        System.out.println("Empresa: " + pasantia.getEmpresa()+ " id-usuario : " + pasantia.getEmpresa().getIdUsuario());
        // Notificar a la empresa
        if (pasantia.getEmpresa() != null && pasantia.getEmpresa().getIdUsuario() != null) {
            String mensaje = "Nueva postulación recibida para la pasantía: " + pasantia.getTitulo();
            notificacionService.crearNotificacion(pasantia.getEmpresa().getIdUsuario(), mensaje);
        }

        // Retornar con relaciones cargadas
            return mapperUtil.entityToResponseDto(
                postulacionMapper.findByIdWithRelations(postulacion.getIdPostulacion()).orElseThrow()
        );
    }

    /**
     * Actualizar una postulación existente
     */
    public PostulacionResponseDTO actualizarPostulacion(Integer id, PostulacionRequestDTO request) {
        // Validar que la postulación existe
        Postulacion postulacionExistente = postulacionMapper.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Postulación no encontrada con ID: " + id));

        // Validar que solo se modifiquen postulaciones en BORRADOR o PENDIENTE_APROBACION
        if (postulacionExistente.getEstado() != EstadoPostulacion.BORRADOR && 
            postulacionExistente.getEstado() != EstadoPostulacion.PENDIENTE_APROBACION) {
            throw new IllegalStateException("Solo se pueden modificar postulaciones en estado BORRADOR o PENDIENTE_APROBACION");
        }

        // Actualizar campos
        mapperUtil.updateEntityFromRequestDto(request, postulacionExistente);
        postulacionMapper.update(postulacionExistente);

        // Retornar actualizada
        return mapperUtil.entityToResponseDto(
                postulacionMapper.findByIdWithRelations(id).orElseThrow()
        );
    }

    /**
     * Cambiar el estado de una postulación
     */
    public PostulacionResponseDTO actualizarEstado(Integer id, ActualizarEstadoPostulacionDTO request) {
        Postulacion postulacion = postulacionMapper.findByIdWithRelations(id)
                .orElseThrow(() -> new IllegalArgumentException("Postulación no encontrada con ID: " + id));

        // Validar transición de estado
        validarTransicionEstado(postulacion.getEstado(), request.getEstado());

        // Si el estado es CUBIERTA, validar datos del contrato
        if (request.getEstado() == EstadoPostulacion.CUBIERTA) {
            if (request.getFechaInicioContrato() == null || request.getDuracionMeses() == null) {
                throw new IllegalArgumentException("Para estado CUBIERTA se requiere fechaInicioContrato y duracionMeses");
            }
            postulacion.setFechaInicioContrato(request.getFechaInicioContrato());
            postulacion.setDuracionMeses(request.getDuracionMeses());
        }

        postulacion.setEstado(request.getEstado());
        postulacionMapper.update(postulacion);

        // Notificar al estudiante sobre el cambio de estado
        if (postulacion.getEstudiante() != null && postulacion.getEstudiante().getIdUsuario() != null) {
            String mensaje = "El estado de tu postulación para " + postulacion.getPasantia().getTitulo() + 
                             " ha cambiado a: " + request.getEstado();
            notificacionService.crearNotificacion(postulacion.getEstudiante().getIdUsuario(), mensaje);
        }

        return mapperUtil.entityToResponseDto(
                postulacionMapper.findByIdWithRelations(id).orElseThrow()
        );
    }

    /**
     * Obtener postulación por ID con toda la información
     */
    @Transactional(readOnly = true)
    public PostulacionDetalleDTO obtenerPostulacionPorId(Integer id) {
        Postulacion postulacion = postulacionMapper.findByIdWithRelations(id)
                .orElseThrow(() -> new IllegalArgumentException("Postulación no encontrada con ID: " + id));

        return mapperUtil.entityToDetalleDto(postulacion);
    }

    /**
     * Buscar postulaciones con filtros y paginación
     */
    @Transactional(readOnly = true)
    public PaginaDTO<PostulacionResponseDTO> buscarPostulaciones(PostulacionFiltroDTO filtro) {
        // Validar paginación
        if (filtro.getPagina() != null && filtro.getPagina() < 0) {
            filtro.setPagina(0);
        }
        if (filtro.getTamanio() != null && (filtro.getTamanio() < 1 || filtro.getTamanio() > 100)) {
            filtro.setTamanio(20);
        }

        // Buscar con filtros
        List<Postulacion> postulaciones = postulacionMapper.findWithFilters(filtro);
        long total = postulacionMapper.countWithFilters(filtro);

        // Convertir a DTOs
        List<PostulacionResponseDTO> dtos = postulaciones.stream()
                .map(mapperUtil::entityToResponseDto)
                .collect(Collectors.toList());

        // Crear página
        PaginaDTO<PostulacionResponseDTO> pagina = new PaginaDTO<>();
        pagina.setContenido(dtos);
        pagina.setTotalElementos(total);
        pagina.setPaginaActual(filtro.getPagina() != null ? filtro.getPagina() : 0);
        pagina.setTamanioPagina(filtro.getTamanio() != null ? filtro.getTamanio() : 20);
        pagina.setTotalPaginas((int) Math.ceil((double) total / pagina.getTamanioPagina()));

        return pagina;
    }

    /**
     * Obtener postulaciones por estudiante
     */
    @Transactional(readOnly = true)
    public List<PostulacionResponseDTO> obtenerPostulacionesPorEstudiante(Integer estudianteId) {
        List<Postulacion> postulaciones = postulacionMapper.findByEstudiante(estudianteId);
        return postulaciones.stream()
                .map(mapperUtil::entityToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtener postulaciones por pasantía
     */
    @Transactional(readOnly = true)
    public List<PostulacionResponseDTO> obtenerPostulacionesPorPasantia(Integer pasantiaId) {
        List<Postulacion> postulaciones = postulacionMapper.findByPasantiaId(pasantiaId);
        return postulaciones.stream()
                .map(mapperUtil::entityToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtener todas las postulaciones de una pasantía con información completa de estudiantes
     */
    @Transactional(readOnly = true)
    public List<PostulacionResponseDTO> obtenerTodasPostulacionesPorPasantia(Integer pasantiaId) {
        List<PostulacionResponseDTO> postulaciones = postulacionMapper.findAllByPasantiaId(pasantiaId);
        
        // Calcular campos adicionales
        postulaciones.forEach(dto -> {
            dto.setEsEditable(
                    dto.getEstado() == EstadoPostulacion.BORRADOR ||
                            dto.getEstado() == EstadoPostulacion.PENDIENTE_APROBACION
            );
        });
        
        return postulaciones;
    }

    /**
     * Obtener todas las postulaciones
     */
    @Transactional(readOnly = true)
    public List<PostulacionResponseDTO> consultarPostulaciones() {
        List<PostulacionResponseDTO> postulaciones = postulacionMapper.findAllResponse();

        // Calcular campos adicionales
        postulaciones.forEach(dto -> {
            dto.setEsEditable(
                    dto.getEstado() == EstadoPostulacion.BORRADOR ||
                            dto.getEstado() == EstadoPostulacion.PENDIENTE_APROBACION
            );
        });

        return postulaciones;
    }


    /**
     * Eliminar postulación (solo si está en BORRADOR)
     */
    public void eliminarPostulacion(Integer id) {
        Postulacion postulacion = postulacionMapper.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Postulación no encontrada con ID: " + id));

        // Solo se pueden eliminar postulaciones en BORRADOR
        if (postulacion.getEstado() != EstadoPostulacion.BORRADOR) {
            throw new IllegalStateException(
                    "Solo se pueden eliminar postulaciones en estado BORRADOR. Estado actual: " + postulacion.getEstado()
            );
        }

        postulacionMapper.delete(id);
    }

    /**
     * Validar transiciones de estado permitidas
     */
    private void validarTransicionEstado(EstadoPostulacion estadoActual, EstadoPostulacion nuevoEstado) {
        switch (estadoActual) {
            case BORRADOR:
                if (nuevoEstado != EstadoPostulacion.PENDIENTE_APROBACION) {
                    throw new IllegalStateException(
                            "Desde BORRADOR solo se puede pasar a PENDIENTE_APROBACION"
                    );
                }
                break;
            case PENDIENTE_APROBACION:
                if (nuevoEstado != EstadoPostulacion.PUBLICADA) {
                    throw new IllegalStateException(
                            "Desde PENDIENTE_APROBACION solo se puede pasar a PUBLICADA"
                    );
                }
                break;
            case PUBLICADA:
                if (nuevoEstado != EstadoPostulacion.CUBIERTA && 
                    nuevoEstado != EstadoPostulacion.FINALIZADA) {
                    throw new IllegalStateException(
                            "Desde PUBLICADA solo se puede pasar a CUBIERTA o FINALIZADA"
                    );
                }
                break;
            case CUBIERTA:
                if (nuevoEstado != EstadoPostulacion.FINALIZADA) {
                    throw new IllegalStateException(
                            "Desde CUBIERTA solo se puede pasar a FINALIZADA"
                    );
                }
                break;
            case FINALIZADA:
                throw new IllegalStateException(
                        "No se puede cambiar el estado de una postulación FINALIZADA"
                );
        }
    }
    @Transactional(readOnly = true)
    public List<PostulacionResponseDTO> consultarMisPostulaciones() {
        // Recuperar usuario autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        // Buscar el usuario en BD
        Optional<Usuario> usuario = usuarioService.findByUsername(username);
        System.out.println("AGUSTIN USUARIO: " + usuario.get().getIdUsuario());
        // Buscar postulaciones por idUsuario
        List<PostulacionResponseDTO> postulaciones = postulacionMapper.findByUsuarioId(usuario.get().getIdUsuario());

        // Calcular campos adicionales
        postulaciones.forEach(dto -> {
            dto.setEsEditable(
                    dto.getEstado() == EstadoPostulacion.BORRADOR ||
                            dto.getEstado() == EstadoPostulacion.PENDIENTE_APROBACION
            );
            dto.setFechaActualizacion(null); // o LocalDateTime.now() si querés setearlo
        });

        return postulaciones;
    }

    /**
     * Obtener postulación del usuario autenticado para una pasantía específica
     */
    @Transactional(readOnly = true)
    public Optional<PostulacionResponseDTO> obtenerPostulacionPorPasantia(Integer pasantiaId) {
        // Recuperar usuario autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        // Buscar el usuario en BD
        Optional<Usuario> usuarioOpt = usuarioService.findByUsername(username);
        if (usuarioOpt.isEmpty()) {
            return Optional.empty();
        }

        Usuario usuario = usuarioOpt.get();

        // Buscar el estudiante asociado al usuario
        Optional<Estudiante> estudianteOpt = estudianteMapper.findByUsuarioId(usuario.getIdUsuario());
        if (estudianteOpt.isEmpty()) {
            return Optional.empty();
        }

        Estudiante estudiante = estudianteOpt.get();

        // Buscar postulación por estudiante y pasantía
        Optional<Postulacion> postulacionOpt = postulacionMapper.findByEstudianteAndPasantia(
                estudiante.getIdEstudiante(), 
                pasantiaId
        );

        if (postulacionOpt.isEmpty()) {
            return Optional.empty();
        }

        // Convertir a DTO
        PostulacionResponseDTO dto = mapperUtil.entityToResponseDto(postulacionOpt.get());
        dto.setEsEditable(
                dto.getEstado() == EstadoPostulacion.BORRADOR ||
                        dto.getEstado() == EstadoPostulacion.PENDIENTE_APROBACION
        );

        return Optional.of(dto);
    }

    /**
     * Obtener postulaciones de la empresa autenticada
     */
    @Transactional(readOnly = true)
    public List<PostulacionResponseDTO> obtenerPostulacionesMiEmpresa() {
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

        // Buscar postulaciones de las pasantías de la empresa
        List<PostulacionResponseDTO> postulaciones = postulacionMapper.findByEmpresaId(empresa.getIdEmpresa());

        // Calcular campos adicionales
        postulaciones.forEach(dto -> {
            dto.setEsEditable(
                    dto.getEstado() == EstadoPostulacion.BORRADOR ||
                            dto.getEstado() == EstadoPostulacion.PENDIENTE_APROBACION
            );
        });

        return postulaciones;
    }
}

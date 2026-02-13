package com.seminario.pasantias.persistence;

import com.seminario.pasantias.dto.request.PasantiaFiltroDTO;
import com.seminario.pasantias.entity.Carrera;
import com.seminario.pasantias.entity.Pasantia;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Optional;

/**
 * Mapper de MyBatis para la entidad Pasantia.
 * Define operaciones CRUD y consultas personalizadas.
 */
@Mapper
public interface PasantiaMapper {

    /**
     * Buscar una pasantía por su ID.
     * Incluye información de la empresa.
     */
    @Select("""
        SELECT 
            p.id_pasantia AS idPasantia,
            p.titulo,
            p.puesto_a_cubrir AS puestoACubrir,
            p.ciudad,
            p.modalidad,
            p.asignacion_estimulo AS asignacionEstimulo,
            p.cantidad_de_pasantes AS cantidadDePasantes,
            p.fecha_publicacion AS fechaPublicacion,
            p.fecha_caducidad AS fechaCaducidad,
            p.estado,
            p.email_contacto AS emailContacto,
            p.conocimientos,
            p.otros_requisitos AS otrosRequisitos,
            p.beneficios,
            p.id_empresa AS idEmpresa,
            e.nombre AS nombreEmpresa,
            e.id_usuario AS idUsuarioEmpresa
        FROM Pasantia p
        LEFT JOIN Empresa e ON p.id_empresa = e.id_empresa
        WHERE p.id_pasantia = #{id}
    """)
    @Results(id = "pasantiaResult", value = {
        @Result(property = "idPasantia", column = "idPasantia"),
        @Result(property = "puestoACubrir", column = "puestoACubrir"),
        @Result(property = "asignacionEstimulo", column = "asignacionEstimulo"),
        @Result(property = "cantidadDePasantes", column = "cantidadDePasantes"),
        @Result(property = "fechaPublicacion", column = "fechaPublicacion"),
        @Result(property = "fechaCaducidad", column = "fechaCaducidad"),
        @Result(property = "emailContacto", column = "emailContacto"),
        @Result(property = "conocimientos", column = "conocimientos"),
        @Result(property = "otrosRequisitos", column = "otrosRequisitos"),
        @Result(property = "beneficios", column = "beneficios"),
        @Result(property = "empresa.idEmpresa", column = "idEmpresa"),
        @Result(property = "empresa.nombre", column = "nombreEmpresa"),
        @Result(property = "empresa.idUsuario", column = "idUsuarioEmpresa")

    })
    Optional<Pasantia> findById(@Param("id") Integer id);

    /**
     * Buscar pasantía completa con todas sus relaciones.
     */
    @Select("""
        SELECT 
            p.id_pasantia AS idPasantia,
            p.titulo,
            p.puesto_a_cubrir AS puestoACubrir,
            p.ciudad,
            p.modalidad,
            p.asignacion_estimulo AS asignacionEstimulo,
            p.cantidad_de_pasantes AS cantidadDePasantes,
            p.fecha_publicacion AS fechaPublicacion,
            p.fecha_caducidad AS fechaCaducidad,
            p.estado,
            p.email_contacto AS emailContacto,
            p.conocimientos,
            p.otros_requisitos AS otrosRequisitos,
            p.beneficios,
            p.id_empresa AS idEmpresa
        FROM Pasantia p
        WHERE p.id_pasantia = #{id}
    """)
    @Results({
        @Result(property = "idPasantia", column = "idPasantia"),
        @Result(property = "empresa", column = "idEmpresa", 
                one = @One(select = "com.seminario.pasantias.persistence.EmpresaMapper.findById")),
        @Result(property = "carreras", column = "idPasantia", 
                many = @Many(select = "findCarrerasByPasantiaId")),
        @Result(property = "postulaciones", column = "idPasantia", 
                many = @Many(select = "com.seminario.pasantias.persistence.PostulacionMapper.findByPasantiaId"))
    })
    Optional<Pasantia> findByIdWithRelations(@Param("id") Integer id);

    /**
     * Listar todas las pasantías activas (PUBLICADA).
     */
    @Select("""
        SELECT 
            p.id_pasantia AS idPasantia,
            p.titulo,
            p.puesto_a_cubrir AS puestoACubrir,
            p.ciudad,
            p.modalidad,
            p.asignacion_estimulo AS asignacionEstimulo,
            p.cantidad_de_pasantes AS cantidadDePasantes,
            p.fecha_publicacion AS fechaPublicacion,
            p.fecha_caducidad AS fechaCaducidad,
            p.estado,
            p.email_contacto AS emailContacto,
            p.conocimientos,
            p.otros_requisitos AS otrosRequisitos,
            p.beneficios,
            p.id_empresa AS idEmpresa,
            e.nombre AS nombreEmpresa
        FROM Pasantia p
        LEFT JOIN Empresa e ON p.id_empresa = e.id_empresa
        WHERE p.estado = 'PUBLICADA'
        ORDER BY p.fecha_publicacion DESC
    """)
    @ResultMap("pasantiaResult")
    List<Pasantia> findAllActive();

    /**
     * Listar todas las pasantías.
     */
    @Select("""
        SELECT 
            p.id_pasantia AS idPasantia,
            p.titulo,
            p.puesto_a_cubrir AS puestoACubrir,
            p.ciudad,
            p.modalidad,
            p.asignacion_estimulo AS asignacionEstimulo,
            p.cantidad_de_pasantes AS cantidadDePasantes,
            p.fecha_publicacion AS fechaPublicacion,
            p.fecha_caducidad AS fechaCaducidad,
            p.estado,
            p.email_contacto AS emailContacto,
            p.conocimientos,
            p.otros_requisitos AS otrosRequisitos,
            p.beneficios,
            p.id_empresa AS idEmpresa,
            e.nombre AS nombreEmpresa
        FROM Pasantia p
        LEFT JOIN Empresa e ON p.id_empresa = e.id_empresa
        ORDER BY p.fecha_publicacion DESC
    """)
    @ResultMap("pasantiaResult")
    List<Pasantia> findAll();

    /**
     * Buscar pasantías por empresa.
     */
    @Select("""
        SELECT 
            p.id_pasantia AS idPasantia,
            p.titulo,
            p.puesto_a_cubrir AS puestoACubrir,
            p.ciudad,
            p.modalidad,
            p.asignacion_estimulo AS asignacionEstimulo,
            p.cantidad_de_pasantes AS cantidadDePasantes,
            p.fecha_publicacion AS fechaPublicacion,
            p.fecha_caducidad AS fechaCaducidad,
            p.estado,
            p.email_contacto AS emailContacto,
            p.conocimientos,
            p.otros_requisitos AS otrosRequisitos,
            p.beneficios,
            p.id_empresa AS idEmpresa,
            e.nombre AS nombreEmpresa
        FROM Pasantia p
        LEFT JOIN Empresa e ON p.id_empresa = e.id_empresa
        WHERE p.id_empresa = #{idEmpresa}
        ORDER BY p.fecha_publicacion DESC
    """)
    @ResultMap("pasantiaResult")
    List<Pasantia> findByEmpresa(@Param("idEmpresa") Integer idEmpresa);

    /**
     * Buscar pasantías por ciudad.
     */
    @Select("""
        SELECT 
            p.id_pasantia AS idPasantia,
            p.titulo,
            p.puesto_a_cubrir AS puestoACubrir,
            p.ciudad,
            p.modalidad,
            p.asignacion_estimulo AS asignacionEstimulo,
            p.cantidad_de_pasantes AS cantidadDePasantes,
            p.fecha_publicacion AS fechaPublicacion,
            p.fecha_caducidad AS fechaCaducidad,
            p.estado,
            p.email_contacto AS emailContacto,
            p.conocimientos,
            p.otros_requisitos AS otrosRequisitos,
            p.beneficios,
            p.id_empresa AS idEmpresa,
            e.nombre AS nombreEmpresa
        FROM Pasantia p
        LEFT JOIN Empresa e ON p.id_empresa = e.id_empresa
        WHERE p.ciudad = #{ciudad}
        AND p.estado = 'PUBLICADA'
        ORDER BY p.fecha_publicacion DESC
    """)
    @ResultMap("pasantiaResult")
    List<Pasantia> findByCiudad(@Param("ciudad") String ciudad);

    /**
     * Buscar pasantías por modalidad.
     */
    @Select("""
        SELECT 
            p.id_pasantia AS idPasantia,
            p.titulo,
            p.puesto_a_cubrir AS puestoACubrir,
            p.ciudad,
            p.modalidad,
            p.asignacion_estimulo AS asignacionEstimulo,
            p.cantidad_de_pasantes AS cantidadDePasantes,
            p.fecha_publicacion AS fechaPublicacion,
            p.fecha_caducidad AS fechaCaducidad,
            p.estado,
            p.email_contacto AS emailContacto,
            p.conocimientos,
            p.otros_requisitos AS otrosRequisitos,
            p.beneficios,
            p.id_empresa AS idEmpresa,
            e.nombre AS nombreEmpresa
        FROM Pasantia p
        LEFT JOIN Empresa e ON p.id_empresa = e.id_empresa
        WHERE p.modalidad = #{modalidad}
        AND p.estado = 'PUBLICADA'
        ORDER BY p.fecha_publicacion DESC
    """)
    @ResultMap("pasantiaResult")
    List<Pasantia> findByModalidad(@Param("modalidad") String modalidad);

    /**
     * Buscar pasantías con filtros dinámicos.
     * Se implementa en XML para mayor flexibilidad.
     */
    List<Pasantia> findWithFilters(PasantiaFiltroDTO filtro);

    /**
     * Contar total de pasantías con filtros.
     */
    Long countWithFilters(PasantiaFiltroDTO filtro);

    /**
     * Insertar una nueva pasantía.
     */
    @Insert("""
        INSERT INTO Pasantia (
            titulo, puesto_a_cubrir, ciudad, modalidad,
            asignacion_estimulo, cantidad_de_pasantes,
            fecha_publicacion, fecha_caducidad, estado,
            email_contacto, conocimientos, otros_requisitos, beneficios, id_empresa
        ) VALUES (
            #{titulo}, #{puestoACubrir}, #{ciudad}, #{modalidad},
            #{asignacionEstimulo}, #{cantidadDePasantes},
            #{fechaPublicacion}, #{fechaCaducidad}, #{estado},
            #{emailContacto}, #{conocimientos}, #{otrosRequisitos}, #{beneficios}, #{empresa.idEmpresa}
        )
    """)
    @Options(useGeneratedKeys = true, keyProperty = "idPasantia", keyColumn = "id_pasantia")
    void insert(Pasantia pasantia);

    /**
     * Actualizar una pasantía existente.
     */
    @Update("""
        UPDATE Pasantia SET
            titulo = #{titulo},
            puesto_a_cubrir = #{puestoACubrir},
            ciudad = #{ciudad},
            modalidad = #{modalidad},
            asignacion_estimulo = #{asignacionEstimulo},
            cantidad_de_pasantes = #{cantidadDePasantes},
            fecha_publicacion = #{fechaPublicacion},
            fecha_caducidad = #{fechaCaducidad},
            estado = #{estado},
            email_contacto = #{emailContacto},
            conocimientos = #{conocimientos},
            otros_requisitos = #{otrosRequisitos},
            beneficios = #{beneficios}
        WHERE id_pasantia = #{idPasantia}
    """)
    void update(Pasantia pasantia);

    /**
     * Actualizar solo el estado de una pasantía.
     */
    @Update("""
        UPDATE Pasantia 
        SET estado = #{estado}
        WHERE id_pasantia = #{idPasantia}
    """)
    void updateEstado(@Param("idPasantia") Integer idPasantia, @Param("estado") String estado);

    /**
     * Eliminar una pasantía (no recomendado, mejor actualizar estado).
     */
    @Delete("DELETE FROM Pasantia WHERE id_pasantia = #{id}")
    void delete(@Param("id") Integer id);

    /**
     * Verificar si existe una pasantía.
     */
    @Select("SELECT COUNT(*) > 0 FROM Pasantia WHERE id_pasantia = #{id}")
    boolean existsById(@Param("id") Integer id);

    /**
     * Obtener carreras asociadas a una pasantía.
     */
    @Select("""
        SELECT c.id_carrera AS id, c.nombre
        FROM Carrera c
        INNER JOIN Pasantia_Carrera pc ON c.id_carrera = pc.id_carrera
        WHERE pc.id_pasantia = #{idPasantia}
    """)
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "nombre", column = "nombre")
    })
    List<Carrera> findCarrerasByPasantiaId(@Param("idPasantia") Integer idPasantia);

    /**
     * Asociar una carrera a una pasantía.
     */
    @Insert("""
        INSERT INTO Pasantia_Carrera (id_pasantia, id_carrera)
        VALUES (#{idPasantia}, #{idCarrera})
    """)
    void insertPasantiaCarrera(@Param("idPasantia") Integer idPasantia, 
                                @Param("idCarrera") Integer idCarrera);

    /**
     * Eliminar todas las carreras asociadas a una pasantía.
     */
    @Delete("DELETE FROM Pasantia_Carrera WHERE id_pasantia = #{idPasantia}")
    void deleteAllCarrerasByPasantiaId(@Param("idPasantia") Integer idPasantia);
    
    /**
     * Eliminar todas las carreras asociadas a una pasantía (alias).
     */
    @Delete("DELETE FROM Pasantia_Carrera WHERE id_pasantia = #{idPasantia}")
    void deletePasantiaCarrerasByPasantiaId(@Param("idPasantia") Integer idPasantia);
    
    /**
     * Buscar pasantías por empresa ID.
     */
    @Select("""
        SELECT 
            p.id_pasantia AS idPasantia,
            p.titulo,
            p.puesto_a_cubrir AS puestoACubrir,
            p.ciudad,
            p.modalidad,
            p.asignacion_estimulo AS asignacionEstimulo,
            p.cantidad_de_pasantes AS cantidadDePasantes,
            p.fecha_publicacion AS fechaPublicacion,
            p.fecha_caducidad AS fechaCaducidad,
            p.estado,
            p.email_contacto AS emailContacto,
            p.id_empresa AS idEmpresa
        FROM Pasantia p
        WHERE p.id_empresa = #{empresaId}
        ORDER BY p.fecha_publicacion DESC
    """)
    List<Pasantia> findByEmpresaId(@Param("empresaId") Integer empresaId);
    
    /**
     * Buscar pasantías por carrera ID.
     */
    @Select("""
        SELECT 
            p.id_pasantia AS idPasantia,
            p.titulo,
            p.puesto_a_cubrir AS puestoACubrir,
            p.ciudad,
            p.modalidad,
            p.asignacion_estimulo AS asignacionEstimulo,
            p.cantidad_de_pasantes AS cantidadDePasantes,
            p.fecha_publicacion AS fechaPublicacion,
            p.fecha_caducidad AS fechaCaducidad,
            p.estado,
            p.email_contacto AS emailContacto,
            p.id_empresa AS idEmpresa
        FROM Pasantia p
        INNER JOIN Pasantia_Carrera pc ON p.id_pasantia = pc.id_pasantia
        WHERE pc.id_carrera = #{carreraId}
        ORDER BY p.fecha_publicacion DESC
    """)
    List<Pasantia> findByCarreraId(@Param("carreraId") Integer carreraId);
    
    /**
     * Buscar pasantías por estado.
     */
    @Select("""
        SELECT 
            p.id_pasantia AS idPasantia,
            p.titulo,
            p.puesto_a_cubrir AS puestoACubrir,
            p.ciudad,
            p.modalidad,
            p.asignacion_estimulo AS asignacionEstimulo,
            p.cantidad_de_pasantes AS cantidadDePasantes,
            p.fecha_publicacion AS fechaPublicacion,
            p.fecha_caducidad AS fechaCaducidad,
            p.estado,
            p.email_contacto AS emailContacto,
            p.id_empresa AS idEmpresa
        FROM Pasantia p
        WHERE p.estado = #{estado}
        ORDER BY p.fecha_publicacion DESC
    """)
    List<Pasantia> findByEstado(@Param("estado") String estado);

    /**
     * Contar postulaciones de una pasantía.
     */
    @Select("""
        SELECT COUNT(*) 
        FROM Postulacion 
        WHERE id_pasantia = #{idPasantia}
    """)
    Integer countPostulaciones(@Param("idPasantia") Integer idPasantia);

    /**
     * Contar postulaciones por estado.
     */
    @Select("""
        SELECT COUNT(*) 
        FROM Postulacion 
        WHERE id_pasantia = #{idPasantia}
        AND estado = #{estado}
    """)
    Integer countPostulacionesByEstado(@Param("idPasantia") Integer idPasantia, 
                                        @Param("estado") String estado);
}

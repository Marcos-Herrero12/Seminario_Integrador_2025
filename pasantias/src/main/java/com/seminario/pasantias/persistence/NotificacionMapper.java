package com.seminario.pasantias.persistence;

import com.seminario.pasantias.entity.Notificacion;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface NotificacionMapper {

    @Select("SELECT * FROM Notificacion WHERE id_usuario = #{idUsuario} ORDER BY fecha DESC")
    @Results({
        @Result(property = "idNotificacion", column = "id_notificacion"),
        @Result(property = "idUsuario", column = "id_usuario")
    })
    List<Notificacion> findByUsuarioId(@Param("idUsuario") Integer idUsuario);

    @Select("SELECT COUNT(*) FROM Notificacion WHERE id_usuario = #{idUsuario} AND leida = FALSE")
    Integer countUnreadByUsuarioId(@Param("idUsuario") Integer idUsuario);

    @Insert("INSERT INTO Notificacion (mensaje, fecha, leida, id_usuario) VALUES (#{mensaje}, #{fecha}, #{leida}, #{idUsuario})")
    @Options(useGeneratedKeys = true, keyProperty = "idNotificacion", keyColumn = "id_notificacion")
    void insert(Notificacion notificacion);

    @Update("UPDATE Notificacion SET leida = TRUE WHERE id_notificacion = #{idNotificacion}")
    void markAsRead(@Param("idNotificacion") Integer idNotificacion);

    @Update("UPDATE Notificacion SET leida = TRUE WHERE id_usuario = #{idUsuario}")
    void markAllAsRead(@Param("idUsuario") Integer idUsuario);
}

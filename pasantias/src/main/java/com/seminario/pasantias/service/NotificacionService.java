package com.seminario.pasantias.service;

import com.seminario.pasantias.entity.Notificacion;
import com.seminario.pasantias.persistence.NotificacionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class NotificacionService {

    @Autowired
    private NotificacionMapper notificacionMapper;

    public void crearNotificacion(Integer idUsuario, String mensaje) {
        Notificacion notificacion = new Notificacion();
        notificacion.setIdUsuario(idUsuario);
        notificacion.setMensaje(mensaje);
        notificacion.setFecha(LocalDateTime.now());
        notificacion.setLeida(false);
        notificacionMapper.insert(notificacion);
    }

    public List<Notificacion> obtenerNotificaciones(Integer idUsuario) {
        return notificacionMapper.findByUsuarioId(idUsuario);
    }

    public Integer contarNoLeidas(Integer idUsuario) {
        return notificacionMapper.countUnreadByUsuarioId(idUsuario);
    }

    public void marcarComoLeida(Integer idNotificacion) {
        notificacionMapper.markAsRead(idNotificacion);
    }

    public void marcarTodasComoLeidas(Integer idUsuario) {
        notificacionMapper.markAllAsRead(idUsuario);
    }
}

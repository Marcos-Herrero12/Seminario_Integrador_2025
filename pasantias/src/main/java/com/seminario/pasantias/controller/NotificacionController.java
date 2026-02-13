package com.seminario.pasantias.controller;

import com.seminario.pasantias.entity.Notificacion;
import com.seminario.pasantias.service.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notificaciones")
public class NotificacionController {

    @Autowired
    private NotificacionService notificacionService;

    @GetMapping("/{idUsuario}")
    public ResponseEntity<List<Notificacion>> obtenerNotificaciones(@PathVariable Integer idUsuario) {
        return ResponseEntity.ok(notificacionService.obtenerNotificaciones(idUsuario));
    }

    @GetMapping("/no-leidas/{idUsuario}")
    public ResponseEntity<Integer> contarNoLeidas(@PathVariable Integer idUsuario) {
        return ResponseEntity.ok(notificacionService.contarNoLeidas(idUsuario));
    }

    @PutMapping("/marcar-leida/{idNotificacion}")
    public ResponseEntity<Void> marcarComoLeida(@PathVariable Integer idNotificacion) {
        notificacionService.marcarComoLeida(idNotificacion);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/marcar-todas-leidas/{idUsuario}")
    public ResponseEntity<Void> marcarTodasComoLeidas(@PathVariable Integer idUsuario) {
        notificacionService.marcarTodasComoLeidas(idUsuario);
        return ResponseEntity.ok().build();
    }
}

package com.seminario.pasantias.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {
    private Integer idNotificacion;
    private String mensaje;
    private LocalDateTime fecha;
    private Boolean leida;
    private Integer idUsuario;
}

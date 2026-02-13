package com.seminario.pasantias.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(request -> {
                var corsConfiguration = new org.springframework.web.cors.CorsConfiguration();
                corsConfiguration.setAllowedOriginPatterns(java.util.List.of("*"));
                corsConfiguration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                corsConfiguration.setAllowedHeaders(java.util.List.of("*"));
                corsConfiguration.setAllowCredentials(true);
                return corsConfiguration;
            }))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/usuarios/registrarUsuario").permitAll()
                .requestMatchers("/estudiantes/crearEstudiante").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/notificaciones/**").permitAll()
                // Endpoints de usuarios: solo administradores (después de los públicos)
                .requestMatchers("/usuarios/**").hasRole("ADMINISTRADOR")
                // Endpoints de empresas: GET público, resto protegido
                .requestMatchers(HttpMethod.GET, "/empresas", "/empresas/consultarEmpresas").permitAll()
                .requestMatchers("/empresas/**").hasAnyRole("ADMINISTRADOR", "EMPRESA")
                // Endpoints de pasantías: GET público para listar todas, publicadas y ver detalles, resto protegido
                .requestMatchers(HttpMethod.GET, "/pasantias").permitAll()
                .requestMatchers(HttpMethod.GET, "/pasantias/publicadas").permitAll()
                .requestMatchers(HttpMethod.GET, "/pasantias/*").permitAll()
                // Endpoints de carreras: GET público, resto protegido
                .requestMatchers(HttpMethod.GET, "/carreras/consultarCarrera").permitAll()
                .requestMatchers(HttpMethod.GET, "/carreras/listarCarreras").permitAll()
                .requestMatchers("/carreras/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/roles/**").hasRole("ADMINISTRADOR")
                // Estudiantes endpoints
                .requestMatchers("/estudiantes/**").hasAnyRole("ADMINISTRADOR", "ESTUDIANTE")
                // Pasantías endpoints: buscar y ver públicas es público, resto se controla con @PreAuthorize
                .requestMatchers(HttpMethod.GET, "/api/pasantias/publicadas").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/pasantias/buscar").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/pasantias/{id}").permitAll()
                .requestMatchers("/api/pasantias/**").hasAnyRole("ADMINISTRADOR", "EMPRESA")
                // Endpoint para registrar pasantía: requiere autenticación y validación de empresa
                .requestMatchers(HttpMethod.POST, "/pasantias/registrar").hasAnyRole("ADMINISTRADOR", "EMPRESA")
                // Endpoint para aprobar pasantía: solo ADMINISTRADOR
                .requestMatchers(HttpMethod.PUT, "/pasantias/*/aprobar").hasRole("ADMINISTRADOR")
                // Endpoint para finalizar pasantía: solo ADMINISTRADOR
                .requestMatchers(HttpMethod.PUT, "/pasantias/*/finalizar").hasRole("ADMINISTRADOR")
                // OPTIONS requests para CORS preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                //POSTULACIONES ADMINISTRADOR O ESTUDIANTES
                .requestMatchers("/postulaciones/registrarPostulacion").hasAnyRole("ADMINISTRADOR", "ESTUDIANTE")
                .requestMatchers("/postulaciones/consultarPostulaciones").hasAnyRole("ADMINISTRADOR", "ESTUDIANTE")
                // Cualquier otra solicitud requiere autenticación
                .requestMatchers("/cvs/**").hasAnyRole("ADMINISTRADOR", "ESTUDIANTE")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
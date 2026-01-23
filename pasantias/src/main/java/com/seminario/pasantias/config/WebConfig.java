package com.seminario.pasantias.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class WebConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@org.springframework.lang.NonNull CorsRegistry registry) {
                // Get allowed origins from environment variable or use defaults for development
                String allowedOriginsEnv = System.getenv("CORS_ALLOWED_ORIGINS");
                String[] allowedOrigins;
                
                if (allowedOriginsEnv != null && !allowedOriginsEnv.isEmpty()) {
                    // Split by comma and trim whitespace
                    allowedOrigins = allowedOriginsEnv.split(",");
                    for (int i = 0; i < allowedOrigins.length; i++) {
                        allowedOrigins[i] = allowedOrigins[i].trim();
                    }
                } else {
                    // Default to localhost for development
                    allowedOrigins = new String[]{
                        "http://localhost:3000", 
                        "http://localhost.localdomain:3000",
                        "http://localhost:5173", 
                        "http://localhost.localdomain:5173"
                    };
                }
                
                registry.addMapping("/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .exposedHeaders("Authorization", "Content-Type", "Accept")
                        .maxAge(3600);
            }
            
            @Override
            public void extendMessageConverters(@org.springframework.lang.NonNull List<HttpMessageConverter<?>> converters) {
                // Asegurar que todos los converters usen UTF-8
                for (HttpMessageConverter<?> converter : converters) {
                    if (converter instanceof StringHttpMessageConverter) {
                        StringHttpMessageConverter stringConverter = (StringHttpMessageConverter) converter;
                        java.nio.charset.Charset currentCharset = stringConverter.getDefaultCharset();
                        if (currentCharset == null || !StandardCharsets.UTF_8.equals(currentCharset)) {
                            converters.remove(converter);
                            StringHttpMessageConverter utf8Converter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
                            utf8Converter.setWriteAcceptCharset(false);
                            converters.add(utf8Converter);
                            break;
                        }
                    } else if (converter instanceof MappingJackson2HttpMessageConverter) {
                        MappingJackson2HttpMessageConverter jsonConverter = (MappingJackson2HttpMessageConverter) converter;
                        java.nio.charset.Charset currentCharset = jsonConverter.getDefaultCharset();
                        if (currentCharset == null || !StandardCharsets.UTF_8.equals(currentCharset)) {
                            jsonConverter.setDefaultCharset(StandardCharsets.UTF_8);
                        }
                    }
                }
            }
        };
    }
}
package com.solace.ep.asyncapi.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origin}")
    private String allowedOrigin;

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry
            .addMapping("/**")
            .allowedOrigins(allowedOrigin)
            .allowedMethods("GET", "POST");
    }
}

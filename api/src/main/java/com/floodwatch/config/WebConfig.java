/*
 * File:    WebConfig.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Allows only the FloodWatch frontend origins to call the API from a browser.
 */


package com.floodwatch.config;

import com.floodwatch.security.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * Sets up CORS so only the React app (localhost in development, Vercel later) can call /api.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Configuration
public class WebConfig implements WebMvcConfigurer {


    private final AppProperties props;
    private final AuthInterceptor authInterceptor;

    /**
     * Creates the web configuration.
     *
     * @param props           application settings holding the allowed origins
     * @param authInterceptor checks the login token on endpoints marked with RequiresRole
     */


    public WebConfig
    (
        AppProperties props,
        AuthInterceptor authInterceptor
    ) {
        this.props = props;
        this.authInterceptor = authInterceptor;
    }

    /**
     * Registers the allowed origins and HTTP methods for every /api endpoint.
     *
     * @param registry Spring's CORS registry
     */


    @Override
    public void addCorsMappings
    (
        CorsRegistry registry
    ) {
        registry.addMapping("/api/**")
                .allowedOrigins(props.cors().allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE");
    }

    /**
     * Runs the login check before every /api endpoint. It does nothing for endpoints without RequiresRole.
     *
     * @param registry Spring's interceptor registry
     */


    @Override
    public void addInterceptors
    (
        InterceptorRegistry registry
    ) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
    }
}
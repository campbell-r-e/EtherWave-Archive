package com.hamradio.logbook.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration
 * Note: CORS is configured in SecurityConfig to avoid conflicts
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    // CORS configuration removed - handled by SecurityConfig
}

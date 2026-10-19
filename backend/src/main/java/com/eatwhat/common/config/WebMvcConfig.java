package com.eatwhat.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web MVC 配置：跨域、静态资源（上传目录映射）。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${eatwhat.upload.path:./data/upload}")
    private String uploadPath;

    @Value("${eatwhat.upload.url-prefix:/uploads}")
    private String urlPrefix;

    @Value("${eatwhat.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOrigins.split(",");
        for (int i = 0; i < origins.length; i++) {
            origins[i] = origins[i].trim();
        }
        registry.addMapping("/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-Trace-Id")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path path = Paths.get(uploadPath).toAbsolutePath().normalize();
        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations("file:" + path + "/");
    }
}

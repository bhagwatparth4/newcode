package com.cafe.backend.config;

import com.cafe.backend.service.storage.LocalImageStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/*
 * Only for STORAGE_PROVIDER=local: serves saved photos at /uploads/...
 * (read-only; files can only be added through the Admin upload API).
 */
@Configuration
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalUploadsWebConfig implements WebMvcConfigurer {

    private final LocalImageStorage localImageStorage;

    public LocalUploadsWebConfig(LocalImageStorage localImageStorage) {
        this.localImageStorage = localImageStorage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(localImageStorage.getRootDirectory().toUri().toString());
    }
}

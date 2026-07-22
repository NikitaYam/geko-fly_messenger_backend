package com.geckofly.messenger.config;

import jakarta.servlet.MultipartConfigElement;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Ручная конфигурация multipart вместо spring.servlet.multipart.* в yml.
 *
 * Причина: относительный путь location в yml Tomcat разрешает относительно
 * СВОЕЙ временной папки (/tmp/tomcat.../...), а не рабочего каталога приложения.
 * Из-за этого входящий файл ложился на другой том, и transferTo делал копирование
 * 5 ГБ вместо мгновенного rename. Здесь задаём АБСОЛЮТНЫЙ путь на томе хранилища
 * (baseDir/tmp = /app/storage/tmp) — тогда rename в storage/files работает.
 *
 * Определение своего MultipartConfigElement отключает автонастройку из properties,
 * поэтому лимиты размера тоже задаём здесь (из StorageProperties).
 */
@Configuration
@RequiredArgsConstructor
public class MultipartConfig {

    private final StorageProperties storageProperties;

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        Path tmp = Paths.get(storageProperties.getBaseDir())
                .toAbsolutePath().normalize()
                .resolve("tmp");

        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setLocation(tmp.toString());
        factory.setMaxFileSize(storageProperties.getMaxFileSize());
        factory.setMaxRequestSize(storageProperties.getMaxFileSize());
        // 0 — никогда не буферизовать в память: 5 ГБ обязаны идти на диск.
        factory.setFileSizeThreshold(DataSize.ofBytes(0));
        return factory.createMultipartConfig();
    }
}

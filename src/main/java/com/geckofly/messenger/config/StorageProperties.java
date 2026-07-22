package com.geckofly.messenger.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

/**
 * Настройки файлового хранилища (префикс messenger.storage в application.yml).
 * Числа и пути живут здесь, а не константами в контроллерах — правило 4
 * соглашений чистого кода (BACKEND_REWORK_PLAN.md).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "messenger.storage")
public class StorageProperties {

    /** Корень хранилища. Внутри: files/, avatars/, tmp/. Должен совпадать с multipart.location в yml. */
    private String baseDir = "storage";

    /**
     * Неприкосновенный резерв диска. Загрузка отклоняется (507), если свободного
     * места меньше — чтобы переполнение не убило Postgres и систему.
     */
    private DataSize minFreeSpace = DataSize.ofGigabytes(10);
}

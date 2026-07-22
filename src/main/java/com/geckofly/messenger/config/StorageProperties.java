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

    /** Корень хранилища. Внутри: files/, avatars/, tmp/. */
    private String baseDir = "storage";

    /**
     * Неприкосновенный резерв диска. Загрузка отклоняется (507), если ПОСЛЕ приёма
     * файла свободного места станет меньше — чтобы переполнение не убило Postgres.
     * 1 ГБ: диск VPS всего 15 ГБ, 10 ГБ блокировали бы загрузки уже при 33% занятости.
     */
    private DataSize minFreeSpace = DataSize.ofGigabytes(1);

    /** Максимальный размер одного файла. Используется в MultipartConfig. */
    private DataSize maxFileSize = DataSize.ofGigabytes(5);
}

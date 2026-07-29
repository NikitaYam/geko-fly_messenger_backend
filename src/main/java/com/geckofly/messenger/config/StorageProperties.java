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
     * Диск VPS всего 15 ГБ, поэтому резерв небольшой.
     */
    private DataSize minFreeSpace = DataSize.ofMegabytes(512);

    /** Максимальный размер одного файла. Используется в MultipartConfig. */
    private DataSize maxFileSize = DataSize.ofGigabytes(5);

    /** Сколько дней файл ждёт получателей, которые так и не скачали, до принудительной очистки. */
    private int retentionDays = 7;

    /** Порог аварийной очистки: если свободно меньше — sweep удаляет самые старые файлы независимо от доставки. */
    private DataSize emergencyFreeSpace = DataSize.ofGigabytes(1);
}

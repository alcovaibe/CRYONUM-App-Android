# CRYONUM для Android

Kotlin/Jetpack Compose, Android 8.0+ (API 26), namespace/applicationId `com.cryonum`.
Приложение содержит обычный и инженерный калькулятор, работу с подстановками,
историю, OCR двух строк подстановки, 12 загружаемых PDF-лекций, настройки темы,
языка, PIN/биометрии и аналитики. Расписание остаётся незавершённым экраном.

Аудит начат от `master2`, SHA `3f8220cce6b167a6dc1013e7c544ac13bc14edae`.
**Публикация пока заблокирована:** в `ProductionContentKeys` нет доверенного
публичного ключа манифеста. Успешная локальная сборка не устраняет этот блокер.

## Сборка

Нужны JDK 21 с `javac`, Android SDK Platform 37.0, Build Tools 36.0.0,
Gradle 9.6.1 через wrapper, доступ к Google Maven/Maven Central/Gradle Plugin Portal.
Укажите SDK в незакоммиченном `local.properties` (`sdk.dir=...`).

Для локального аудита без Firebase-конфигурации:

```sh
sh gradlew -PlocalAudit=true :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
sh gradlew -PlocalAudit=true :app:assembleRelease :app:lintRelease
sh gradlew -PlocalAudit=true :app:assembleDebugAndroidTest
```

`localAudit` меняет ID на `com.cryonum.audit`, не применяет Google Services/Crashlytics
Gradle-плагины и не инициализирует отправку Firebase в AnalyticsManager.
Это отдельный проверочный вариант, **не производственный релиз**. Библиотеки Firebase
остаются в зависимостях; работа SDK с реальным проектом проверяется отдельно.

Обычная сборка требует собственного `app/google-services.json` для `com.cryonum`.
Для производственной подписи используйте локальный `key.properties` или уже
поддерживаемые `ANDROID_KEYSTORE_PATH`, `ANDROID_STORE_PASSWORD`, `ANDROID_KEY_ALIAS`,
`ANDROID_KEY_PASSWORD`. Не передавайте приватные ключи в чат и не добавляйте их в Git.
Без signing configuration release APK остаётся неподписанным. Не запускайте публикацию
или загрузку mapping в Firebase до отдельного разрешения владельца.

## Документация

- [Архитектура](docs/ARCHITECTURE.md): компоненты, навигация, потоки и границы доверия.
- [Калькулятор](docs/CALCULATOR.md): грамматика, числовые режимы, проектные решения, пределы.
- [Данные и приватность](docs/DATA_AND_PRIVACY.md): хранилища, миграции, Analytics/Crashlytics.
- [Контент](docs/R2_CONTENT_SETUP.md): подписи, URL, загрузки, офлайн и действия владельца.
- [Проверки и безопасность](docs/TESTING_AND_SECURITY.md): команды, модель угроз и невыполненные сценарии.
- [Производительность](docs/PERFORMANCE.md): ограничения реализации и методика измерений на 1/4 ГБ.
- [Отчёт](docs/audit/REPORT.md), [находки](docs/audit/FINDINGS.md), [реестр файлов](docs/audit/coverage.csv).
- [Подготовка релиза](docs/RELEASE.md): блокеры и порядок дальнейшей проверки.

Зависимость mXparser заменена вычислительным модулем приложения и Apache Commons Math
для приближённых комплексных функций. Лицензия Apache 2.0 и NOTICE сохранены
в `docs/third-party/commons-math3/`. Это калькулятор ограниченного языка, не CAS.

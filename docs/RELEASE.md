# Подготовка публикации

## Блокеры

- В ProductionContentKeys нет доверенного ключа. Production-манифест заявляет content-2026-01,
  но подпись нельзя доверенно проверить. Нужен публичный EC SubjectPublicKeyInfo и независимое
  подтверждение владельцем, что это ключ его offline signer. Приватный ключ не передавать.
- Нет app/google-services.json для нового applicationId com.cryonum и проверки реальной
  инициализации Analytics/Crashlytics. localAudit=true намеренно использует com.cryonum.audit.
- Нет запуска на Android: PIN/биометрия/жизненный цикл, визуальная регрессия, OCR, PDF, WorkManager,
  API26 и ARMv7/ARM64 требуют проверки на устройствах. Инструментальные тесты только скомпилированы.
- Production-подпись, обновление предыдущей установки, Play Console/Data safety/политика и требования
  магазина в этом аудите не проверены. Версия и target не заменяют проверку готовности публикации.

## Переименование

Namespace/applicationId production — com.cryonum; локальная сборка — com.cryonum.audit.
FileProvider authority строится из applicationId. Старый путь icy_content сохранён только для
миграции. Не менять applicationId существующей опубликованной записи без решения владельца:
Android считает это другим приложением. Владелец сообщил, что после переименования публикации
ещё не было; доступ к Play Console не использовался.

Проверить перед публикацией: package в Firebase-конфигурации и сертификаты, signingConfig,
app label/icon/четыре перевода, домены и Telegram, публичную политику с независимым Crashlytics,
чистую установку offline и принятие политики, версию/versionCode, список разрешений итогового
production merged manifest и R8 mapping. Расписание остаётся пустой заглушкой; не рекламировать
готовое расписание. Экран партнёров больше не скрывается на эмуляторе/первые 48 часов release.

## Команды

```sh
sh gradlew -PlocalAudit=true :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
sh gradlew -PlocalAudit=true :app:assembleRelease :app:lintRelease :app:assembleDebugAndroidTest
# Только при подключённом изолированном устройстве:
sh gradlew -PlocalAudit=true :app:connectedDebugAndroidTest
# После настройки production-конфигурации владельцем:
sh gradlew :app:verifyReleaseSigning :app:assembleRelease :app:lintRelease
```

Локальный release без настроек подписи — unsigned. Не загружать его в магазин.
Google Services и Crashlytics Gradle plugins отключены только в явном localAudit; production
не получает скрытого fallback. Логи сборки содержат предупреждения, а не обещание полной чистоты.

Изменения не отправлены. SHA проверенного кода и история коммитов фиксируются в отчёте;
окончательный SHA документационного коммита сообщается отдельно, чтобы не создавать
самоссылочный хеш внутри коммита. git push / PR / workflows не выполнялись.

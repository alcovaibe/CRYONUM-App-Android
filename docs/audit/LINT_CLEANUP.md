# Точечная доработка Lint

Основа: master2, 2916f41fb13d622ac8aa756a2a919d85bb471ed0.
Изменения по отдельному запросу владельца:

- Убрана проверка SDK>=26 при создании notification channel: minSdk уже26; канал продолжает создаваться.
- mipmap-anydpi-v26 перенесён в mipmap-anydpi. Оба launcher XML побайтово совпадают с исходными.
- Сохранение calc_prefs_v2 использует androidx.core.content.edit с прежней асинхронной apply-семантикой.
- Commons Math3.6.1 и ExifInterface1.4.1 перенесены в libs.versions.toml без изменения версий.
- OkHttp и MockWebServer согласованно обновлены с5.4.0 до5.5.0. Официальный проект: https://github.com/lysine-dev/okhttp.

Проверка в той же локальной среде JDK21/Gradle9.6.1/SDK37:
`sh gradlew -PlocalAudit=true -Pkotlin.incremental=false :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease :app:lintRelease`.
Лог: logs/lint-cleanup-build.log. Новый результат Lint и unit-тестов: logs/lint-cleanup-results.json.
Исходные отчёты аудита сохранены как исторические снимки прежнего коммита.

Инструментальные проверки на устройстве и production Firebase/подпись этим изменением не проверяются.

Результат: debug/release собраны, 86 unit-тестов прошли; по112 предупреждений Lint, ошибок нет.
ObsoleteSdkInt, UseKtx, UseTomlInstead и предупреждение о MockWebServer5.4.0 исчезли.
Первый запуск остановлен после длительного отсутствия прогресса; следующие выявили
несогласованное инкрементальное состояние упаковки/classes.dex и старый APK.
Промежуточные неудачные логи сохранены. После удаления только incremental/packageDebug,
incremental/packageRelease и outputs/apk/debug, outputs/apk/release повторная команда
с --offline завершилась BUILD SUCCESSFUL. Компиляция/тесты/защита не отключались.

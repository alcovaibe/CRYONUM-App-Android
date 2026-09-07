# Проверки, модель угроз и воспроизведение

## Границы модели

| Субъект/сбой | Возможности | Проверка и предел |
|---|---|---|
| Обычное чужое приложение | exported Intent, выданный content URI | Merged manifest проверен; свои экраны non-exported, launcher публичен. FileProvider non-exported, только camera cache. Межприложенческий сценарий на устройстве не запускался |
| Временный доступ к разблокированному телефону | UI, возврат из фона, попытки PIN | Исправлены fail-open/счётчик/часы по коду; SecurityAuditTests подготовлен, не исполнен |
| Враждебный серверный ответ | Байты HTTP, редирект/Range/метаданные | JVM MockWebServer и verifier tests. Production только обычные GET; атаки не проводились |
| Повреждение файлов/завершение процесса | Частичный файл, JSON, потеря ключа | Часть покрыта unit/статикой; реальные kill/reboot/disk-full не выполнены |
| Root/debugger/модифицированный APK | Чтение private storage, вмешательство в процесс | Отдельная модель; экранный PIN не обещает защиты данных на диске. Anti-root не добавлялся |

## Локальные воспроизведения

`tools/audit/reproduce_baseline.py` извлекает CalculatorEngine прямо из базового Git-коммита,
убирает только вызовы android.util.Log для JVM-компиляции и запускает настоящий mXparser 6.1.1.
Лог показывает дефекты модуля, недопустимых символов, процентов, степеней, тригонометрии и точности.
Новые CalculatorEngineTests проверяют соответствующие корректные результаты и отказы.

`tools/audit/reproduce_integer_overflow.py` компилирует исходный SignedContentManifestVerifier
с текущим тестом. Он ожидает ровно два провала: schemaVersion=4294967297 и revision=
18446744073709551617 переполняли узкий целочисленный getter Gson. В тесте payload подписан
изолированным тестовым ключом, не ключом владельца. Это **не подделка ECDSA**. После строгого
разбора диапазона все 14 verifier tests проходят, включая повреждённую подпись и неизвестный ключ.

CancellableHttpTest использует MockWebServer с задержкой заголовков: cancel coroutine должен
закрыть Call за ограниченное время. Тест проверяет флаг cancelled, а не просто завершение Job.
Существующие ContentHttpValidatorTests/Integrity/URL/Links тесты также исполнены; перечень и
числа находятся в JUnit XML. Это не равно полному скачиванию с убийством процесса на Android.

SecurityAuditTests сохраняет/восстанавливает DataStore в localAudit-приложении, выполняет
30 конкурентных неверных PIN, проверяет блокировку правильного PIN и повреждённые encrypted
флаги. Он только скомпилирован: воспроизведение на Keystore этой среды отсутствует.

## Независимые математические эталоны

- 1500 случайных рациональных выражений: Python fractions.Fraction, seed 20260907.
- 500 комплексных функций: Python cmath, относительная погрешность 1e-11, абсолютная 1e-12.
- Все 5040 перестановок 7 элементов: чётность через независимый разбор циклов, сравнение с
  подсчётом инверсий. Дубликаты, чужие/повторяющиеся значения и многозначные элементы отдельно.
- Точные известные значения, сохранение Ans и canonical round-trip, ошибки и ресурсные пределы.

Два алгоритма binary64 могут разделять численные ограничения. Это содержательная дифференциальная
проверка, не доказательство всех областей/ветвей. Property tests не заменяют сценарии кнопок UI.

## Содержательность существующих тестов

Число86 включает существующий ExampleUnitTest с обычным арифметическим assert: это проверка
тестового окружения, не функциональности приложения. Часть старых тестов настроек напрямую
читает SharedPreferences и не проверяет весь UI/lifecycle. policyDownloadOfferIsVisible и
policyUpdateOfferIsVisible используют один и тот же helper и не доказывают разные серверные
сценарии. Старый тест отсутствия предложения лекций содержал условие-константу; теперь он
компонует ReferenceMaterialScreen, но передаёт заранее заданный showLecturePrompt=false.
Следовательно, он проверяет отображение state, а не решение ViewModel о предложении загрузки.
Эти ограничения не скрыты увеличением общего счётчика. Добавленные проверки ядра, verifier,
отмены, миграции и permutation проверяют конкретные результаты/отказы и имеют отдельные доказательства.

## Как запустить

Основной путь — Gradle-команды README. Для чистого JVM без Android:

```sh
python3 tools/audit/run_host_tests.py --gradle-lib /path/gradle-9.6.1/lib --test-libs /path/test-libs
python3 tools/audit/differential.py --gradle-lib /path/gradle-9.6.1/lib --test-libs /path/test-libs
python3 tools/audit/reproduce_baseline.py --gradle-lib /path/gradle-9.6.1/lib --test-libs /path/test-libs
python3 tools/audit/reproduce_integer_overflow.py --gradle-lib /path/gradle-9.6.1/lib --test-libs /path/test-libs
python3 -m unittest discover -s tools/tests -v
python3 tools/audit/inventory.py
```

test-libs: junit 4.13.2, hamcrest-core 1.3, commons-math3 3.6.1, gson 2.14.0; mXparser 6.1.1
нужен только для baseline. Скрипт overflow дополнительно использует OkHttp из Gradle cache.
В контейнере host-компиляция использовала embedded Kotlin 2.3.21 Gradle и JRE17;
реальная Android-компиляция — проектный Kotlin 2.4.0 и JDK21. Никаких Android stubs для
выдуманного успешного запуска не создавалось. Скрипты подписания/миграции R2 предварительно
читались; `--apply`, upload, реальные операции подписи приватным ключом не запускались.

## Обязательная следующая матрица устройства

API26 ARMv7/1 ГБ и актуальный ARM64/4 ГБ: холодный старт без сети; rotate/process death во всех
экранах; PIN после background, reboot, смены часов, повреждения Keystore; biometric cancellation,
lockout и отсутствие сенсора; каждый видимый calculator key, повторное равно/MR/M± после Ans;
OCR EXIF, отказ camera, gallery cancellation, нет Play Services; все PDF offline и очистка cache;
mock загрузки с 200/206/416, ETag changed, HTML, wrong MIME/hash/length, медленной сетью,
restart/cancel/двумя workers, disk-full, kill и reboot. На рабочий сервер эти сценарии не направлять.

## Google/OWASP: применимость, а не сертификат

Сверено 2026-09-07: MASVS STORAGE/AUTH/NETWORK/PRIVACY и MASTG применимы как методика;
полная сертификация/покрытие всех контролей не заявляются. Certificate pinning не объявлен
автоматически обязательным. Public verification key не является секретом. Аккаунтов/рекламы
в приложении не найдено; account deletion/ad consent flows неприменимы в текущем продукте.

48dp touch target, TalkBack labels, контраст, крупный шрифт и адаптивная компоновка — рекомендации
качества, не доказательство прохождения Google Play. CalcButton получил минимальную высоту48dp,
но отсутствует визуальная проверка width/contrast/focus на конкретном устройстве. Lint отдельно
сохраняет fixed orientation/unused resources и другие предупреждения. Четыре набора string keys
проверены на совпадение, качество перевода человеком не оценено. Правила Play Console и итоговый
Data safety необходимо сверить владельцу для фактической поставки.

Источники:
- https://developer.android.com/docs/quality-guidelines/core-app-quality
- https://developer.android.com/docs/quality-guidelines/adaptive-app-quality
- https://developer.android.com/guide/topics/ui/accessibility/principles
- https://developer.android.com/build/releases/agp-9-3-0-release-notes
- https://mas.owasp.org/MASVS/
- https://mas.owasp.org/checklists/

Полного автоматического CVE/SBOM-аудита транзитивных зависимостей нет. Проверены фиксированные
версии, удалён mXparser из runtime, сохранены LICENSE/NOTICE Apache Commons Math. Отсутствие
публичных advisory на странице проекта не означает отсутствие всех уязвимостей. CVE других
Apache Commons-пакетов не приписываются commons-math3.

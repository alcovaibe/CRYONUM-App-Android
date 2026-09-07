# Карта приложения

## Точки входа и экраны

`SplashActivity` — launcher. После проверки сессии открывается `ActivitySubstitutions`
или `ActivitySecurity` в MODE_UNLOCK. Навигация между экранами осуществляется Intent,
а UI каждого экрана рисуется Compose. Единого Navigation Compose графа нет.

| Группа | Реализация | Состояние и зависимости |
|---|---|---|
| Калькулятор | ActivityCalculator → CalculatorViewModel → CalculatorEngine | ViewModel переживает поворот; вычисления Dispatchers.Default; персистентность v2 в SharedPreferences |
| Подстановки | ActivitySubstitutions → PermutationInput/PermutationUtils → ActivityResult | Валидация обеих строк; n до 1000; подсчёт O(n²), максимум 499500 инверсий |
| OCR | ImagePicker → ML Kit Latin recognizer | Ограниченная временная копия URI, уменьшение bitmap, EXIF; результат требует подтверждения пользователем |
| История | ActivityHistory/HistoryManager/HistoryItem | Общий JSON, максимум 50 записей; калькулятор хранит режим углов и прежний Ans |
| Лекции | ActivityReferenceMaterial/ContentDownloadViewModel | 12 LectureId, подписанный манифест, DownloadRepository |
| Политика | PolicyManager → ActivityAbout → verifiedLocalFile → ActivityPdfViewer | Все пути открытия политики проходят проверку контента; версия берётся из проверенного файла |
| PDF | PdfViewerScreen/PdfPage/PdfRenderBudget | LazyColumn; renderer/descriptor закрываются use; bitmap только у компонуемых страниц |
| PIN/биометрия | SecurityManager, SecurityUtils, AppLockObserver, ActivitySecurity | DataStore/Keystore, сессия в памяти процесса, единый mutex проверки PIN |
| Настройки | ActivitySettings/Language/ThemeSelection/Analytics | LocaleManager, ThemeManager, AnalyticsManager |
| Информация | About/HelpCenter/Partners | Ссылки через ACTION_VIEW; Telegram из RemoteLinksRepository |
| Расписание | ActivitySchedule/ScheduleScreen | Заглушка; в аудит не добавлялась реализация расписания |

## Потоки и фон

`MyApplicationInfo` инициализирует язык/тему, локальную диагностику и разрешённые SDK,
регистрирует глобальную проверку блокировки и планирует WorkManager.
ContentDependencies — singleton только с applicationContext. OkHttp запрещает редиректы,
куки и автоматические повторы; явные повторы организует Worker. Контент скачивается
последовательно, разные наборы сериализуются собственными mutex. Работа с манифестом
сериализована отдельно. restart ждёт освобождения mutex перед удалением частичных файлов.

Корутины ViewModel отменяются с ViewModel. CancellableHttp связывает отмену с Call.cancel
на всём протяжении чтения ответа. localSummary и хеширование выполняются на IO.
PolicyUpdateWorker планируется еженедельно с окном, а не как точный будильник.

## Границы доверия

1. Экранная блокировка скрывает UI, но не шифрует историю и PDF.
2. Чужой content URI не считается безопасным изображением: 20 МиБ входа, декодирование
   вне UI, уменьшение длинной стороны до приблизительно 2048 пикселей.
3. Манифест должен пройти ECDSA, схему, ревизию и whitelist путей; PDF — размер, magic и SHA-256.
4. TLS-конфигурация Telegram ограничивает хост и форму пути, но не доказывает принадлежность
   нового канала владельцу. Политика изменения канала остаётся ответственностью сайта.
5. Analytics, Crashlytics и локальная БД — отдельные каналы; см. DATA_AND_PRIVACY.

## Остаточный технический долг

Большинство остальных Activity по-прежнему хранит UI-состояние самостоятельно и блокирует
ориентацию. Не выполнена массовая миграция всей навигации. Два legacy-класса
OrientationManager и ZoomableTwoDScrollView не имеют найденных обращений в main-коде;
их наличие не означает использование активным PDF-viewer. Стили и ресурсы содержат
неиспользуемые элементы (зафиксированы Lint), без необоснованного массового удаления.

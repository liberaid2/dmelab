# Desktop: Kotlin/JVM и UniFFI

Основная цель — macOS Apple Silicon (`aarch64-apple-darwin`). Desktop-сборка
предоставляет существующий API MS43 и native serial через
`dmelab-transport-serial`. Приложение и Compose UI собираются отдельно.

## Сборка

```sh
bash scripts/build-desktop.sh
# Явный target:
bash scripts/build-desktop.sh aarch64-apple-darwin
# Несколько уже установленных targets:
bash scripts/build-desktop.sh aarch64-apple-darwin x86_64-apple-darwin
```

По умолчанию используется host выбранного Rust toolchain. Нужны Rust 1.95,
установленные targets и соответствующие linker/SDK. Скрипт ничего не
устанавливает; Gradle, Android SDK и NDK не требуются. Поддерживаются
`RUSTUP_TOOLCHAIN` и `CARGO_TARGET_DIR`. Генератор всегда выполняется на host.

Cargo feature `dmelab-ffi/desktop` включает native serial API. Без неё остаётся
Android API с транспортом приложения, без serial-зависимости у FFI. Serialport
уже используется CLI; его конфигурация не включает фоновые потоки.

Результат в `dist/desktop/`:

- `kotlin/dev/coldcat/dmelab/ffi/dmelab_ffi.kt` — Kotlin/JVM API;
- `resources/darwin-aarch64/libdmelab_ffi.dylib` — библиотека macOS ARM;
- `BUILDINFO` — toolchain, UniFFI и targets последней сборки.

Для macOS Intel используется `darwin-x86-64`. Скрипт также содержит пути
Linux GNU ARM64/x86_64 и Windows x86_64/ARM64 при наличии targets и linker.
На этих платформах сборка и работа пока не проверялись. После изменения FFI
пересобирайте все поставляемые targets: старые resource-каталоги сохраняются.

## Подключение к приложению

Подключите `kotlin/` как JVM-исходники, `resources/` как JVM resources и обычную
зависимость `net.java.dev.jna:jna:5.19.1` (JAR). JNA выбирает нативную библиотеку
по ОС и архитектуре работающей JVM и загружает её из ресурсов, включая JAR.
Для разработки можно использовать
`-Djna.library.path=/absolute/path/to/resources/darwin-aarch64`.
ARM-библиотеке нужна ARM JVM; Intel JVM использует Intel-библиотеку.

Пакет — `dev.coldcat.dmelab.ffi`. Конфигурация генерации находится в
`crates/dmelab-ffi/uniffi-desktop.toml`: `android = false`,
`android_cleaner = false`. Это global override для аргумента `--config`;
основной `uniffi.toml` сохраняет настройки Android.

В Kotlin Multiplatform размещайте bindings и JNA в desktop/JVM source set.
Android source set использует отдельную генерацию `dist/android/` и JNA AAR.
Не включайте обе генерации с одинаковым пакетом в одну компиляцию.
`commonMain` обращается к Kotlin-контрактам приложения, а Java/JNA остаются
в платформенной реализации. Сгенерированные файлы не редактируйте;
поставляйте bindings и нативную библиотеку одной сборки вместе.

## Serial API

```kotlin
import dev.coldcat.dmelab.ffi.*

val ports = listSerialPorts()
val portName = "/dev/cu.usbserial-FT1234" // Выбор принадлежит приложению.
Ms43Client.openSerial(portName = portName, timeoutMs = 1000u).use { client ->
    val ecu = client.identify(timeoutMs = 2000u)
    val dtcs = client.readDtcs(timeoutMs = 2000u)
}
```

`listSerialPorts()` возвращает отсортированные `SerialPortInfo`: `name`,
`usbVid`, `usbPid`, `serialNumber`, `description`. USB-метаданные необязательны;
VID/PID не используются как обязательный фильтр. На macOS возможны обе формы
`cu`/`tty`; предпочтительна `cu`. Автоматического выбора порта в FFI нет.

`openSerial` проверяет непустое имя и timeout 1–60000 мс, открывает порт на
9600 8E1, без flow control, с выключенными RTS/DTR. При открытии диагностические
пакеты не отправляются. Каждая операция принимает собственный timeout.
Ошибочные аргументы дают `DmeException.InvalidArgument`; ошибки перечисления,
открытия и настройки — `DmeException.Transport` с причиной ОС.

Клиент владеет OS handle между операциями. `use`/`close()` освобождает его,
когда закончены активные вызовы и больше нет Rust-ссылок. Перед закрытием
остановите stream и дождитесь возврата: закрытие не прерывает операцию.
Один клиент на физическое соединение. После исполнительного теста явно
вызовите `endDiagnosticControl()`; закрытие порта эту команду не заменяет.

## Выполнение

Доступны идентификация, DTC, пять live-блоков, stream, flash и управление
приводами. Rust выполняет вызовы и callbacks синхронно, потоков не создаёт.
Приложение вызывает операции вне UI-потока.

Перед каждой протокольной операцией UART возвращается к настройке 9600 8E1, а MS43
идентифицируется. Для stream это происходит один раз на весь вызов.
Flash сохраняет согласованный разгон и возврат 9600. Контракты `Busy`,
кооперативной остановки и `NeedsReconnect` совпадают с Android API.
`NeedsReconnect` блокирует последующие настройки и команды: восстановите
ECU и адаптер на 9600, затем создайте новый клиент.

FFI хранит `Arc<Mutex<SerialTransport>>` для безопасного владения объектом
UniFFI, а протокольную Session создаёт локально. Общий Transport не получает
ограничение `Send`. Native IO-ошибки передаются напрямую в протокол,
с сохранением `io::ErrorKind` для решений о восстановлении.

## Проверка

На macOS ARM собраны `.dylib` и Kotlin/JVM bindings. Со стандартным JNA 5.19.1
проверены загрузка библиотеки из ресурсов JAR без ручного пути, версия,
перечисление портов, валидация аргументов и ошибка отсутствующего порта.
Подготовленные сценарии Kotlin-транспорта проверяют в desktop-сборке
идентификацию, live, восстановление, `Busy`, остановку и управление приводами.
Артефакты — `dist/desktop-check/`. Обмен с реальным serial-адаптером и ECU
этими проверками не подтверждается.

Пройдены rustfmt, Clippy с feature `desktop` и без неё, 23 существующих
Rust-теста. Android-библиотеки пересобраны для четырёх ABI, свежие Kotlin
bindings отдельно скомпилированы. Проверено отсутствие desktop serial API
в Android-генерации и serialport в зависимостях Android FFI. Новые постоянные
тесты не добавлялись в соответствии с фризом.

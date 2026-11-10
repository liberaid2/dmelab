# Android: Kotlin bindings без Gradle

Библиотека владеет генерацией Kotlin API через UniFFI 0.32.2. Пакет —
`dev.coldcat.dmelab.ffi`, нативная библиотека — `libdmelab_ffi.so`.
Gradle для сборки этих артефактов не требуется; упаковка приложения выполняется отдельно.

Desktop Kotlin/JVM собирается отдельно: [desktop bindings](desktop-bindings.md).
Android сохраняет конструктор `Ms43Client(transport)` и собственный USB-драйвер.
Не включайте Cargo feature `desktop` при Android-сборке; стандартный
`build-android.sh` её не использует. Нативные serial API и JNA JAR принадлежат
desktop-варианту, Android использует JNA AAR и Android cleaner.
При добавлении desktop пересобраны четыре Android ABI, Kotlin отдельно
скомпилирован; отсутствие desktop serial API и зависимости serialport
в Android FFI проверено. Код приложения этим изменением не затрагивается.

## Сборка

Требуются Rust 1.95, `cargo-ndk`, Android NDK и установленные Rust targets для
выбранных ABI. NDK задаётся через `ANDROID_NDK_HOME` либо обнаруживается
`cargo-ndk` в Android SDK. Скрипт ничего не устанавливает.

```sh
bash scripts/build-android.sh
# Выбор ABI:
bash scripts/build-android.sh arm64-v8a armeabi-v7a x86_64 x86
# Если Android targets установлены в другом toolchain той же версии:
RUSTUP_TOOLCHAIN=stable bash scripts/build-android.sh
```

По умолчанию собираются `arm64-v8a` и `x86_64`, минимальный Android API — 23.
Результат в `dist/android/`:

- `kotlin/dev/coldcat/dmelab/ffi/dmelab_ffi.kt` — сгенерированный API;
- `jniLibs/<abi>/libdmelab_ffi.so` — нативные библиотеки;
- `BUILDINFO` — версии сборщика, UniFFI, API и выбранные ABI.

Не редактируйте сгенерированный Kotlin. Исходники API находятся в
`crates/dmelab-ffi/src/`, конфигурация — в `crates/dmelab-ffi/uniffi.toml`.
Поставляйте Kotlin и `.so` одной сборки вместе. Повторный запуск заменяет
артефакты выбранных ABI; ранее собранные другие ABI остаются в каталоге.

Release использует `opt-level = "z"`, полный LTO и одну единицу генерации кода.
Скрипт сначала генерирует Kotlin из библиотеки с таблицей символов, затем
выполняет `llvm-strip --strip-unneeded` для копий в `dist/android/jniLibs/`.
Исходные артефакты Cargo сохраняют метаданные для повторной генерации.
Не включайте глобальный `strip = "symbols"`: генератор UniFFI не сможет
прочитать обработанный ELF. `panic = "unwind"` сохраняет перехват паник UniFFI.
Результаты сравнения: [размер release-сборок](release-size-comparison.md).

Для Android включён `android_cleaner = true`: с API 34 используется
SystemCleaner, на более ранних версиях — JNA Cleaner. Генерируемые
bindings сохраняют совместимость с минимальным API без отключения
проверки Android Lint NewApi. Настройка находится в `uniffi.toml`.

## Подключение к приложению

Скопируйте `kotlin/` в каталог Kotlin-исходников, `jniLibs/` — в каталог нативных
библиотек приложения. Нужна Android-версия JNA (`net.java.dev.jna:jna:5.19.1@aar`);
зависимость и упаковка относятся к сборке приложения. Сохраняйте классы JNA и
сгенерированный пакет при минификации (правила keep для `com.sun.jna.**` и
`dev.coldcat.dmelab.ffi.**`). UniFFI не предоставляет Android USB-драйвер.

Приложение получает USB-разрешение, открывает FTDI через Android USB Host и
реализует `KlineTransport`. Затем создаёт `Ms43Client(transport)` и вызывает
`identify(1000u)`, `readDtcs(1000u)`, `readLiveData(2000u)`, `readFlash(...)` или `writeFlash(...)`. Доступны также
`validateImage(...)`, `libraryVersion()`, `ProgressListener` и типизированные ошибки.
`readDtcs` возвращает полный список `Dtc` либо ошибку страницы с индексом;
массивы `data` и `raw` сохраняют исходные байты. Подробности: [чтение DTC](ms43-dtc.md).
`clearDtcs(timeoutMs)` однократно отправляет очистку и возвращает `Unit` только
после подтверждения ECU. Отказ/потеря подтверждения — `DmeException.DtcClear`;
автоматических повторов и чтения после очистки нет. Для проверки текущего списка
вызовите `readDtcs` отдельно. Отмена однократных и flash-операций пока не
реализована; непрерывный `streamLiveData` останавливается через
`LiveDataStreamControl.stop()`.

`readLiveData` возвращает типизированный `LiveData` с основными показаниями,
`intakeVanos`/`exhaustVanos` (`actualDeg`, `targetDeg`, `referenceDeg`) и
`disaCommandActive`. Выполняются идентификация и три запроса данных. Ошибка
любого блока — `DmeException.LiveDataRead`; частичный результат не выдаётся.
В `LiveData` доступны все 28 величин основного блока: в том числе
`ambientPressureHpa`, `batteryVoltageV`, `terminal15VoltageV`,
`idleSpeedIntegratorPct`, `idleAirControlDutyPct`, четыре
`oxygenSensorHeater…DutyPct` и отдельные `intakeCamshaftAngleDeg` /
`exhaustCamshaftAngleDeg` из `0B 03`. `intakeVanos`/`exhaustVanos` сохраняют
все три угла из `0B 90`. Новые поля доступны в однократном и непрерывном API.
Изменение состава record требует заново сгенерированных Kotlin bindings
и нативной библиотеки одной сборки; прежние bindings с новой `.so` не смешивайте.
Единицы и ограничения: [текущие параметры MS43](ms43-live-data.md).

## Непрерывная live-сессия

Для устойчивого опроса вызывайте `streamLiveData` один раз вне UI-потока:

```kotlin
val control = LiveDataStreamControl()
val listener = object : LiveDataListener {
    override fun onSample(data: LiveData) {
        // Опубликовать полный свежий набор; callback должен завершаться быстро.
    }
    override fun onRecovering(failure: LiveDataStreamFailure) {
        // Записать block, details и consecutiveFailures; опрос продолжается.
    }
}
client.streamLiveData(timeoutMs = 2000u, control = control, listener = listener)
```

`control.stop()` вызывается из UI/другого потока приложения либо из listener.
Флаг не требует захвата занятого клиента; Rust потоков не создаёт. Для следующего
запуска создайте новый control: остановка необратима. Заранее остановленный
control завершает вызов без настройки транспорта. Одной отмены Kotlin coroutine
недостаточно для прерывания блокирующего Rust-вызова — приложение должно передать
сигнал `stop()`.

Порт настраивается и ECU идентифицируется один раз на весь вызов. Rust удерживает
сессию и последовательно читает три блока на 9600 8E1 с паузами минимум 100 мс.
Ошибки пакетов передаются в `onRecovering` и не завершают сессию независимо от их
количества. Неудачный замер отбрасывается; восстановительная пауза растёт от
100 до 1000 мс. `block = null` означает сбой начальной идентификации.
Отдельные потерянные замеры не превращаются в старые данные с новой отметкой времени.

Явное отключение транспорта, невременные ошибки его настройки/очистки и
неподдерживаемый ECU возвращают ошибку. Исключение
listener также завершает stream с `DmeException.Transport`; оно не игнорируется.
На всём протяжении вызова другие операции этого клиента возвращают `Busy`.
Остановка проверяется между блоками/замерами и во время backoff; текущий
транспортный callback не прерывается, первоначальная идентификация завершается
до очередной проверки. Время остановки зависит от соблюдения таймаутов адаптером.

Однократный `readLiveData` сохранён. Старый Android-цикл повторных `readLiveData`
не получает восстановление автоматически: приложению нужно использовать новый
`streamLiveData`. Эта задача изменяет Rust/UniFFI и CLI, без изменения Android UI.

## Выбор live-блоков

Старые `readLiveData` / `streamLiveData` и record `LiveData` сохранены для всех
трёх блоков. Новые методы принимают `LiveDataSelection` с пятью флагами. Новые `pedal` и `roughRunning` по умолчанию `false`:

```kotlin
val selection = LiveDataSelection(
    measurements = true, // 0B 03: все 28 показаний.
    vanos = false,       // 0B 90: actual / target / reference обоих валов.
    digital = false,     // 0B 04: команда DISA.
    pedal = true,        // 0B 92: каналы педали/дросселя и адаптации.
    roughRunning = true, // 2B 05: неравномерность и счётчики пропусков.
)
val data = client.readLiveDataSelected(selection, timeoutMs = 2000u)
val rpm = data.measurements?.engineSpeedRpm
// data.vanos == null, data.digital == null.

val control = LiveDataStreamControl()
client.streamLiveDataSelected(selection, 2000u, control,
    object : SelectedLiveDataListener {
        override fun onSample(data: SelectedLiveData) {
            // Опубликовать data.measurements; callback выполняется синхронно.
        }
        override fun onRecovering(failure: LiveDataStreamFailure) {
            // Ошибка выбранного блока; опрос продолжается.
        }
    })
```

Любая непустая комбинация допустима. Все `false` возвращают
`DmeException.InvalidArgument` до `configureInitial` и обмена с ECU, в том числе
для заранее остановленного control. Непрочитанные группы в `SelectedLiveData`
равны `null`; выбранный блок с нулевыми значениями остаётся ненулевым record.
`measurements` содержит `LiveMeasurements`, `vanos` — `LiveVanos` с `intake` и
`exhaust`, `digital` — `LiveDigital` с `disaCommandActive`.

Дополнительно доступны `pedal: LivePedal?` (13 величин) и
`roughRunning: LiveRoughRunning?` (`status`, шесть `cylinders`).
Каждый цилиндр содержит `cylinder`, `roughness`, `misfireCount`; записи
отсортированы 1–6. Статус системной проверки передаётся как есть, её запуск
автоматически не выполняется; наличие значений не подтверждает свежесть.

Порядок выбранных блоков фиксирован:
measurements → vanos → digital → pedal → roughRunning.
Выбор действует на весь вызов stream; чтобы его сменить, остановите вызов и
запустите новый с новым control. Ошибка отбрасывает весь выбранный замер;
восстановление начинает выбранный набор заново. Настройка 9600, идентификация
один раз, пауза минимум 100 мс между запросами, `Busy`, остановка и обработка
ошибок listener соответствуют полному stream. Приложение подключает новые
методы самостоятельно; Android UI этим изменением не обновляется.

## Диагностическое управление

```kotlin
client.setDisa(enabled = true, timeoutMs = 2000u)
client.setDisa(enabled = false, timeoutMs = 2000u)
client.setCoolingFan(dutyPct = 50.0, timeoutMs = 2000u)
client.endDiagnosticControl(timeoutMs = 2000u)
```

Это однократные синхронные команды на 9600 с идентификацией. Процент вентилятора
должен быть конечным числом 0–100; иначе `InvalidArgument` до настройки USB.
Успех означает подтверждение ECU. Автоматических повторов, завершения при
закрытии объекта и фонового keepalive нет; завершайте управление явно.
Ошибка воздействия/завершения — `DmeException.DiagnosticControl` с
`operation`, `status: UByte?` и `details`. Статус присутствует при явном отказе;
таймаут/ошибка транспорта сохраняют `status = null`. Исходный отказ
идентификации возвращается до команды управления.

Перед воздействием или завершением остановите занятый live-stream и дождитесь
его возврата; иначе получите `Busy`. Приложение отвечает за UI и порядок
действий. [Пакеты и границы проверки](ms43-actuators.md).

`DmeException.LiveDataRead` и `LiveDataStreamFailure` теперь содержат
`command` вместе с `block`, чтобы различать `0B` и `2B`. У failure оба поля
равны `null` при ошибке идентификации. Record selection расширен двумя флагами
с Kotlin defaults `false`, поэтому прежние вызовы конструктора с тремя
аргументами сохраняются. Kotlin bindings и `.so` необходимо обновить вместе.

## Контракт транспорта и выполнения

- `configureInitial()` устанавливает 9600, 8E1, без flow control, RTS/DTR выключены.
  Он вызывается перед каждой операцией, для stream — один раз перед всем циклом.
  Соединением и его закрытием владеет приложение.
- `read(maxBytes, timeoutMs)` возвращает не больше запрошенного; частичные чтения
  допустимы. Сохраняйте лишние USB-байты для следующего чтения. Пустой результат
  означает таймаут. Отключение сообщайте через `TransportFailure.Disconnected`.
- `write(data, timeoutMs)` возвращает количество принятых байтов; частичная запись
  допустима. Данные должны поступать в UART без накопления до будущего `flush`.
- `setBaudRate()` должен поддерживать выбранную скорость, включая 125000;
  согласованием с ECU управляет Rust. `clearBuffers()` очищает входной и выходной буферы.
- Каждый callback обязан соблюдать переданный таймаут; настройки и очистка также
  должны завершаться за ограниченное время. Rust не может прервать зависший USB-вызов.
- Один клиент на физическое соединение. Все операции и callbacks синхронные,
  выполняются в вызывающем потоке. Rust не создаёт потоки; приложение вызывает API
  вне UI-потока. Служебная очистка JVM/JNA принадлежит среде приложения.
- Параллельный или повторный вход в тот же клиент возвращает `DmeException.Busy`.
  Прогресс обрабатывайте быстро; исключения listener игнорируются с записью в
`tracing`, чтобы сбой UI не прерывал запись flash.
- `DmeException.NeedsReconnect` требует восстановления ECU и адаптера на 9600 и создания
  нового клиента. Простого повторного открытия адаптера недостаточно, если ECU
  остался на повышенной скорости.

Чтение/запись используют разгон и возврат 9600, как Rust API и CLI. Bootloader
никогда не стирается и не записывается; остальные ограничения описаны в
[протоколе flash](ms43-flash.md). Сборка артефактов не подтверждает обмен с реальным
Android USB-адаптером и ECU; эти проверки выполняются на оборудовании отдельно.

## Выполненная автономная проверка

Собраны `.so` для `arm64-v8a` и `x86_64` с NDK 28.2.13676358; Kotlin скомпилирован
компилятором 2.2.10 с классами JNA 5.19.1 без запуска Gradle. На macOS через
JNA 5.17.0 выполнена идентификация с подготовленными ответами транспорта Kotlin
и частичными чтениями: проверены результат, типизированные ошибки и отклонение
повторного входа. Это проверка границы FFI, а не Android USB-драйвера или таймингов ECU.

Для `readDtcs` отдельно проверен вызов Kotlin → Rust → транспорт Kotlin:
автоматическая идентификация, две страницы, повтор после таймаута, first-wins,
передача data/raw и `DmeException.DtcRead` при четырёх неудачах второй страницы.
Android-артефакты с этим API пересобраны для обоих ABI.

Для `readLiveData` Android Kotlin скомпилирован с Android SDK 37.0 и JNA 5.19.1.
Отдельная JVM-генерация с отключённым Android cleaner использована для проверки
на macOS через JNA 5.17.0: проверены все поля, автоматическая идентификация,
три запроса данных и ошибка при коротком последнем ответе. Продуктовая настройка
`android_cleaner = true` сохранена. Нативные Android-библиотеки пересобраны для
обоих ABI; на Android-устройстве новый вызов пока не проверялся.

После расширения `LiveData` до всех 28 величин `0B 03` повторно выполнены
однократный и непрерывный вызовы Kotlin → UniFFI → Rust → транспорт Kotlin.
Проверены все новые поля, знаковые преобразования, сохранение трёх запросов
данных и отказ при коротком ответе. Kotlin скомпилирован для JVM и Android
без Gradle; нативные библиотеки пересобраны для четырёх ABI. Артефакты проверки
находятся в `dist/live-measurements-check/`. Проверка не использует реальный ECU.

Для `clearDtcs` проверены успешный `A0`, отказ `B0`, потеря подтверждения без
повторов и прекращение операции после обнаружения неподдерживаемого ECU.
Проверка выполнена через Kotlin → UniFFI → Rust с подготовленными ответами
транспорта. Android Kotlin скомпилирован, библиотеки обоих ABI пересобраны.
На реальном ECU очистка не выполнялась.

Для `streamLiveData` выполнен вызов Kotlin → UniFFI → Rust → транспорт Kotlin:
12 последовательных таймаутов/ошибок XOR не завершили опрос, затем получены два
полных замера. Проверены одна конфигурация и идентификация, callbacks с исходной
причиной, `Busy`, остановка из listener и заранее остановленный control, а также
завершение при отключении USB и ошибке listener. Kotlin bindings с новым API
скомпилированы для JVM и Android без Gradle; `.so` пересобраны для четырёх ABI.
Артефакты автономной проверки — `dist/live-stream-check/`. На Android-устройстве
с FTDI и реальным ECU непрерывный режим пока не проверялся.

Выбор блоков проверен через Kotlin → UniFFI → Rust с подготовленным транспортом:
семь комбинаций, отсутствие лишних команд и nullable-группы, восстановление
выбранной пары после ошибок каждого блока, остановка, `Busy` и отклонение
пустого выбора до настройки транспорта. Прежние `readLiveData` / `streamLiveData`
повторно прошли проверку. Kotlin скомпилирован для JVM и Android без Gradle,
`.so` пересобраны для четырёх ABI. Артефакты — `dist/live-selection-check/`;
обмена с реальным ECU в этой проверке нет.

Расширение `pedal` / `roughRunning` и команды `setDisa`, `setCoolingFan`,
`endDiagnosticControl` проверены вызовами Kotlin → UniFFI → Rust → транспорт
Kotlin. Проверены типы, номера цилиндров, восстановление stream с обоими
байтами команды, точные пакеты управления, ошибки статуса и потери ответа
без повторов, отклонение неправильного процента до настройки USB. Повторно
пройдены прежние проверки live API. Kotlin скомпилирован для JVM и Android
без Gradle; `.so` пересобраны для четырёх ABI. Артефакты —
`dist/diagnostics-check/`; физические приводы не проверялись.

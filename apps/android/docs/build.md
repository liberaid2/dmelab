# Настройка и сборка Android

Проект состоит из `:androidApp` (каталог `app/`), `:desktopApp`, `:shared` и `:ecu`.
`:shared` и `:ecu` используют Kotlin Multiplatform и Android-KMP Library plugin;
у них Android и JVM desktop targets. Общие контракты, модели, сценарии и Compose UI
находятся в `commonMain`, платформенные интеграции — в `androidMain`/`desktopMain`.
Этот документ описывает Android; запуск и упаковка macOS — в
[инструкции desktop](desktop.md), границы модулей — в
[мультиплатформенной архитектуре](multiplatform-architecture.md).

## Требования

- JDK 21 для Gradle; wrapper использует Gradle 9.4.1 и AGP 9.2.1.
- Kotlin Multiplatform 2.3.20 и Compose Multiplatform 1.10.3;
  версии плагинов и библиотек зафиксированы в каталоге Gradle.
- Android SDK Platform 37 и Build Tools 36.0.0. Путь SDK задаётся локально
  в `local.properties` через `sdk.dir` или стандартное окружение Android SDK.
- Артефакты UniFFI 0.32.2 в `dist/android/` общего проекта.

`compileSdk` — 37, поскольку текущие Core KTX и Lifecycle требуют API 37.
`targetSdk` остаётся 36, `minSdk` — 27. В APK включаются `arm64-v8a` и `x86_64`.

## Подготовка UniFFI

Из корня общего проекта, на два уровня выше Android:

```sh
bash scripts/build-android.sh
```

Для пересборки Rust нужны toolchain из `rust-toolchain.toml`, cargo-ndk,
Android NDK и Rust targets выбранных ABI. Подробности и контракт транспорта —
[Android bindings](../../../docs/android-bindings.md).

Модуль `:ecu` напрямую использует Kotlin из `../../dist/android/kotlin` и `.so`
из `../../dist/android/jniLibs` только для Android source set;
UniFFI Gradle-плагин не нужен. В `ecu/src/commonMain/kotlin/` находятся
собственные модели и контракты ECU; в `ecu/src/androidMain/kotlin/` —
UniFFI, USB, преобразование данных и Android-реализации этих контрактов.
Generated bindings, JNA и нативные классы недоступны общему коду фич.
Перед сборкой `:ecu:verifyDmelabBindings` проверяет наличие Kotlin,
BUILDINFO, обеих нативных библиотек и ожидаемую версию UniFFI. При отсутствии
артефактов сборка завершается с инструкцией их подготовки.

После изменения Rust API заново собирайте Kotlin и `.so` одной командой.
Не смешивайте артефакты разных сборок и не редактируйте сгенерированный Kotlin.
Gradle не запускает Rust-сборку автоматически. Имена пакета и библиотеки —
`dev.coldcat.dmelab.ffi` и `libdmelab_ffi.so`.

Подключён JNA 5.19.1 в Android AAR-формате. Правила сохранения классов JNA и
UniFFI находятся в `ecu/consumer-rules.pro`. AGP передаёт consumer rules
приложению при release-сборке с R8; debug остаётся без минификации.

## Зависимости и запуск приложения

Версии находятся в `gradle/libs.versions.toml`. Gradle скачивает зависимости
из Google Maven и Maven Central; JitPack разрешён только для
`com.github.mik3y:usb-serial-for-android`.

Koin 4.2.2 и Koin Compiler Plugin 1.2.1 работают с Kotlin 2.3.20. Compose Compiler
использует ту же версию Kotlin. В `:shared` и `:ecu` применены
`org.jetbrains.kotlin.multiplatform` и `com.android.kotlin.multiplatform.library`;
`:shared` также подключает `org.jetbrains.compose`. `:androidApp` сохраняет
Android Application plugin со встроенной поддержкой Kotlin.

KMP-библиотеки явно применяют `com.android.lint`: новый Android KMP-плагин
не создаёт задачи анализа без него. Используем `:shared:lint` и `:ecu:lint`,
а у приложения остаётся `:androidApp:lintDebug`. Host-test compilations
подключены к стандартному `commonTest`; новых тестовых файлов нет.
Общий код также используется JVM desktop target; его компиляция и упаковка
не требуют Android-генерации UniFFI. Android-сборка сохраняет свои bindings и JNA AAR.

Общий UI и логика находятся в `shared/src/commonMain/kotlin/`, строки и изображения —
в `shared/src/commonMain/composeResources/`. Android-контейнеры системного UI
и другие платформенные адаптеры библиотеки находятся в `shared/src/androidMain/kotlin/`.
Activity, manifest, сервис, файловые адаптеры и единственный DI-модуль остаются
в обычной Android-структуре `app/src/main/`.

`DmeLabApplication` запускает единственный `AppModule` с
`@ComponentScan("dev.coldcat.dmelab")`. Модуль предоставляет IO dispatcher
и один Preferences DataStore для `settings.preferences_pb`. Классы приложения
обнаруживаются по Koin Annotations во всех трёх модулях: compiler plugin
генерирует metadata в библиотеках, а ComponentScan в `:androidApp` собирает
единый граф. Отдельных Koin-модулей библиотек, KSP и ручного списка компонентов нет.

Voyager 1.0.1 открывает онбординг подключения и идентификации MS43. Подключены
AndroidX ViewModel/Compose Lifecycle, Coroutines 1.10.2, DataStore 1.2.1 и
USB Serial 3.11.0. `EcuLibrary.version()` вызывает UniFFI вне main-потока.
USB-разрешения, выбор адаптера и идентификация подключены через
`UsbAdapterMonitor`, `EcuConnectionCoordinator` и синхронный `UsbKlineTransport`.
Аппаратная совместимость требует отдельной проверки. Остальные операции ECU
подключены к общему исполнителю `:shared`. Для minSdk 27 генератор UniFFI использует
`android_cleaner = true`: Android SystemCleaner с API 34 и JNA Cleaner ниже.

## Сборка APK

Из `apps/android`:

```sh
./gradlew :androidApp:assembleDebug
```

Результат — `app/build/outputs/apk/debug/dme-lab-debug.apk`, подписанный debug-ключом.

Для release:

```sh
./gradlew :androidApp:assembleRelease
```

Результат — `app/build/outputs/apk/release/dme-lab-release.apk`.
Release-подпись пока не настроена: этот APK не подписан. Имена задаются через
`androidComponents` и `VariantOutput.outputFileName` в `app/build.gradle.kts`.

Для настройки проекта критерий успеха — собранный APK; ADB, устройство и эмулятор
не требуются. Сборка подтверждает компиляцию bindings и упаковку `.so`,
но не загрузку нативной библиотеки на устройстве или обмен с ECU.

Фриз на новые тесты сохраняется. Для дальнейших изменений кода порядок проверок
описан в [архитектуре](architecture.md#порядок-работы-агента).

Проверки после изменений модулей:

```sh
./gradlew :androidApp:assembleDebug :androidApp:assembleRelease \
  :androidApp:lintDebug :shared:lint :ecu:lint \
  :androidApp:testDebugUnitTest
```

Новые тесты не добавляются. Существующие JVM- и instrumentation-тесты остаются
в `app/src/test` и `app/src/androidTest`; установка — `:androidApp:installDebug`,
инструментальные проверки — `:androidApp:connectedDebugAndroidTest`.
Идентификатор приложения `dev.coldcat.dmelab`, manifest-компоненты, провайдер
логов и приватные каталоги файлов сохранены, поэтому перенос модулей сам по себе
не меняет адреса файлов или данные установленного приложения.

## R8 для release

В `app/build.gradle.kts` включены `isMinifyEnabled = true` и
`isShrinkResources = true`: удаление неиспользуемого кода и ресурсов,
оптимизация и обфускация. Для AGP 9.2.1 используем штатный DSL и
`proguard-android-optimize.txt`. Новый `optimization.enable` в этой версии
требует экспериментальный `android.r8.gradual.support`; этот флаг не включаем.
Правила ECU передаются приложению из `ecu/consumer-rules.pro` через
конфигурацию consumer keep rules Android-KMP библиотеки.

Используем стандартный full mode; `android.enableR8.fullMode=false` не задаём.
Оптимизированное удаление ресурсов в AGP 9 уже доступно без дополнительного
`android.r8.optimizedResourceShrinking=true`. Debug-настройки не меняем.

Перепаковка классов в один безымянный корневой пакет уже включена по умолчанию
начиная с AGP 9.1. Отдельный `-repackageclasses` не нужен; именованный пакет
добавил бы лишний префикс. В текущем mapping 272 класса приложения перенесены
в корневой пакет, например `EcuLibrary → ux`, `UsbKlineTransport → sn1`.
Исключения — 168 защищённых классов UniFFI и три компонента Android
(`Application`, `Activity`, `Service`), сохраняющие имена по keep-правилам.
Эти числа относятся к прежней сборке до разделения модулей; текущий результат
смотрите в mapping соответствующего APK. `-allowaccessmodification`
уже поступает из стандартного оптимизирующего файла; дополнительные
`-optimizationpasses`, `-optimizations`, `-overloadaggressively` R8 игнорирует.

Сохраняем `com.sun.jna.**` и `dev.coldcat.dmelab.ffi.**`: имена, JNI-методы,
поля структур и callbacks нужны для reflection и нативной регистрации.
Не добавляем `-keep` для всего приложения, Compose или Koin. Правила библиотек
поступают из их зависимостей; например, USB Serial сохраняет драйверы.
Уточнять защиту JNA/UniFFI можно только вместе с проверкой обмена на устройстве.

JNA содержит изолированные desktop-функции работы с AWT-окнами, которые
приложение и UniFFI не вызывают. Для отсутствующих в Android
`java.awt.Component`, `GraphicsEnvironment`, `HeadlessException`, `Window`
заданы четыре точечных `-dontwarn`. Других собственных исключений для
отсутствующих классов нет. Не используем глобальные `-ignorewarnings`, `-dontwarn **`,
`-dontoptimize` и `-dontobfuscate`.

В `app/build/outputs/mapping/release/mapping.txt` находится mapping для
расшифровки stack trace. AGP 9.2.1 в текущей конфигурации не создаёт отдельные
`configuration.txt` и `usage.txt`. При распространении каждой версии сохраняйте её
APK и соответствующий `mapping.txt`: файл следующей сборки не заменяет старый.
Оптимизация R8 относится к JVM-коду и Android-ресурсам; `.so` оптимизируются
в Rust-сборке отдельно. Успешная сборка не подтверждает работу JNI после
оптимизации: запуск приложения, идентификация и callbacks на устройстве
проверяются отдельно, без автоматического запуска записи прошивки или сброса DTC.

При включении R8 для текущего кода unsigned release APK уменьшился с
11 965 708 до 3 141 717 байт (11,4 → 3,0 МиБ, примерно −74 %).
Debug/release-сборки, Android Lint и существующий JVM-тест выполнены успешно;
работа оптимизированного APK на устройстве и с ЭБУ пока не проверена.

Основания: [руководство Android по R8](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization),
[глобальные опции и перепаковка классов](https://developer.android.com/topic/performance/app-optimization/global-options),
[исходники desktop-функций JNA 5.19.1](https://github.com/java-native-access/jna/blob/5.19.1/src/com/sun/jna/Native.java).

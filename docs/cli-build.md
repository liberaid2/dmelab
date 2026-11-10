# Сборки CLI для macOS, Linux и Windows

Сборка всех четырёх вариантов с macOS:

```sh
bash scripts/build-cli.sh
```

Скрипт собирает только `dmelab-cli`, без Android-приложения и UniFFI.
Использует release-профиль проекта и `Cargo.lock`, копирует бинарники в
`dist/cli/` и создаёт архивы для GitHub Releases в `dist/releases/`.
Сам скрипт зависимости не устанавливает и релиз не публикует.

## Инструменты

Проверенная конфигурация: Rust 1.95.0, Zig 0.16.0, cargo-zigbuild 0.22.3,
Apple Command Line Tools с macOS SDK и Python 3 для упаковки архивов.
Zig и cargo-zigbuild нужны для сборок Linux и Windows с Mac.
Зависимости библиотечного Rust API не изменяются.

Дополнительные targets устанавливаются отдельно от обычной установки CLI:

```sh
rustup target add --toolchain 1.95.0 \
  aarch64-apple-darwin x86_64-apple-darwin \
  x86_64-unknown-linux-musl x86_64-pc-windows-gnullvm
```

Если инструменты отсутствуют, установите Apple Command Line Tools через
`xcode-select --install`, [Zig 0.16.0](https://ziglang.org/download/0.16.0/),
а cargo-zigbuild — командой:

```sh
cargo install cargo-zigbuild --version 0.22.3 --locked
```

| Платформа | Target | Способ сборки | Бинарник |
| --- | --- | --- | --- |
| macOS Apple Silicon | `aarch64-apple-darwin` | Cargo + Apple SDK | `dist/cli/macos-arm64/dmelab` |
| macOS Intel | `x86_64-apple-darwin` | Cargo + Apple SDK | `dist/cli/macos-x86_64/dmelab` |
| Linux x64 | `x86_64-unknown-linux-musl` | cargo-zigbuild | `dist/cli/linux-x86_64/dmelab` |
| Windows x64 | `x86_64-pc-windows-gnullvm` | cargo-zigbuild | `dist/cli/windows-x86_64/dmelab.exe` |

Linux-бинарник статический: ему не нужна установленная glibc или musl.
Windows использует UCRT и системные DLL; поставлять рядом DLL от MinGW
не требуется. Для Windows выбран target GNU LLVM, совместимый с UCRT
из Zig 0.16. Для macOS используется установленный Apple SDK.

## Отдельные платформы и архивы

Можно передать любой непустой набор поддерживаемых targets:

```sh
bash scripts/build-cli.sh aarch64-apple-darwin x86_64-apple-darwin
bash scripts/build-cli.sh x86_64-unknown-linux-musl
bash scripts/build-cli.sh x86_64-pc-windows-gnullvm
```

Архивы содержат исполняемый файл без дополнительных каталогов:

```text
dist/releases/dmelab-v0.1.0-macos-arm64.tar.gz
dist/releases/dmelab-v0.1.0-macos-x86_64.tar.gz
dist/releases/dmelab-v0.1.0-linux-x86_64.tar.gz
dist/releases/dmelab-v0.1.0-windows-x86_64.zip
dist/releases/SHA256SUMS
```

Версия берётся из Cargo metadata. `SHA256SUMS` содержит контрольные суммы
архивов текущего запуска. При сборке только части платформ остальные архивы
не обновляются; полный комплект для релиза готовьте одним запуском без аргументов.
Повторная сборка заменяет соответствующие бинарники и архивы.

Поддерживается `CARGO_TARGET_DIR`. Кеши Zig и cargo-zigbuild по умолчанию
создаются внутри каталога сборки; явные настройки кешей сохраняются.
Команды можно выполнять офлайн, если зависимости уже загружены:

```sh
CARGO_NET_OFFLINE=true bash scripts/build-cli.sh
```

## Проверки

Форматирование, Clippy и существующие Rust-тесты запускаются отдельно:

```sh
cargo fmt --all --check
cargo clippy --workspace --all-targets -- -D warnings
cargo test --workspace
```

На Mac проверены запуск ARM64 и запуск Intel CLI через Rosetta.
Для Linux проверяется формат ELF и отсутствие динамических зависимостей;
для Windows — PE x64 и список импортируемых DLL. Эти проверки не заменяют
запуск на соответствующей ОС и проверку FTDI на автомобиле.

Автоматический выбор `usbserial` остаётся функцией macOS. На других ОС
передавайте порт явно, например `--port /dev/ttyUSB0` или `--port COM3`.

Основания: [cargo-zigbuild](https://github.com/rust-cross/cargo-zigbuild),
[Windows GNU LLVM / UCRT](https://doc.rust-lang.org/rustc/platform-support/windows-gnullvm.html).

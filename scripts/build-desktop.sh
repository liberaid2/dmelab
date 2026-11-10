#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

# Native host by default. Cross-builds require already installed targets/linkers.
# This script does not install toolchains, dependencies, or platform SDKs.
targets=("$@")
host=$(rustc -vV | sed -n 's/^host: //p')
if [[ $# -eq 0 ]]; then
    targets=("$host")
fi
installed=$(rustup target list --installed)
output="$PWD/dist/desktop"
artifact_root="${CARGO_TARGET_DIR:-$PWD/target}"
library_names=()
resource_prefixes=()

for target in "${targets[@]}"; do
    case "$target" in
        aarch64-apple-darwin) name=libdmelab_ffi.dylib; resource=darwin-aarch64 ;;
        x86_64-apple-darwin) name=libdmelab_ffi.dylib; resource=darwin-x86-64 ;;
        aarch64-unknown-linux-gnu) name=libdmelab_ffi.so; resource=linux-aarch64 ;;
        x86_64-unknown-linux-gnu) name=libdmelab_ffi.so; resource=linux-x86-64 ;;
        x86_64-pc-windows-msvc|x86_64-pc-windows-gnu) name=dmelab_ffi.dll; resource=win32-x86-64 ;;
        aarch64-pc-windows-msvc) name=dmelab_ffi.dll; resource=win32-aarch64 ;;
        *) echo "Unsupported desktop target: $target" >&2; exit 1 ;;
    esac
    if ! [[ "$installed" == *"$target"* ]]; then
        echo "Missing target $target in the selected Rust toolchain. Select an installed toolchain with RUSTUP_TOOLCHAIN or install the target explicitly." >&2
        exit 1
    fi
    library_names+=("$name")
    resource_prefixes+=("$resource")
done

for index in "${!targets[@]}"; do
    target="${targets[$index]}"
    cargo build --locked --release -p dmelab-ffi --lib --features desktop --target "$target"
    destination="$output/resources/${resource_prefixes[$index]}"
    mkdir -p "$destination"
    cp "$artifact_root/$target/release/${library_names[$index]}" "$destination/${library_names[$index]}"
done

cargo run --locked --target "$host" -p dmelab-ffi --features bindgen --bin dmelab-bindgen -- \
    generate "$output/resources/${resource_prefixes[0]}/${library_names[0]}" \
    --language kotlin --metadata-no-deps --config crates/dmelab-ffi/uniffi-desktop.toml \
    --out-dir "$output/kotlin" --no-format

cat > "$output/README.md" <<'DOC'
# dmelab desktop Kotlin/JVM bindings

Kotlin package: `dev.coldcat.dmelab.ffi`.
Compile `kotlin/` as JVM sources, and include `resources/` as JVM resources.
The application needs standard JNA (`net.java.dev.jna:jna:5.19.1`, a JAR).
JNA selects and loads the native library from its platform resource directory.
Alternatively, set `-Djna.library.path` to that directory.

Use `listSerialPorts()` and `Ms43Client.openSerial(portName, timeoutMs)`.
Close the client with Kotlin `use` after all its operations have returned.
The client owns the native serial handle. Do not create two clients per port.
All Rust calls are blocking; the application owns the calling thread.

Desktop and Android bindings have the same package but platform-specific
implementations. Put each in its platform source set, never both together.
Do not mix bindings/native libraries from different builds. Rebuild every
packaged target after changing the FFI API; old target directories are retained.
See docs/desktop-bindings.md for the complete build and API contract.
DOC
{
    rustc --version
    echo "UniFFI: 0.32.2"
    echo "Kotlin package: dev.coldcat.dmelab.ffi"
    echo "Cargo features: desktop"
    echo "Targets: ${targets[*]}"
    echo "JNA resources: ${resource_prefixes[*]}"
} > "$output/BUILDINFO"
echo "Desktop artifacts: $output"

#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

# Select an already installed toolchain with RUSTUP_TOOLCHAIN if necessary.
# No dependencies or targets are installed by this script.
abis=("$@")
if [[ $# -eq 0 ]]; then
    abis=(arm64-v8a x86_64)
fi
targets=()
installed=$(rustup target list --installed)
for abi in "${abis[@]}"; do
    case "$abi" in
        arm64-v8a) triple=aarch64-linux-android ;;
        armeabi-v7a) triple=armv7-linux-androideabi ;;
        x86) triple=i686-linux-android ;;
        x86_64) triple=x86_64-linux-android ;;
        *) echo "Unsupported Android ABI: $abi" >&2; exit 1 ;;
    esac
    if ! [[ "$installed" == *"$triple"* ]]; then
        echo "Missing target $triple in selected toolchain; select one with RUSTUP_TOOLCHAIN or install the target explicitly." >&2
        exit 1
    fi
    targets+=(-t "$abi")
done

cargo ndk --version
output="$PWD/dist/android"
mkdir -p "$output"
cargo ndk "${targets[@]}" -P 23 -o "$output/jniLibs" \
    build --locked --release -p dmelab-ffi --lib
cargo run --locked -p dmelab-ffi --features bindgen --bin dmelab-bindgen -- \
    generate "$output/jniLibs/${abis[0]}/libdmelab_ffi.so" \
    --language kotlin --metadata-no-deps --out-dir "$output/kotlin" --no-format

# Strip distribution copies only, after UniFFI has extracted metadata.
# cargo-ndk selects the same NDK and host toolchain as the build above.
(
    ndk_environment="$(mktemp)"
    trap 'rm -f "$ndk_environment"' EXIT
    cargo ndk-env -t "${abis[0]}" -P 23 > "$ndk_environment"
    source "$ndk_environment"
    strip_tool="${CLANG_PATH%/*}/llvm-strip"
    for abi in "${abis[@]}"; do
        "$strip_tool" --strip-unneeded "$output/jniLibs/$abi/libdmelab_ffi.so"
    done
)

cat > "$output/README.md" <<'EOF'
# dmelab Android bindings

Kotlin package: `dev.coldcat.dmelab.ffi`.
Use Kotlin sources in `kotlin/` together with native libraries in `jniLibs/`.
The application also needs JNA for Android: `net.java.dev.jna:jna:5.19.1@aar`.
Do not mix Kotlin and native libraries from different builds.

The application implements `KlineTransport` and owns USB permissions, the FTDI
driver, the connection, and the calling thread. All Rust operations are blocking.
See `docs/android-bindings.md` in the source repository for the full contract.
EOF
{
    rustc --version
    cargo ndk --version
    echo "UniFFI: 0.32.2"
    echo "Android API: 23"
    echo "Kotlin package: dev.coldcat.dmelab.ffi"
    echo "ABIs: ${abis[*]}"
    echo "Symbols: stripped after Kotlin generation (llvm-strip --strip-unneeded)"
} > "$output/BUILDINFO"
echo "Android artifacts: $output"

#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

# Dependencies are installed explicitly; this script only builds and packages.
targets=("$@")
if [[ $# -eq 0 ]]; then
    targets=(aarch64-apple-darwin x86_64-apple-darwin
             x86_64-unknown-linux-musl x86_64-pc-windows-gnullvm)
fi
installed=$(rustup target list --installed)
artifact_root="${CARGO_TARGET_DIR:-$PWD/target}"
if [[ "$artifact_root" != /* ]]; then
    artifact_root="$PWD/$artifact_root"
fi
export ZIG_GLOBAL_CACHE_DIR="${ZIG_GLOBAL_CACHE_DIR:-$artifact_root/zig-global-cache}"
export ZIG_LOCAL_CACHE_DIR="${ZIG_LOCAL_CACHE_DIR:-$artifact_root/zig-local-cache}"
export CARGO_ZIGBUILD_CACHE_DIR="${CARGO_ZIGBUILD_CACHE_DIR:-$artifact_root/zigbuild}"
platforms=()
needs_zig=false

command -v python3 >/dev/null
for target in "${targets[@]}"; do
    case "$target" in
        aarch64-apple-darwin) platform=macos-arm64 ;;
        x86_64-apple-darwin) platform=macos-x86_64 ;;
        x86_64-unknown-linux-musl) platform=linux-x86_64; needs_zig=true ;;
        x86_64-pc-windows-gnullvm) platform=windows-x86_64; needs_zig=true ;;
        *) echo "Unsupported CLI target: $target" >&2; exit 1 ;;
    esac
    if ! [[ "$installed" == *"$target"* ]]; then
        echo "Missing Rust target: $target; install it explicitly with rustup target add." >&2
        exit 1
    fi
    platforms+=("$platform")
done
if "$needs_zig"; then
    command -v zig >/dev/null
    command -v cargo-zigbuild >/dev/null
fi

for index in "${!targets[@]}"; do
    target="${targets[$index]}"
    binary=dmelab
    case "$target" in
        *-apple-darwin)
            cargo build --locked --release -p dmelab-cli --bin dmelab --target "$target"
            ;;
        *)
            cargo zigbuild --locked --release -p dmelab-cli --bin dmelab --target "$target"
            ;;
    esac
    if [[ "$target" == *-windows-* ]]; then binary=dmelab.exe; fi
    destination="$PWD/dist/cli/${platforms[$index]}"
    mkdir -p "$destination"
    cp "$artifact_root/$target/release/$binary" "$destination/$binary"
done

python3 - "${platforms[@]}" <<'PY'
from pathlib import Path
import hashlib
import json
import subprocess
import sys
import tarfile
import zipfile

metadata = json.loads(subprocess.check_output(
    ['cargo', 'metadata', '--locked', '--offline', '--no-deps', '--format-version', '1']))
version = next(p['version'] for p in metadata['packages'] if p['name'] == 'dmelab-cli')
output = Path('dist/releases')
output.mkdir(parents=True, exist_ok=True)
checksums = []
for platform in dict.fromkeys(sys.argv[1:]):
    binary = Path('dist/cli') / platform / ('dmelab.exe' if platform.startswith('windows-') else 'dmelab')
    archive = output / f'dmelab-v{version}-{platform}'
    if platform.startswith('windows-'):
        archive = Path(str(archive) + '.zip')
        with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as bundle:
            bundle.write(binary, binary.name)
    else:
        archive = Path(str(archive) + '.tar.gz')
        with tarfile.open(archive, 'w:gz') as bundle:
            bundle.add(binary, arcname=binary.name)
    checksums.append(f'{hashlib.sha256(archive.read_bytes()).hexdigest()}  {archive.name}')
    print(f'{binary}: {binary.stat().st_size} bytes; {archive}: {archive.stat().st_size} bytes')
(output / 'SHA256SUMS').write_text('\n'.join(checksums) + '\n')
PY

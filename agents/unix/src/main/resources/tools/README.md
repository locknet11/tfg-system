# Bundled Tools

Native tool binaries bundled with the agent so it can complete scan / fetch plan
steps without depending on tools being pre-installed on the target host.

`BundledToolProvisioner` extracts the subdirectory matching the running
`os.name`/`os.arch` at startup, sets the executable bit, and prepends the
extraction directory to `PATH` for every command the agent runs. Tools that are
not bundled for a given platform fall through to the host's own `PATH`.

## Directory Layout

```
tools/
├── linux-amd64/       # Linux x86_64 — PRIMARY DEPLOYMENT TARGET (fully self-contained)
│   ├── nmap           # host discovery (-sn) & service/version detection (-sV)
│   ├── rustscan       # fast TCP port sweep
│   ├── nc             # raw TCP connectivity (ncat)
│   └── curl           # remote file retrieval
└── darwin-arm64/      # macOS Apple Silicon — LOCAL DEV ONLY
    └── rustscan       # nmap / nc / curl come from the host on macOS (see note below)
```

## Bundled Versions & Provenance

| Platform     | Tool     | Version      | Source                                                                                          | License  |
|--------------|----------|--------------|-------------------------------------------------------------------------------------------------|----------|
| linux-amd64  | nmap     | 6.49BETA1    | https://github.com/andrew-d/static-binaries `binaries/linux/x86_64/nmap` (static)               | NPSL/GPLv2 |
| linux-amd64  | nc       | 6.49BETA1    | https://github.com/andrew-d/static-binaries `binaries/linux/x86_64/ncat` (static)               | NPSL/GPLv2 |
| linux-amd64  | rustscan | 2.4.1        | https://github.com/bee-san/RustScan/releases/download/2.4.1/x86_64-linux-rustscan.tar.gz.zip    | GPLv3    |
| linux-amd64  | curl     | 8.21.0       | https://github.com/stunnel/static-curl/releases/download/8.21.0/curl-linux-x86_64-musl-8.21.0.tar.xz | curl (MIT-like) |
| darwin-arm64 | rustscan | 2.4.1        | https://github.com/bee-san/RustScan/releases/download/2.4.1/aarch64-macos-rustscan.tar.gz.zip   | GPLv3    |

All linux-amd64 binaries are statically linked (verified `statically linked` /
`static-pie linked` via `file`), so they run on a bare target host with no shared
library dependencies. nmap/ncat 6.49BETA1 is old but the `-sn` host-discovery and
`-sV -T4` service-scan flags and their output format are unchanged, so the agent's
output parsers work against it.

### macOS local-dev note

macOS does not support fully static linking, and no portable static macOS
`nmap`/`ncat`/`curl` distribution exists. macOS ships `nc` and `curl` by default,
and `nmap` is available via Homebrew (`brew install nmap`). On darwin-arm64 the
agent therefore bundles only `rustscan` and resolves the rest from the host `PATH`.
This platform is used for local development and native-build testing only, not
deployment.

## SHA-256 Checksums

| File                       | SHA-256                                                            |
|----------------------------|-------------------------------------------------------------------|
| `linux-amd64/nmap`         | 353fd20c9efcd0328cea494f32d3650b9346fcdb45bfe20d8dbee2dd7b62ca62  |
| `linux-amd64/nc`           | 328a7313830e97685b372ff4de89ee0161abe88c50a2250a0b34de7ff4fc6587  |
| `linux-amd64/rustscan`     | 8a507fbd1821746ce747dcce6c2c4cfa0c6aff9c4d1095967a4c2707db6d1b41  |
| `linux-amd64/curl`         | 153ca463957609117d21a848be29b70691b85f9e5cc9370c7daa037b839a4e45  |
| `darwin-arm64/rustscan`    | e9d6713dea6592cc857c25f948a531322e4f38f723768209cf2ce2ea9a9c5403  |

Verify with:

```bash
cd agents/unix/src/main/resources/tools
shasum -a 256 -c <<'EOF'
353fd20c9efcd0328cea494f32d3650b9346fcdb45bfe20d8dbee2dd7b62ca62  linux-amd64/nmap
328a7313830e97685b372ff4de89ee0161abe88c50a2250a0b34de7ff4fc6587  linux-amd64/nc
8a507fbd1821746ce747dcce6c2c4cfa0c6aff9c4d1095967a4c2707db6d1b41  linux-amd64/rustscan
153ca463957609117d21a848be29b70691b85f9e5cc9370c7daa037b839a4e45  linux-amd64/curl
e9d6713dea6592cc857c25f948a531322e4f38f723768209cf2ce2ea9a9c5403  darwin-arm64/rustscan
EOF
```

## Licensing Note

nmap/ncat (NPSL, a modified GPLv2) and rustscan (GPLv3) are copyleft. Bundling and
redistributing these binaries carries source-availability obligations under those
licenses. This is an academic/prototype (TFG) system and the licensing implication
is a documented, conscious tradeoff (see `specs/020-agent-tool-bundling/research.md`
§4). For wider redistribution, replace nmap/rustscan/ncat with permissively-licensed
or custom-built equivalents (e.g. a minimal Go-based scanner) in a follow-up effort.

## Refreshing the binaries

The binaries are versioned build assets, not source. To update, download the same
assets listed above, verify architecture with `file`, place them in the matching
`tools/<os>-<arch>/` directory with the exact filename the `BundledTool` enum
expects (`nmap`, `rustscan`, `nc`, `curl`), `chmod +x`, and update the version and
checksum tables here.

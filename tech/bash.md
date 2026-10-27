# Bash (the scripts)

## What it is

Bash is a **shell**: a program that reads commands you type (or a file of commands, a **script**) and runs them. A script starts with a **shebang** line, `#!/usr/bin/env bash`, which tells the system to run the file with bash. Commands are separated by new lines. A command that succeeds ends with **exit code** 0; a failing one ends with a non-zero code.

## Why this repository uses it

A reader should be able to see each "Done when" line of the lesson with one command. Scripts do that with tools every Mac and Linux machine has (`curl`, `jq`, `bash`). Bash is also the right size: each script is a short list of commands, not a program.

## Where it appears

| Path | What it does |
| --- | --- |
| `scripts/scenario.sh` | checkout, order list, rollback and audit over HTTP (`make run`) |
| `scripts/audit.sh` | runs the dependency scanner in a container (`make audit`) |
| `docker-compose.yml` health checks | a one-line bash command inside each container |

## How it works underneath

1. `bash script.sh` (or `./script.sh` if the file is executable) reads the file line by line and runs each command as a separate process. A **pipe** `a | b` connects a's output to b's input. `a && b` runs b only if a succeeded; `a || b` only if a failed.
2. A **variable** is set with `NAME=value` (no spaces) and read with `$NAME`. `$(command)` runs the command and substitutes its output. Double quotes `"$NAME"` keep a value with spaces as one word; single quotes `'...'` keep everything literal.
3. `${1:-default}` means "the first argument, or `default` if there is none". `$0` is the script's own path.
4. `set -euo pipefail` at the top changes how failures behave: `-e` stops the script at the first failing command; `-u` makes an unset variable an error; `-o pipefail` makes a pipeline fail if any stage fails, not only the last.
5. `trap 'command' EXIT` runs a command when the script ends, however it ends. It is used here to delete temporary folders.
6. `curl` talks HTTP and `jq` reads JSON (see `curl.md` and `jq.md`).

## Walkthrough of this repository's use

### `scripts/scenario.sh`

```bash
set -euo pipefail

BASE="${1:-http://localhost:${APP_PORT:-18102}}"
JSON='Content-Type: application/json'
heading() { printf '\n== %s\n' "$1"; }
```

- `set -euo pipefail`: stop on the first error, unset variable is an error, a pipeline fails if any stage fails.
- `BASE="${1:-http://localhost:${APP_PORT:-18102}}"`: first argument, else the host port from `$APP_PORT`, else 18102.

```bash
CUSTOMER="$(curl -fsS -X POST "$BASE/customers" -H "$JSON" -d '{"name":"Ada Example"}')"
echo "$CUSTOMER" | jq -c .
ID="$(echo "$CUSTOMER" | jq -r .id)"
curl -fsS -X PUT "$BASE/stock/sku-keyboard" -H "$JSON" -d '{"available":5}' | jq -c .
curl -fsS -X PUT "$BASE/stock/sku-monitor" -H "$JSON" -d '{"available":0}' | jq -c .
```

`CUSTOMER="$(curl -fsS -X POST ... )"` stores the JSON answer in a variable. `echo "$CUSTOMER" | jq -c .` prints it compactly. `ID="$(echo "$CUSTOMER" | jq -r .id)"` extracts the id as raw text. The two `PUT` calls set the stock levels (see `curl.md` for the flags).

```bash
heading "Trap 2 fixed: a checkout whose second line is out of stock is 409 and rolls back as a whole"
curl -sS -i -X POST "$BASE/customers/$ID/orders" -H "$JSON" \
  -d '{"lines":[{"sku":"sku-keyboard","quantity":1},{"sku":"sku-monitor","quantity":1}]}' |
  tr -d '\r' | grep -E '^(HTTP|Content-Type)|^\{' | sed 's/^{/body: {/'
echo "orders still stored: $(curl -fsS "$BASE/customers/$ID/orders" | jq length)   (1, the failed one left nothing)"
echo "sku-keyboard stock:  $(curl -fsS "$BASE/stock/sku-keyboard" | jq .available)   (3, the failed one gave its unit back)"
```

The failing checkout is sent with `curl -sS -i` (headers included); `tr -d '\r'`, `grep -E` and `sed` pick out the status line, content type and body. `echo "orders still stored: $(curl -fsS "$BASE/customers/$ID/orders" | jq length)"` counts the orders left afterwards (`jq length` is the list length): 1, because the failed checkout rolled back.

### `scripts/audit.sh`

```bash
IMAGE="${OSV_SCANNER_IMAGE:-ghcr.io/google/osv-scanner@sha256:4f3b7e2c64b9d38a5febc5a7e4a3dfef1d1362f726dea7a2ef4762b46469fcd0}"
docker run --rm -v "$PWD:/src:ro" -w /src "$IMAGE" scan source --lockfile=pom.xml --config=osv-scanner.toml
```

Runs the dependency scanner in a throwaway container (`--rm`) with the project folder mounted read-only at `/src`; the image is pinned by digest.

**The container health checks** use bash because the Java images have no `curl`:

```yaml
test: ["CMD", "bash", "-c", "exec 3<>/dev/tcp/localhost/8080 && printf 'GET /actuator/health/readiness HTTP/1.0\\r\\n\\r\\n' >&3 && grep -q '\"status\":\"UP\"' <&3"]
```

`exec 3<>/dev/tcp/localhost/PORT` opens a TCP connection and attaches it to file descriptor 3 (bash treats `/dev/tcp/host/port` as a special file); `printf 'GET ... HTTP/1.0\r\n\r\n' >&3` writes an HTTP request into it; `grep -q ... <&3` reads the answer from it and succeeds if it contains the text. No outside tool is needed.

## Try it yourself

1. After `make up`: `scripts/scenario.sh http://localhost:18102`.
2. `bash -n scripts/scenario.sh` checks the syntax without running it.
3. `bash -x scripts/scenario.sh 2>&1 | head -40` shows each command with variables expanded.

5. `shellcheck scripts/*.sh` (if installed) finds common mistakes.

## Mistakes people make

- Unquoted variables: `rm -rf $DIR` with an empty `DIR` is a disaster; `"$DIR"` is safe.
- Forgetting `set -euo pipefail`: a failed step is ignored and the script reports success.
- Spaces around `=` in an assignment: `NAME = value` is a command named `NAME`.
- Parsing JSON with `grep`/`sed`: use `jq`.
- Assuming the script runs from the project root: scripts here `cd "$(dirname "$0")/.."` first when they need it.

## Check yourself

1. What does `set -e` do? *Stops the script at the first failing command.*
2. What is `$(...)`? *Runs a command and substitutes its output.*
3. What does `${1:-x}` mean? *The first argument, or `x` if absent.*
4. What does `trap '...' EXIT` do? *Runs the command when the script ends.*
5. What does a trailing `&` do? *Runs the command in the background.*

## Further reading

- GNU Bash Reference Manual: https://www.gnu.org/software/bash/manual/bash.html
- ShellCheck wiki (common mistakes): https://www.shellcheck.net/wiki/

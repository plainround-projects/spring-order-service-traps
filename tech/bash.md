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

A **terminal** is a window where you type commands as text. The **shell** is the program inside it that reads each line, finds the program you name (it looks through the folders listed in the environment variable `PATH`), and runs it in the **working directory**, the folder you are currently in. A **script** is a file of such commands. Bash is the shell this repository's scripts are written for; macOS also ships zsh, which is similar for the simple commands you type.

Words first. Each is used in the walkthrough below.

- A **process** is a running program. When a shell runs `curl`, the operating system starts a new process for `curl`; when it ends, it returns an **exit code**, a number: `0` means success, anything else means failure.
- **Standard output** (stdout) is where a program writes its normal results; **standard error** (stderr) is where it writes problems. Both appear on your screen unless redirected. **Standard input** (stdin) is where it reads input from.
- A **redirection** sends a stream elsewhere: `> file` writes stdout to a file, `2>&1` sends stderr to the same place as stdout, `< file` reads stdin from a file, `>/dev/null` throws output away (`/dev/null` is a special file that discards everything).
- A **pipe** `a | b` connects a's stdout to b's stdin, so the two run together and b processes what a prints.
- An **environment variable** is a named value that every process receives from the one that started it (`HOME`, `PATH`, `APP_PORT`). `NAME=value command` sets one for that command only.
- A **variable** of the script itself is set with `NAME=value` (no spaces around `=`) and read with `$NAME` or `${NAME}`. `$(command)` runs the command and gives its stdout. `${1:-default}` is the first argument, or `default` if there is none; `${2:+&$2}` is `&` plus the second argument if there is a second argument, otherwise nothing.
- **Quoting:** inside double quotes `"..."` the shell still expands `$NAME` and `$(...)` but keeps spaces together as one word; inside single quotes `'...'` nothing is expanded.
- `a && b` runs b only if a succeeded; `a || b` only if a failed; `a; b` runs b after a whatever happened. `( ... )` runs the commands in a **subshell**, a child shell whose `cd` and variables do not leak back. `{ ...; }` groups commands without a child shell.
- A **function** is a named block: `name() { ...; }`; `$1` is its first argument.
- A **background job**: a trailing `&` starts the command and lets the script go on; `$!` is the id of the last one (a **process id**, PID); `wait` blocks until background jobs end; `kill PID` asks a process to stop (it sends the **signal** SIGTERM, a polite request).
- `set -euo pipefail` at the top changes how failures behave: `-e` stops the script at the first failing command; `-u` makes an unset variable an error; `-o pipefail` makes a pipeline fail if any stage fails, not only the last.
- `trap 'command' EXIT` runs a command when the script ends, however it ends.
- A **regular expression** (regex) is a pattern for text, used by `grep -E`, `sed` and `awk`: `^` start of a line, `$` end, `.` any character, `*` repeat, `|` or, `[a-z]` one character from a set.
- A **here-string** `<<<"text"` feeds the text to a command's stdin.
- `/dev/tcp/host/port` is a special name that **bash** (not other shells) treats as a network connection, and a **file descriptor** is a small number a process uses to refer to an open file or connection (0 stdin, 1 stdout, 2 stderr; bash lets you open more, such as 3).

The shebang `#!/usr/bin/env bash` on the first line tells the system to run the file with bash. A script becomes a command when the file is marked executable (`chmod +x`).

## Walkthrough of this repository's use

Each script is printed whole with line numbers, and every line is explained after it.

### `scripts/scenario.sh` (`make run`)

```bash
 1  #!/usr/bin/env bash
 2  # The lesson's scenario through the HTTP API: checkout, the order list, a checkout that fails halfway, and the
 3  # audit trail. The three traps themselves are proved by the tests (`make traps`).
 4  # Usage: scripts/scenario.sh [base-url]   (default http://localhost:18102)
 5  set -euo pipefail
 6
 7  BASE="${1:-http://localhost:${APP_PORT:-18102}}"
 8  JSON='Content-Type: application/json'
 9  heading() { printf '\n== %s\n' "$1"; }
10
11  heading "Set up: one customer and two SKUs (sku-keyboard: 5 units, sku-monitor: 0 units)"
12  CUSTOMER="$(curl -fsS -X POST "$BASE/customers" -H "$JSON" -d '{"name":"Ada Example"}')"
13  echo "$CUSTOMER" | jq -c .
14  ID="$(echo "$CUSTOMER" | jq -r .id)"
15  curl -fsS -X PUT "$BASE/stock/sku-keyboard" -H "$JSON" -d '{"available":5}' | jq -c .
16  curl -fsS -X PUT "$BASE/stock/sku-monitor" -H "$JSON" -d '{"available":0}' | jq -c .
17
18  heading "Checkout: two keyboards -> 201, the order with its lines"
19  curl -fsS -X POST "$BASE/customers/$ID/orders" -H "$JSON" -d '{"lines":[{"sku":"sku-keyboard","quantity":2}]}' | jq -c .
20
21  heading "The customer's orders with their lines (one query for any number of orders, see make traps)"
22  curl -fsS "$BASE/customers/$ID/orders" | jq -c .
23
24  heading "Trap 2 fixed: a checkout whose second line is out of stock is 409 and rolls back as a whole"
25  curl -sS -i -X POST "$BASE/customers/$ID/orders" -H "$JSON" \
26    -d '{"lines":[{"sku":"sku-keyboard","quantity":1},{"sku":"sku-monitor","quantity":1}]}' |
27    tr -d '\r' | grep -E '^(HTTP|Content-Type)|^\{' | sed 's/^{/body: {/'
28  echo "orders still stored: $(curl -fsS "$BASE/customers/$ID/orders" | jq length)   (1, the failed one left nothing)"
29  echo "sku-keyboard stock:  $(curl -fsS "$BASE/stock/sku-keyboard" | jq .available)   (3, the failed one gave its unit back)"
30
31  heading "Trap 3 fixed: both attempts, including the failed one, are in the audit trail"
32  curl -fsS "$BASE/audit?limit=5" | jq -c '.[] | {action, detail}'
33
34  heading "Validation, errors and health"
35  curl -sS -X POST "$BASE/customers" -H "$JSON" -d '{"name":""}' | jq -c .
36  curl -sS "$BASE/customers/999999/orders" | jq -c .
37  curl -fsS "$BASE/actuator/health" | jq -c .
```

- Lines 2 to 4: comments. Line 5: `set -euo pipefail` (explained above).
- Line 7: `BASE` is the service\'s address: the first argument, or `http://localhost:` plus `$APP_PORT`, or 18102 if that variable is not set.
- Line 8: a header stored in a variable. Line 9: the `heading` function (a blank line, `== `, the argument).
- Line 11: the title.
- Line 12: `CUSTOMER="$(curl -fsS -X POST "$BASE/customers" -H "$JSON" -d '{"name":"Ada Example"}')"` sends a POST to create a customer and stores the JSON answer in the variable. `-f` fails on an HTTP error, `-s` silent, `-S` still shows errors, `-X POST` the method, `-H` a header, `-d` the body (single quotes keep the double quotes inside).
- Line 13: `echo "$CUSTOMER" | jq -c .` prints the answer compactly.
- Line 14: `jq -r .id` extracts the `id` as plain text into `ID`.
- Lines 15 and 16: `PUT` sets the stock of two SKUs: 5 keyboards, 0 monitors.
- Lines 18 and 19: checkout of two keyboards; the answer is the stored order with its lines.
- Lines 21 and 22: lists the customer\'s orders.
- Lines 24 to 27: a checkout whose second line is out of stock. A trailing `\` continues the command on the next line, and a trailing `|` continues the pipeline. `curl -sS -i` shows headers too (no `-f`, so the 409 is printed). `tr -d '\r'` removes carriage returns, `grep -E '^(HTTP|Content-Type)|^\{'` keeps the status line, the content type and the JSON line, and `sed 's/^{/body: {/'` labels the JSON.
- Line 28: `$(curl ... | jq length)` counts the orders: 1, because the failed checkout rolled back. Line 29: reads the keyboard stock with `jq .available`: 3, the first line\'s reservation was rolled back too.
- Lines 31 and 32: `/audit?limit=5` returns the newest audit entries; `jq -c '.[] | {action, detail}'` prints one small object for each entry.
- Lines 34 to 37: a blank name (400), an unknown customer (404) and the health answer.

### `scripts/audit.sh` (`make audit`)

```bash
1  #!/usr/bin/env bash
2  # Dependency audit (OSV-Scanner 2.7.0, pinned by digest): OSV-Scanner reads pom.xml (with the Spring Boot parent's managed versions) and reports
3  # known vulnerabilities. Fails on any finding that is not listed, with a reason, in osv-scanner.toml.
4  set -euo pipefail
5
6  cd "$(dirname "$0")/.."
7  IMAGE="${OSV_SCANNER_IMAGE:-ghcr.io/google/osv-scanner@sha256:4f3b7e2c64b9d38a5febc5a7e4a3dfef1d1362f726dea7a2ef4762b46469fcd0}"
8  docker run --rm -v "$PWD:/src:ro" -w /src "$IMAGE" scan source --lockfile=pom.xml --config=osv-scanner.toml
```

- Line 4: `set -euo pipefail`. Line 6: moves to the project root.
- Line 7: `IMAGE` is the scanner image from `OSV_SCANNER_IMAGE` or the pinned default (OSV-Scanner 2.7.0, fixed by its digest).
- Line 8: `docker run --rm -v "$PWD:/src:ro" -w /src "$IMAGE" scan source --lockfile=pom.xml --config=osv-scanner.toml` runs the scanner in a throwaway container (`--rm`) with the project mounted read-only at `/src` and used as the working folder (`-w`), reading the dependency list from `pom.xml`.

**The container health checks** are one-line bash commands inside `docker-compose.yml` because the Java images have no `curl`:

```yaml
test: ["CMD", "bash", "-c", "exec 3<>/dev/tcp/localhost/8080 && printf 'GET /actuator/health/readiness HTTP/1.0\\r\\n\\r\\n' >&3 && grep -q '\"status\":\"UP\"' <&3"]
```

Read it in pieces: `["CMD", "bash", "-c", "..."]` runs bash with the string after `-c` as the program. `exec 3<>/dev/tcp/localhost/PORT` opens a network connection to the port on this machine and attaches it, for reading and writing (`<>`), to file descriptor 3. `printf 'GET /actuator/health/readiness HTTP/1.0\r\n\r\n' >&3` writes an HTTP request into it (`>&3` redirects stdout to descriptor 3). `grep -q '"status":"UP"' <&3` reads the answer from descriptor 3 (`<&3`) and succeeds only if the text appears. The `&&` between the three parts means each runs only if the one before succeeded, so the check passes only when the server answers UP.

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

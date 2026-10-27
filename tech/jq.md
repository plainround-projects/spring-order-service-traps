# jq

## What it is

`jq` is a command-line tool that filters JSON. You give it a **filter** (a small expression) and it reads JSON from standard input and prints the result. It is like `sed` or `awk`, but it understands the structure of JSON.

## Why this repository uses it

The service answers in JSON. The scripts need single values from it (an id, a count, a status) and the README shows readable output. `jq` is installed with `brew install jq` and used as a prerequisite.

## Where it appears

| Path | Role |
| --- | --- |
| `scripts/scenario.sh` | extracts ids and counts; pretty-prints answers |
| `Makefile` (`make health`) | `jq -e '.status == "UP"'` as a yes-or-no test |

## How it works underneath

1. `jq` parses the JSON into a tree, applies the filter to it, and prints each result as JSON.
2. The simplest filter is `.` (the whole input, pretty-printed). `.name` takes a field. `.a.b` goes deeper. `.[0]` takes the first item of a list. `.[]` produces every item.
3. `|` inside a filter pipes one step into the next. `{a, b}` builds a new object. `map_values(f)` applies `f` to every value of an object. `length` counts.
4. Flags: `-r` prints strings raw (without quotes); `-c` prints compact one-line JSON; `-e` sets the exit code from the last result (non-zero if it is `false` or `null`); `-s` ("slurp") reads all inputs into one list.

## Walkthrough of this repository's use

```bash
ID="$(echo "$CUSTOMER" | jq -r .id)"
```

`jq -r .id` extracts the id as plain text.

```bash
curl -fsS "$BASE/audit?limit=5" | jq -c '.[] | {action, detail}'
```

`.[] | {action, detail}` goes through every audit entry (`.[]`) and keeps only two fields. `jq length` elsewhere counts the orders in a list.

## Try it yourself

1. `echo '{"a":{"b":[1,2,3]}}' | jq '.a.b[1]'` prints `2`.
2. `echo '{"a":1}' | jq -e '.a == 2'; echo $?` prints `false` then `1`: the exit code a script can test.
3. `curl -s localhost:18102/audit | jq 'length'` counts items in the answer.
4. `echo '{"a":"x"}' | jq -r .a` prints `x` without quotes.

## Mistakes people make

- Forgetting `-r` and getting `"value"` with quotes in a shell variable.
- Using `.field` on a list: use `.[0].field` or `.[]`.
- Treating `null` as an error: a missing field prints `null` unless `-e` is used.
- Parsing JSON with `grep` or `sed`.

## Check yourself

1. What does `.a.b` do? *Reads field `b` inside field `a`.*
2. What does `-r` do? *Prints strings without quotes.*
3. What does `jq -e` do with a `false` result? *Exits non-zero.*
4. What does `.[]` do? *Produces every element of a list.*
5. What does `-s` do? *Reads all input values into one list.*

## Further reading

- jq manual: https://jqlang.org/manual/

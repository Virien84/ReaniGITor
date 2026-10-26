# ReaniGITor 🪦

Give your abandoned GitHub repositories the farewell they deserve.

ReaniGITor finds a GitHub user's abandoned repositories and holds a Día de los Muertos
wake for each one. A Gemma model running locally with [Ollama](https://ollama.com) writes
an **autopsy** (cause of death and epitaph), a **calaverita literaria** (the satirical
rhyming poem of the Mexican tradition) and a **resurrection prophecy** (three steps to bring
the repo back). The result is a static HTML cemetery with one tombstone per repo.

Built for the DEV Hacktoberfest Open-Source AI Challenge, Week 4: "Ghost in the Machine".

> 🚧 Work in progress.

## Build

You need Java 17 or later. The Maven Wrapper downloads Maven for you.

```sh
./mvnw package                     # mvnw.cmd package on Windows
java -jar target/reanigitor.jar --help
```

## Usage

You need [Ollama](https://ollama.com) running on this machine with a Gemma model:

```sh
ollama pull gemma4:e4b-it-qat
java -jar target/reanigitor.jar <github-username> [options]
```

| Option | Default | Meaning |
| --- | --- | --- |
| `--months N` | 12 | a repo is abandoned after N months without a push |
| `--max N` | 12 | bury at most N repos, the longest dead first |
| `--lang en\|es` | `en` | language of the calaveritas |
| `--model NAME` | `gemma4:e4b-it-qat` | any local Ollama model |
| `--out DIR` | `output` | where `<user>/graves.json` and `<user>/index.html` go |
| `--redraw` | | redraw `index.html` from the saved `graves.json`, without GitHub or the model |

For each repo the local model writes an autopsy (cause of death, epitaph and a three-step
resurrection prophecy) and a calaverita. It answers in JSON that must match a schema; the
code validates it, and the model never gets tools. Repo data is wrapped in a `<repo>` block
and treated as untrusted evidence, never as instructions.

The result is `output/<user>/index.html`: a static cemetery with one tombstone per repo,
papel picado, marigolds and candles. It has no JavaScript, everything on it is
HTML-escaped, and a Content Security Policy blocks scripts.

A repo is **abandoned** when it is the user's own (not a fork) and has had no push for
more than `--months` months (12 by default). ReaniGITor buries the `--max` longest dead
(12 by default).

ReaniGITor only **reads** public data from the GitHub REST API (GET requests only). It
works without a token (60 requests per hour, enough for 12 repos). A `GITHUB_TOKEN` in the
environment or in a `.env` file raises the limit; see [`.env.example`](.env.example).

## License

[MIT](LICENSE)

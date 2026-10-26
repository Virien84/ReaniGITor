# ReaniGITor 🪦

Finds a GitHub user's **abandoned repositories** and gives each one a Día de los Muertos
farewell, written by an open-weight model running locally:
an **autopsy** (cause of death + epitaph), a **calaverita literaria** (the satirical rhyming
poem of the Mexican tradition) and a **resurrection prophecy** (3 steps to bring it back).
The output is a static HTML **cemetery** with one tombstone per repo.

Built for the **Hacktoberfest Open-Source AI Challenge: Week 4, "Ghost in the Machine"**:
build something spooky, or something that honors the past in the spirit of Día de los
Muertos, with open-weight models or open-source AI.
- Challenge: https://dev.to/challenges/hacktoberfest-week4-2026-10-26 (hub: https://dev.to/challenges/hf26)
- Safe deadline: **Saturday 2026-10-31, 06:59 UTC**. The page rules say Oct 31 at
  11:59 PM PDT, but the API reports an end 24 h earlier, so we use the earlier one.
- The user is registered for the Week 4 MLH event.
- Submission is a DEV post using the challenge template (tags `#devchallenge`,
  `#hf26challenge`). Writing quality weighs the most; the post must explain why open
  innovation matters for what we built.
- Target categories: **Best Use of Gemma** ($200) and **Best Use of ElevenLabs** ($100).
  Only one prize can be won per challenge.
- **Everything in the repo is in English** (code, comments, logs, prompts, docs, README),
  because the judges evaluate it in English. The only exception is `cambios.md`, the user's
  task list, which is in Spanish. The conversation with the user is in Spanish.
- Calaveritas can be generated in Spanish or English (an option). The rest of the output is
  in English.

## Repository

- Name: **ReaniGITor** (chosen by the user on 2026-10-26). Planned public GitHub repo:
  https://github.com/Virien84/ReaniGITor (account `Virien84`, `gh` is logged in; not created
  yet, see `cambios.md`). Branch
  `main`. MIT license (same as the Week 3 project).
- Tracked: code, `README.md`, `LICENSE`, `CLAUDE.md`, `cambios.md`, `skills-lock.json`.
  Git-ignored: `.env`, `target/`, the generated `output/`, and `.claude/` and `.agents/`
  (local agent settings and installed skills; `skills-lock.json` pins the skills).

## Task list: `cambios.md`

**`cambios.md` (repo root) is the reference document for pending and done tasks**, also at
the start of a new session (after `/clear`):

- `## CAMBIOS`: what the user wants done, in order. It is the only section to read and
  execute.
- `## HECHO`: finished items, as `✅ DD/MM/AAAA · <original text>` followed by a `→` line
  that says what was done. No need to reread it, except to look something up.
- When an item is finished: move it from `CAMBIOS` to `HECHO`, update this file if anything
  described here changed, and commit (once the repo exists). Push only when the user asks.
- New requests from the user that are not done right away go to `CAMBIOS`.

## Mandatory rules (non-negotiable)

1. **The core runs on an open-weight model, locally.** All text generation goes through
   Ollama on this machine (`http://127.0.0.1:11434`). No cloud LLM APIs, no closed models.
   The Ollama host must be localhost.
2. **ElevenLabs is optional garnish.** It is closed and in the cloud, so the project must
   work fully without it: with no `ELEVENLABS_API_KEY`, the cemetery is generated without
   audio. Only already-generated public text (calaveritas) is sent to ElevenLabs, never
   repo contents.
3. **Security comes first.**
   - The LLM **never gets tools**. It only returns JSON that matches a schema (Ollama
     `format=`), which the code validates. Code decides every action, never the model.
   - Repo data (README, descriptions, commit messages, issue titles) is **untrusted**. It is
     delimited in the prompt (`<repo>…</repo>`) and the prompts say to treat it as data.
   - **Everything written to the HTML is escaped** (model output and repo data alike). No
     inline content from the model is ever treated as HTML or JS.
   - The tool **only reads** from GitHub (public REST API, GET only). It never writes,
     archives, deletes or comments on anything.
   - Secrets (`GITHUB_TOKEN`, `ELEVENLABS_API_KEY`) are read from the environment or a
     git-ignored `.env`. Never print, log or `cat` them, never put them in code, and never
     ask for them in chat. If one is pasted in chat, tell the user to rotate it.
   - Don't run anything that spends ElevenLabs credits until the user says yes.

## Stack (decided, nothing built yet)

- **Java 17** (Liberica JDK 17.0.14, `C:\Program Files\BellSoft\LibericaJDK-17`) +
  **Maven 3.10.0** (installed with `scoop install maven`; winget has no `Apache.Maven`).
  The user `JAVA_HOME` was fixed on 2026-10-26 (it pointed to a deleted JDK 15). Shells
  opened before that still have the old value: set `JAVA_HOME` in the command if `mvn`
  complains. The platform encoding is Cp1252, so the POM must set UTF-8 explicitly.
- HTTP: `java.net.http.HttpClient`. JSON: Jackson.
- GitHub REST API without a token (60 requests/hour). `GITHUB_TOKEN` is optional, to raise
  the limit.
- **Ollama** 0.40.6 (installed; any `ollama` command starts it). Models present:
  `gemma4:e4b-it-qat` (default; first JSON call took ~37 s including the model load),
  `gemma4:e4b`, `gemma4:e2b`. `gemma4:e2b-it-qat` is not downloaded. Hardware: GTX 1650
  (4 GB VRAM), 16 GB RAM. In the Week 3 project (`../4/CLAUDE.md`) `gemma4:e4b-it-qat` ran
  on this machine (~24% on the GPU, the rest on the CPU, slow). `gemma4:e2b-it-qat` is
  faster. Use `think=false`. Ollama 0.35.1 crashed with a CUDA error with Gemma 4, so
  don't downgrade.
- **ElevenLabs**: generate the MP3 files **once with the official CLI** and link them from
  the static HTML (decision: the skills only document Python, JS and the CLI, not Java or
  REST). First-time key setup: `npx -y @elevenlabs/cli@latest onboard init` from the
  project folder.
- Output: a static HTML cemetery that can be opened locally or published.

## Planned pipeline

`GitHub user → list repos → pick the abandoned ones (no push in N months, threshold to
decide) → gather the "remains" (README, language, stars, open issues, last commit message,
dates) → Gemma: autopsy + epitaph, calaverita, resurrection prophecy (JSON) → HTML
cemetery → (optional) ElevenLabs: narration of each calaverita + ambient sound`.

## Installed skills

- **ElevenLabs** (project, `.agents/skills/`, linked from `.claude/skills/`; reviewed on
  2026-10-26, Markdown only, MIT): `text-to-speech` (`eleven_v4`, audio tags like
  `[whispers]`, effects such as the `old_radio` filter and `hall` reverb), `sound-effects`
  (0.5–30 s, loopable) and `setup-api-key` (only for an existing key or one that stopped
  working). The other 7 were removed. `skills-lock.json` pins their hashes.
- **Programming** (Superpowers plugin and others installed by the user): use them whenever
  they apply. In particular `superpowers:brainstorming` before designing something new,
  `superpowers:writing-plans` for multi-step work, `superpowers:test-driven-development`
  when writing code, `superpowers:systematic-debugging` for bugs and
  `superpowers:verification-before-completion` before saying something is done.
- **Front end**: before touching the cemetery HTML/CSS, always invoke
  `frontend-design:frontend-design` and follow it, even for small changes.
- **DevRelay**: `devrelay-publishing` for the DEV post and `devrelay-sessions` to save the
  agent session (optional, it helps the judges).

## CAMBIOS

- Prompts y salida JSON validada con Gemma: autopsia (causa de la muerte + epitafio), calaverita literaria (opción español/inglés) y profecía de resurrección en 3 pasos. Afinarlos hasta que salgan graciosos.
- Generar el cementerio en HTML estático (usar /frontend-design:frontend-design): una lápida por repo, velas y cempasúchil.
- Probar con los repos del usuario (pedirle su usuario de GitHub).
- (Opcional) ElevenLabs: crear cuenta y reclamar créditos en hacktoberfest.com/my, configurar la key con `onboard init`, narrar cada calaverita con voz espectral (`[whispers]`, filtro `old_radio`, reverberación) y sonido ambiente de cementerio. Pedir permiso antes de gastar créditos.
- Escribir el post en DEV con la plantilla del challenge (`#devchallenge`, `#hf26challenge`) y, opcionalmente, guardar la sesión con DevRelay e incrustarla. Fecha límite segura: 31/10/2026 a las 06:59 UTC.

## HECHO

- ✅ 26/10/2026 · Buscar el último challenge de Hacktoberfest 2026 en DEV.
  → Week 4 «Ghost in the Machine» (26–31 de octubre): algo terrorífico o que honre el pasado al estilo del Día de Muertos, con IA open source / modelos open-weight.
- ✅ 26/10/2026 · Elegir idea.
  → «Panteón de Repos»: autopsia, calaverita y profecía de resurrección para los repos abandonados de un usuario de GitHub, con Gemma en local, en Java. Mezcla de las ideas 1 y 4.
- ✅ 26/10/2026 · Instalar las skills de patrocinadores y registrarse en la Week 4.
  → Instaladas las skills de ElevenLabs (las únicas publicadas, en el evento Hacktoberfest 2026). Registrado en el evento de MLH de la Week 4.
- ✅ 26/10/2026 · Revisar las skills que vamos a usar y quitar las 7 que sobran.
  → `setup-api-key`, `text-to-speech` y `sound-effects` revisadas: solo Markdown, licencia MIT, sin scripts; usan la CLI oficial `@elevenlabs/cli@latest` (versión sin fijar). Quitadas `agents`, `dubbing`, `music`, `speech-engine`, `speech-to-text`, `voice-changer` y `voice-isolator`.
- ✅ 26/10/2026 · Crear CLAUDE.md y cambios.md, haciendo que CLAUDE.md contemple cambios.md.
  → CLAUDE.md (en inglés, como el de la Week 3) recoge el challenge, las reglas, el stack decidido, el pipeline y las skills, y explica el flujo de cambios.md.
- ✅ 26/10/2026 · Preparar el entorno: instalar Maven (`winget install Apache.Maven`), arrancar Ollama y comprobar que está `gemma4:e4b-it-qat` (o `gemma4:e2b-it-qat`), descargándolo si falta.
  → winget no tiene `Apache.Maven`; Maven 3.10.0 instalado con `scoop install maven`. Corregido el `JAVA_HOME` de usuario, que apuntaba a un JDK 15 borrado, para que apunte al Liberica JDK 17. Ollama 0.40.6 arrancado; `gemma4:e4b-it-qat` ya estaba y devuelve JSON con `format` y `think=false` (~37 s la primera llamada). No hizo falta descargar nada.
- ✅ 26/10/2026 · Decidir el nombre definitivo del proyecto y crear el repositorio (git + GitHub, licencia open source).
  → Nombre elegido: **ReaniGITor**. Repo git en `main` con licencia MIT, `README.md` inicial y `.gitignore` (excluye `.env`, `target/`, `output/`, `.claude/` y `.agents/`). El repo de GitHub no se pudo crear: el modo automático de Claude Code bloqueó `gh repo create --public`; queda pendiente en CAMBIOS. CLAUDE.md actualizado con el nombre y la sección Repository.
- ✅ 26/10/2026 · Esqueleto del proyecto Maven (Java 17), con `.gitignore` que excluya `.env` y la salida generada.
  → `pom.xml` (Java 17, UTF-8, Jackson 2.22.4, JUnit 6.1.3; shade genera `target/reanigitor.jar` ejecutable), Maven Wrapper fijado a 3.10.0 (`mvnw`/`mvnw.cmd`) y CLI mínima `ReaniGITor` (`--help`, `--version`, valida el nombre de usuario de GitHub) con 5 tests en verde (TDD). `.gitignore` ya excluía `.env`, `output/` y `target/` (comprobado con `git check-ignore`); añadidos `.env.example` y `.gitattributes`. CLAUDE.md y README actualizados.
- ✅ 26/10/2026 · Hacer push a https://github.com/Virien84/ReaniGITor (el repo ya existe, lo creó el usuario, y ya está añadido como `origin`).
  → `git push -u origin main`: `main` sigue a `origin/main` y es la rama por defecto en GitHub.
- ✅ 26/10/2026 · Cliente de GitHub (solo lectura): listar los repos de un usuario, decidir el umbral de «abandonado» (p. ej. sin push en 12 meses) y reunir los «restos» de cada repo (README, lenguaje, estrellas, issues abiertas, último mensaje de commit, fechas).
  → Rama `feature/github-client`. `GitHubClient` (solo GET, valida usuario y `owner/repo`, limpia y recorta todo el texto, errores claros de usuario inexistente y de límite de peticiones), `AbandonmentPolicy` (umbral: no es fork y sin push en **12 meses**, `--months`; los más antiguos primero, como mucho **12**, `--max`), `Remains` (README, último commit, hasta 5 issues/PR abiertas), `Env` (entorno y `.env`) y `Options`. La CLI imprime el censo de muertos. 34 tests en verde con un GitHub falso local; probado contra la API real con `octocat`.

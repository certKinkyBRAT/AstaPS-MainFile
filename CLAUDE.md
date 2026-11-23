# Notes for Claude

## Where changes go (owner's rule)

This repository is a personal fork: `emeliaMTF/AstaPS-QuestVibe`.

- Commit and push only to this fork. Open pull requests only against this fork's own `main` branch.
- Never push to, open pull requests on, open issues on, or comment on the original upstream AstaPS project or any repository other than the owner's forks.
- If a fix seems worth sharing upstream, tell the owner and let them ask the original creators first.

The matching game data fork is `emeliaMTF/AstaPS-Resource`, and the same rule applies there.

## Project notes

- Java 21 server built on Grasscutter. Quest logic is in `src/main/java/emu/grasscutter/game/quest/`.
- Quest bugs often come from the resource fork's data. For example, flattened `QUEST_COND_STATE_EQUAL [0, 3]` "quest 0" placeholders in `ExcelBinOutput/QuestExcelConfigData.json` hide the real prerequisites.
- The owner has little coding experience. Explain findings and changes in plain language.

## Building and testing

- `gradlew` is not executable in a fresh checkout, so run it as `bash ./gradlew ...`.
- If Maven Central answers 429 (Too Many Requests), retry with
  `--no-daemon --no-parallel --no-configuration-cache -Dorg.gradle.workers.max=1`.
- Three tests fail without a database or full server startup: `AccountPersistenceTest` (2) and `GrasscutterTest`. They fail on unmodified `main` too.

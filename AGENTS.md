# Agent Instructions

## Workflow: Iterative Prototyping

Never do a "waterfall" — never write large amounts of code in a single step.
Follow this cycle for every piece of work:

### Cycle

1. **Plan** — Analyze the task. Define a small, concrete mini-step.
2. **Pick the simplest part** — Start with the easiest piece. Build on solid ground.
3. **Write code** — Implement only that small piece.
4. **Read your own code** — Review what you wrote before running anything.
5. **Verify** — Run the full verification pipeline, in order:
   - `./gradlew lint`
   - `./gradlew assembleDebug`
   - Install and visually inspect on emulator/device
   - **Look at how it works** — visual verification is not optional
6. **Fix or commit:**
   - If something is broken → fix it and go back to step 5.
   - If everything works → commit with a conventional commit message.
7. **Repeat** — Pick the next small piece.

### Key Principles

- **A commit is a reward for working code**, not for written code.
- **Every commit must be a working state** of the application.
- **Verification is the heart of the process** — code that compiles but looks wrong is still broken.
- **Small steps prevent compounding errors** — never build on an unverified foundation.

## Commit Convention

Use Conventional Commits: <https://www.conventionalcommits.org/en/v1.0.0/>

### Format

```plaintext
<type>(scope): <description>
```

### Types

- feat — new feature
- fix — bug fix
- style — visual/CSS changes
- refactor — code restructuring without behavior change
- docs — documentation
- chore — tooling, dependencies, config
- test — adding or updating tests

## Safety & Communication

- **Never git add . or git add -A** — always review changed files first and stage only relevant files.
- **Ask before deleting** — never remove files or significant code without asking.
- **Discuss before acting** — explain what you plan to do and wait for confirmation.

## Code Hygiene

- **No new dependencies without asking.**
- **Comments are welcome** — add meaningful comments; avoid obvious ones.
- **Don't refactor unrelated code.**
- **Don't write tests unless asked.**
- **Read before writing** — understand existing code before modifying.
- **Don't over-engineer** — do exactly what was asked.

## Project Language

- UI language: Polish
- Code (variables, components, comments): English

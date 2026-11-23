# FloodWatch – Rules for AI Agents and Automation

This file binds **every** AI agent, assistant, bot or script that changes this repository, whatever tool or model it is. A person reviewing an agent's work should reject anything that breaks these rules. CI (`.github/workflows/pr-rules.yml`) checks rules 1 and 2 on every pull request.

## Before starting
- Read `docs/HANDOFF.md` (current state), `CODING_RULE.md` (how code must look and behave) and, for UI work, `docs/CONTEXT_AND_PLAN.md`.

## Hard rules

1. **No agent co-authorship or credit.** Commits and pull requests must not contain:
   - a `Co-Authored-By:` / `Co-authored-by:` trailer naming an agent, model or tool;
   - lines like "Generated with …", "Created by …" or "Written by …" that credit an AI;
   - any mention of an AI model, assistant or vendor as an author or contributor.

   This applies to commit messages, PR titles, PR descriptions and review comments that are posted. Authorship stays with the team members.

2. **No agent name in branch names.** A branch must never start with an agent's or tool's name, such as `claude/…`, `codex/…`, `copilot/…`, `cursor/…`, `gpt/…`, `openai/…`, `gemini/…`, `devin/…`, `agent/…`, `ai/…` or `bot/…`. Use a type prefix that describes the work:

   | Prefix | Use for |
   |---|---|
   | `feat/` | new behaviour, e.g. `feat/risk-page` |
   | `fix/` | bug fixes, e.g. `fix/alert-cooldown` |
   | `docs/` | documentation only |
   | `test/` | tests only |
   | `refactor/` | restructuring without behaviour change |
   | `chore/` | build, CI, dependencies, tooling |

3. **Never commit secrets.** `.env` is ignored by git and must stay that way. Never print, copy or commit its contents, Supabase keys, tokens or passwords. Change `.env.example` only with empty placeholders.
4. **Normal pushes only.** Never force-push, rebase or rewrite history on a branch that has been pushed. If a branch must be redone, push a new branch and open a new PR that links the old one.
5. **One task, one PR.** Branch from the latest `origin/main`, keep the PR focused, and do not merge your own PR unless the maintainer has said you may. Open it with a description of what was built, every edge case covered, and how to test it.
6. **Stay in your lane.** When several agents work in parallel, edit only the files your task assigns to you. Shared files (`config/AppProperties.java`, the `application*.properties` files, `README.md`, `WebConfig.java`) belong to one agent per round.
7. **Green before review.** `python3 scripts/check-style.py` must print `0 violations` and `cd api && ./mvnw test` must pass before you open or update a PR. Do not disable, skip or weaken tests or checks to get there.
8. **No guessing about results.** Report what was actually run and verified. Say clearly what was not tested.

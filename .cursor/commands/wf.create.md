---
description: Create a new work branch from master
allowed-tools: Bash(git status:*), Bash(git branch:*), Bash(git checkout:*)
---

## Context

- Current branch: !`git branch --show-current`
- Working tree status: !`git status --porcelain`

## Your task

Goal: create a new branch for isolated work.

Rules:
1. A branch name argument MUST be provided.
   - If missing, STOP and warn: "Branch name required. Example: 'update-engagement-session-status'"
2. Working tree MUST be clean.
   - If not, STOP and warn.
3. Current branch MUST be `master`.
   - If not, STOP and warn: "Not on master. Run /wf.align first."
4. No fetch, pull, or rebase is allowed here.
   - Repo alignment is assumed to have been handled by /wf.align.
5. Create and checkout a new branch using the provided name.

Execute only git commands.

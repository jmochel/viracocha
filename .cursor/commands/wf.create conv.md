---
description: Create a conventional work branch from a short work description (requires clean, up-to-date master)
allowed-tools: Bash(git status:*), Bash(git branch:*), Bash(git checkout:*)
---

## Context

- Current branch: !`git branch --show-current`
- Status (incl. ahead/behind): !`git status -sb`
- Working tree status: !`git status --porcelain`

## Your task

Create a conventional branch name from a work description, then create/check out that branch.

### Input
- You MUST receive a single argument: <work-description>
  - If missing, STOP and warn: "Work description required. Example: /wf.create.conv add session sequencing to engagement sessions"

### Preconditions (fail fast)
1. Working tree MUST be clean.
   - If `git status --porcelain` is non-empty, STOP and warn.
2. Current branch MUST be `master`.
   - If not, STOP and warn: "Not on master. Run /wf.align first."
3. Repo MUST be up to date with origin/master.
   - Inspect `git status -sb` output:
     - If it contains "ahead", "behind", or shows no upstream tracking for master, STOP and warn:
       "master is not in sync with origin. Run /wf.align first."

### Branch naming rules

- Construct the branch name according to `052-conventional-branch.mdc` from the supplied <work-description>:

### Execution
- Create and checkout the branch:
  - `git checkout -b <final-branch-name>`

Output only git tool calls.

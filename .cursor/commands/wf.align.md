---
description: Align local repo to start new work (clean tree, sync refs, checkout master)
allowed-tools: Bash(git status:*), Bash(git fetch:*), Bash(git remote:*), Bash(git branch:*), Bash(git checkout:*)
---

## Context

- Current branch: !`git branch --show-current`
- Working tree status: !`git status --porcelain`
- Remote default (origin/HEAD): !`git symbolic-ref --short refs/remotes/origin/HEAD 2>$null || echo "origin/HEAD not set"`

## Your task

Goal: bring this repo into a known-good starting state for new work.

Rules:
- If there are any uncommitted or untracked changes, STOP and print a warning. Do NOT run any other git commands.
- Otherwise:
  1. Fetch/prune from origin.
  2. Update origin/HEAD to match the remote default.
  3. Ensure a local `master` exists tracking `origin/master`.
     - If `master` exists locally, checkout `master` and set upstream to `origin/master`.
     - If it does not exist locally, create it from `origin/master` and set upstream.
- After checkout, print the current branch and tracking info.

Execute only git commands.


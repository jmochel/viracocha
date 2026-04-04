---
description: Capture current work into a single commit (conventional when possible)
allowed-tools: Bash(git status:*), Bash(git diff:*), Bash(git branch:*), Bash(git add:*), Bash(git commit:*), Bash(git push:*)
---

## Context

- Current branch: !`git branch --show-current`
- Status (short): !`git status -sb`
- Working tree status: !`git status --porcelain`
- Diff vs HEAD: !`git diff HEAD`

## Your task

Create ONE commit that captures the current local work.

### Preconditions (fail fast)
1. Current branch MUST NOT be `master`.
   - If on `master`, STOP with error:
     "You are on master. Use /wf.create or /wf.create.conv first."
2. There MUST be uncommitted changes (staged or unstaged or untracked).
   - If `git status --porcelain` is empty, STOP with warning:
     "No uncommitted changes found. Nothing to capture."

### Commit message selection
Let <branch> be the current branch name.

A) If <branch> matches this *conventional branch* pattern:
   ^(feat|fix|refactor|chore|docs|test)\/([a-z0-9-]+)-(.+)$

   Then generate a Conventional Commit message according to `051-conventional-commits.mdc` using:

B) Otherwise (non-conventional branch name):
   Infer the best commit subject from the diff:
   - Use Conventional Commit format anyway with type inferred:
     - docs if only docs/markdown changes
     - test if only test changes
     - chore if build/tooling/config changes dominate
     - refactor if code structure changes without behavior changes
     - fix if bug/defect behavior is addressed
     - feat if new behavior is introduced
   - scope:
     - infer from dominant module/package/directory affected; if unclear use "core"
   - description:
     - summarize the change in one line, imperative

### Execution
1. Stage all changes (including new files).
2. Create exactly ONE commit with the chosen message.
3. Push the current branch to origin.
   - If the branch has no upstream, set it on push.

Constraints:
- Do NOT create a PR.
- Do NOT amend.
- Execute only git tool calls.

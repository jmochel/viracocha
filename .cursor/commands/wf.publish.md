---
description: Publish current branch (push + create GitHub PR)
allowed-tools: Bash(git status:*), Bash(git branch:*), Bash(git log:*), Bash(git push:*), Bash(gh pr create:*), Bash(gh pr view:*)
---

## Context

- Current branch: !`git branch --show-current`
- Status: !`git status -sb`
- Commits since master: !`git log --oneline master..HEAD`
- Commit subjects since master: !`git log --pretty=format:%s master..HEAD`

## Your task

Publish the current work branch by pushing it and opening a PR.

### Preconditions (fail fast)
1. Current branch MUST NOT be `master`.
   - If on `master`, STOP with error:
     "You are on master. Use /wf.create or /wf.create.conv first."
2. There MUST be at least one commit on this branch that is not on master.
   - If `git log --oneline master..HEAD` is empty, STOP with error:
     "No commits found on this branch. Use /wf.capture (or commit manually) before /wf.publish."

### Actions
1. Push the current branch to origin.
   - If the branch has no upstream, set it on push.

2. Create a PR with:
   - base branch: `master`
   - title: the latest commit subject (`git log -1 --pretty=%s`)
   - body: a concise summary generated from ALL commit subjects in `master..HEAD`
     - Use a short paragraph summary (1–2 sentences)
     - Then a bullet list of commit subjects (deduplicated if repetitive)

3. Create the PR using GitHub CLI (`gh pr create`).

4. Return the PR URL (and nothing else):
   - Use `gh pr view --json url -q .url`

Constraints:
- Do NOT merge the PR.
- Do NOT modify commits.
- Execute only allowed tools.

---
description: Show open GitHub PRs authored by the current user
allowed-tools: Bash(gh pr list:*)
---

## Context

- Open PRs authored by me: !`gh pr list --author @me --state open --json number,title,headRefName,baseRefName,updatedAt,url`

## Your task

Check whether the current user has any unmerged PRs in this repository.

Rules:
1. Query GitHub for PRs:
   - Author = current authenticated user
   - State = open
2. If no PRs are found:
   - Report: "No open PRs authored by you in this repo."
3. If PRs are found:
   - List each PR with:
     - PR number
     - Title
     - Source branch → target branch
     - URL
     - Last updated time
4. Do NOT create, update, close, or merge any PRs.
5. Do NOT modify local git state.

Output only the results of the query.

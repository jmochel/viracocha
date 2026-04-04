# Deferred Items — Phase 02: publishers-and-patterns

## Pre-existing Test Failure (out of scope for Plan 02-02)

**FreemarkerVariableExtractorTest.skipsHiddenFilesAndDirectories** (line 82)

- File: `src/test/java/org/saltations/pattern/FreemarkerVariableExtractorTest.java`
- Failure: `Must NOT contain 'secret' from hidden dir ==> expected: <false> but was: <true>`
- Confirmed pre-existing: failure was present before Plan 02-02 changes (verified via `git stash` test run)
- Cause: `FreemarkerVariableExtractor` does not yet skip hidden files/directories when scanning
- Fix: Plan 02-03 (pattern commands) should include hidden-dir filtering in `FreemarkerVariableExtractor`
- Priority: Medium — functional correctness issue but not blocking publisher commands

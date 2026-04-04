# External Integrations

**Analysis Date:** 2026-03-27

## APIs & External Services

**Not detected** - This is a standalone CLI application with no external API integrations. The application does not call out to any third-party services.

## Data Storage

**Databases:**
- Not detected - Application does not use database integrations

**File Storage:**
- Local filesystem only - Application can read/write files locally if implemented in command logic

**Caching:**
- None - No caching framework is configured

## Authentication & Identity

**Auth Provider:**
- Not applicable - CLI tool with no authentication system
- No OAuth, API key, or credential management framework present

## Monitoring & Observability

**Error Tracking:**
- Not detected - No error tracking services (Sentry, DataDog, etc.) integrated

**Logs:**
- Logback - Logging framework configured via `logback-classic` dependency
- Configuration: Default Logback configuration (standard classpath scanning)
- Output: Console and/or file-based logging as configured by Logback

## CI/CD & Deployment

**Hosting:**
- Not applicable - Deployed as standalone JAR executable
- Runs as command-line tool on local machine

**CI Pipeline:**
- GitHub Actions - Workflow defined in `.github/workflows/maven.yml`
  - Triggers: Push to main branch, Pull requests to main branch
  - Build steps: JDK 21 setup, Maven verify
  - Cache: Maven dependency cache enabled
  - Distribution: Temurin

## Environment Configuration

**Required env vars:**
- Not detected - Application does not require environment variables
- Configuration via `application.yml` at `/home/jmochel/pers/viracocha/src/main/resources/application.yml`

**Secrets location:**
- Not applicable - No secrets management needed for this CLI tool

## Webhooks & Callbacks

**Incoming:**
- None - Not a web service with HTTP endpoints

**Outgoing:**
- None - No webhook integrations

## Network Requirements

**External Calls:**
- None - Application is fully self-contained
- No HTTP client calls detected in source code

---

*Integration audit: 2026-03-27*

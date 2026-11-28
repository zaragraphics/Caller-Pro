# Caller-Pro

Android caller and call-recording application.

## Current development build

- Branch: `batch-1-recording-engine`
- Build: Android Debug APK via GitHub Actions
- Workflow: `.github/workflows/android.yml`
- Output artifact: `Caller-Pro-debug`

Builds run automatically when this development branch is updated, or manually through the workflow dispatch action.

## Build locally

```bash
gradle assembleDebug --stacktrace
```

# Assay

A CLI tool that automatically generates and runs tests against an
existing Kotlin codebase, then emits a report of what it found.

The tool will be focused on property based testing for the first version.

## Status

**!Early development!** 

Check [docs/Architecture.md](https://github.com/DanOzb/Assay/blob/main/Docs/Architecture.md) for current progress

## What it does

Assay is a *bug-hunting* tool. You point it at an existing
Gradle project and it runs in the background and produces a report.

## How it works
 
```
target repo → Ingestion → Discovery/Classify → Synthesis (LLM generated tests)
            → Execution → Triage/Scoring → Reporting
```

## Tech stack

- **Language / runtime:** Kotlin on the JVM
- **CLI:** [Clikt](https://ajalt.github.io/clikt/)
- **Source analysis:** Kotlin compiler PSI & [Analysis API](https://kotlin.github.io/analysis-api/index_md.html)
- **Test framework (for generated tests and for project code):** [Kotest](https://kotest.io/)

## Prerequisites

Coming soon

## Usage

Coming soon

## License

Coming soon

# Logs 

## [2026-06-25] Pivot to Invariant Generation via Assay (PR #4)
**This is a log after a change** 
The parsing and pbt generating phase was meant to go like this: 
``` 
1. Function semantics gathered (no function body)
2. Ollama model recieves semantics and generates property based tests 

```
During PR #4 I instead changed what the models did. 
Now the parsing and pbt generation will go like this: 
```
1. Function semantics gathered 
2. Ollama model recieves semantics and generates invariants 
3. Assay generates tests from invariants selected

```
Reason: Test generation on models can be flaky, so we want models to focus on invariant generation
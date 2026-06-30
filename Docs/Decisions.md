# Logs 

## [2026-06-30] Pivot from JSON schema to free text for pbt generation (PR #13)
property test generation with the previously created schema was a fail. 
The local model used (Qwen3:14b) struggled to output useful Json blocks. 
The reason for this is that ollama models can't have thinking on while 
also enforcing a schema, which forced the model to both make bad decisions. 
The project had to pivot to free text + thinking on.

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
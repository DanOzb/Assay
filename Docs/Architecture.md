# System architecture
Doc for visualizing current architecture 

## Current Overview

Analysis API parser -> Pbt Generator
Pbt Generator -> Ktor + Ollama models
Ktor + Ollama models -> Invariant results
Invariant results -> printed kotests

## Components 

### Parsing
* Parses TOP_LEVEL functions in a kotlin project and returns a list of parsed functions: 
```
  ParsedFunction(
    val name: String,
    val fullName: String,
    val receiver: String?,
    val params: List<ParsedParam>,
    val returnType: String,
    val visibility: String,
    val docs: String?,
    val annotations: List<AnnotationModel>,
    val body: String?,
    val isSuspend: Boolean,
    )
```    
### Pbt generation
* Talks to the default ollama model for every parsed function. 
* Prompts model to generate invariants for property based testing 

```
  InvariantPlan(
    val decision: Decision,
    val skipReason: String? = null,
    val invariants: List<Invariant>? = null
)   

  Invariant(
    val kind: String,
    val testName: String,
    @Serializable(with = FlexibleStringList::class)
    val args: List<String> = emptyList(),
    val value: String? = null,
    val predicate: String? = null,
    val reference: String? = null,
    val code: String? = null,
    val preconditions: List<Precondition> = emptyList(),
)
```

### Client
* Only one client: Ollama, with an application content type as JSON

### test generation 
* first version of test generation developed for 10 invariants + custom code block

### Utilities
* Llm text output to JSON decoding 
* String to list serializer in case llm outputs args as one string
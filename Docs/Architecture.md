# System architecture
Doc for visualizing current architecture 

## Current Overview

Analysis API parser -> Pbt Generator
Pbt Generator -> Ktor + Ollama models
Ktor + Ollama models -> Invariant results

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

### Client
* Only one client: Ollama, with an application content type as JSON

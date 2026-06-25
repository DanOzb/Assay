# System architecture
MD file for visualizing current architecture 

## Current Overview

Analysis API parser -> Pbt Generator
Pbt Generator -> Ktor + Ollama models
Ktor + Ollama models -> Invariant results

## Components 

### Parsing
* Currently parses TOP_LEVEL functions in a kotlin project and returns a list of parsed functions: 
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
    )
```    
### Pbt generation
* Currently talks to the default ollama model for every parsed function. 
* prompts models to generate invariants for property based testing 

### Client
* Currently only one client: Ollama, with an application content type as JSON

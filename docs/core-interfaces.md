# Core Interfaces

## Overview

The Pipeline Doctor Plugin Core provides the foundational interfaces and contracts that extensions implement. The core focuses on:

- Extension point definitions
- Common data models
- Plugin lifecycle management
- Integration contracts

## Dependency Architecture

**IMPORTANT**: Each plugin depends ONLY on the core plugin and should NOT depend on each other.

```
pipeline-doctor-pattern-plugin → pipeline-doctor-core-plugin
pipeline-doctor-llm-plugin     → pipeline-doctor-core-plugin  
pipeline-doctor-xxx-plugin     → pipeline-doctor-core-plugin

❌ Plugins should NOT depend on each other:
pipeline-doctor-pattern-plugin ❌→ pipeline-doctor-llm-plugin
```

This ensures:
- Clean separation of concerns
- Independent development and deployment
- No circular dependencies
- Easier testing and maintenance

## Core Interfaces

### DiagnosticProvider Interface

```java
public interface DiagnosticProvider {
    /**
     * Analyze build logs and return diagnostic results
     */
    List<DiagnosticResult> analyze(BuildContext context);
    
    /**
     * Get the provider's unique identifier
     */
    String getProviderId();
    
    /**
     * Get supported categories for this provider
     */
    Set<String> getSupportedCategories();
}
```

### DiagnosticResult Model

```java
public class DiagnosticResult {
    private final String id;
    private final String category;
    private final Severity severity;
    private final String summary;
    private final String description;
    private final List<Solution> solutions;
    private final Map<String, Object> metadata;
    
    public enum Severity {
        CRITICAL, HIGH, MEDIUM, LOW
    }
}
```

### Solution Model

```java
public class Solution {
    private final String id;
    private final String title;
    private final String description;
    private final List<ActionStep> steps;
    private final int priority;
    private final Map<String, String> examples;
}

public class ActionStep {
    private final String description;
    private final String command;
    private final boolean optional;
}
```

### BuildContext Interface

```java
public interface BuildContext {
    /**
     * Get the raw build log content
     */
    String getBuildLog();
    
    /**
     * Get build environment information
     */
    Map<String, String> getEnvironment();
    
    /**
     * Get build metadata (job name, build number, etc.)
     */
    BuildMetadata getMetadata();
}
```

### Extension Registry

```java
public interface ExtensionRegistry {
    /**
     * Register a diagnostic provider
     */
    void registerProvider(DiagnosticProvider provider);
    
    /**
     * Get all registered providers
     */
    List<DiagnosticProvider> getProviders();
    
    /**
     * Get providers for specific category
     */
    List<DiagnosticProvider> getProviders(String category);
}
```

## Extension Points

### 1. Diagnostic Providers
Extensions can implement `DiagnosticProvider` to add new analysis capabilities:
- Pattern-based analyzers
- LLM-powered analyzers  
- Rule-based analyzers
- Custom integrations

### 2. Solution Renderers
Extensions can provide custom solution rendering:
- Jenkinsfile generation
- Configuration templates
- Interactive wizards

### 3. Result Processors
Extensions can process diagnostic results:
- Filtering and ranking
- Aggregation and correlation
- External integrations

## Plugin Lifecycle

### Initialization
1. Core loads extension definitions
2. Extensions register their providers
3. Core validates and activates providers

### Analysis Flow
1. Core receives build context
2. Core invokes all registered providers
3. Results are collected and processed
4. Final report is generated

### Configuration
Extensions are configured through:
- Jenkins global configuration
- Job-level configuration
- Extension-specific settings

## Integration Contracts

### Jenkins Integration
```java
public interface JenkinsIntegration {
    /**
     * Get Jenkins build information
     */
    BuildContext createBuildContext(Run<?, ?> build);
    
    /**
     * Display results in Jenkins UI
     */
    void displayResults(Run<?, ?> build, List<DiagnosticResult> results);
}
```

### MCP Integration
```java
public interface MCPIntegration {
    /**
     * Expose diagnostic capabilities via MCP
     */
    void registerMCPTools();
    
    /**
     * Handle MCP diagnostic requests
     */
    MCPResponse handleDiagnosticRequest(MCPRequest request);
}
```
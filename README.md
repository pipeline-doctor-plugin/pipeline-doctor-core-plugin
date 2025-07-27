# Pipeline Doctor Core Plugin

The core plugin that provides foundational interfaces and extension points for the Pipeline Doctor diagnostic system.

## Overview

This plugin establishes the base architecture for diagnosing Jenkins pipeline build failures. It provides:

- **Core Interfaces**: `DiagnosticProvider`, `DiagnosticResult`, `Solution`, etc.
- **Extension Registry**: Automatic discovery and management of diagnostic providers
- **Build Integration**: Automatic analysis of failed builds
- **UI Components**: Display diagnostic results in Jenkins build pages

## Architecture

The core plugin follows a clean dependency model:

```
pipeline-doctor-pattern-plugin → pipeline-doctor-core-plugin
pipeline-doctor-llm-plugin     → pipeline-doctor-core-plugin  
pipeline-doctor-xxx-plugin     → pipeline-doctor-core-plugin
```

**Key Principle**: Each extension plugin depends ONLY on the core plugin, never on each other.

## Key Components

### Core Interfaces

- **`DiagnosticProvider`**: Extension point for diagnostic analysis
- **`DiagnosticResult`**: Represents an identified issue with solutions
- **`Solution`**: Contains steps to resolve an issue
- **`BuildContext`**: Provides access to build logs and metadata

### Registry System

- **`DiagnosticRegistry`**: Discovers and manages all diagnostic providers
- Auto-discovery of providers via Jenkins extension mechanism
- Priority-based execution ordering
- Category-based filtering

### Jenkins Integration

- **`DiagnosticBuildListener`**: Triggers analysis on build completion
- **`DiagnosticAction`**: Stores and displays results in build UI
- **`JenkinsBuildContext`**: Extracts build information for analysis

## Usage

### For Extension Developers

Implement the `DiagnosticProvider` interface:

```java
@Extension
public class MyDiagnosticProvider implements DiagnosticProvider {
    
    @Override
    public List<DiagnosticResult> analyze(BuildContext context) {
        // Analyze build logs and return results
    }
    
    @Override
    public String getProviderId() {
        return "my-provider";
    }
    
    @Override
    public String getProviderName() {
        return "My Diagnostic Provider";
    }
    
    @Override
    public Set<String> getSupportedCategories() {
        return Set.of("build-tool", "test");
    }
}
```

### For Users

1. Install the core plugin
2. Install desired extension plugins (e.g., pattern-plugin, llm-plugin)
3. Configure providers in Jenkins global configuration
4. Run builds - analysis happens automatically on failures

## Building

```bash
mvn clean package
```

## Testing

```bash
mvn test
```

## Dependencies

- Jenkins 2.387.3+
- Java 11+
- Workflow plugins for pipeline support

## Extension Points

The core plugin provides these extension points for other plugins:

1. **`DiagnosticProvider`**: Add new analysis capabilities
2. **Result Processors**: Process and filter diagnostic results
3. **UI Renderers**: Custom display of results

## Configuration

The plugin automatically discovers all registered `DiagnosticProvider` implementations. No additional configuration is required for basic operation.

For advanced configuration, see individual extension plugin documentation.
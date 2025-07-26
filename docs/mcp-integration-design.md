# MCP Integration Design for Pipeline Doctor Plugin

## Overview

The Pipeline Doctor Plugin uses a hybrid approach:
1. **Built-in Pattern Library**: Fast, regex-based matching for well-known issues
2. **MCP Protocol Support**: Extensible analysis for user-specific issues via Model Context Protocol

## Architecture

### Core Components

```
┌─────────────────────────────────────────────────────────────┐
│                   Pipeline Doctor Plugin                      │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────┐    ┌──────────────────────────┐   │
│  │   Pattern Matcher    │    │    MCP Client Handler    │   │
│  │  (Built-in Library)  │    │   (User-specific Issues) │   │
│  └──────────┬──────────┘    └────────────┬─────────────┘   │
│             │                             │                   │
│  ┌──────────▼─────────────────────────────▼───────────┐     │
│  │              Diagnostic Aggregator                  │     │
│  │    (Combines results from both sources)            │     │
│  └─────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                               │
                               ▼
                    ┌──────────────────┐
                    │   MCP Server      │
                    │ (User-configured) │
                    └──────────────────┘
```

### MCP Integration Flow

1. **Build Analysis Request**
   - Plugin extracts build logs and metadata
   - Runs built-in pattern matching first
   - If MCP is configured, sends analysis request

2. **MCP Communication**
   ```java
   // Pseudo-code for MCP interaction
   MCPClient client = new MCPClient(jenkinsConfig.getMCPServerUrl());
   
   AnalysisRequest request = AnalysisRequest.builder()
       .buildLog(build.getLog())
       .jobName(build.getJob().getName())
       .buildNumber(build.getNumber())
       .patternMatchResults(patternResults)
       .build();
   
   MCPResponse response = client.analyze(request);
   ```

3. **Result Aggregation**
   - Combine pattern-based and MCP-based findings
   - De-duplicate similar issues
   - Rank solutions by confidence

## MCP Protocol Implementation

### Supported MCP Tools

1. **analyze_build_log**
   - Input: Build log content, job metadata
   - Output: Diagnosed issues and solutions

2. **get_historical_context**
   - Input: Job name, issue type
   - Output: Previous occurrences and resolutions

3. **suggest_prevention**
   - Input: Recurring issue pattern
   - Output: Preventive measures

### Message Format

```json
{
  "method": "tools/call",
  "params": {
    "name": "analyze_build_log",
    "arguments": {
      "log_content": "...",
      "job_name": "my-app-build",
      "build_number": 123,
      "known_issues": [
        {
          "type": "NETWORK_TIMEOUT",
          "confidence": 0.95
        }
      ]
    }
  }
}
```

## Jenkins Configuration

### System Configuration Page

```java
@Extension
public class PipelineDoctorGlobalConfig extends GlobalConfiguration {
    private String mcpServerUrl;
    private String mcpApiKey;
    private boolean mcpEnabled;
    private int mcpTimeout = 30; // seconds
    
    // Getters/setters and form validation
}
```

### Per-Job Configuration

- Option to disable MCP analysis for specific jobs
- Custom MCP server override per folder/job
- Timeout adjustments for long-running analysis

## Security Considerations

1. **Authentication**
   - Support for API keys
   - OAuth2 integration (future)
   - Jenkins credential store integration

2. **Data Privacy**
   - Option to redact sensitive data before sending to MCP
   - Configurable log sanitization rules
   - Audit logging of MCP requests

3. **Network Security**
   - HTTPS only for MCP communication
   - Proxy support via Jenkins proxy configuration
   - Connection pooling and retry logic

## Performance Optimization

1. **Asynchronous Processing**
   - Non-blocking MCP calls
   - Continue with pattern matching while waiting for MCP

2. **Caching**
   - Cache MCP responses for identical issues
   - Configurable cache TTL

3. **Graceful Degradation**
   - If MCP fails, fall back to pattern-only analysis
   - Timeout handling to prevent build delays

## Example MCP Server Implementation

Users can implement their own MCP server to:
- Connect to their specific monitoring tools
- Apply organization-specific knowledge
- Integrate with their ML models
- Access their internal documentation

```python
# Example MCP server endpoint
@app.post("/analyze_build_log")
async def analyze_build_log(request: AnalysisRequest):
    # Custom analysis logic
    issues = detect_custom_issues(request.log_content)
    solutions = generate_solutions(issues, request.job_name)
    
    return {
        "issues": issues,
        "solutions": solutions,
        "confidence": calculate_confidence(issues)
    }
```

## Testing Strategy

1. **Mock MCP Server**
   - Test various response scenarios
   - Error handling and timeouts
   - Performance under load

2. **Integration Tests**
   - Real MCP server connection
   - End-to-end build analysis
   - Configuration changes

3. **Compatibility Testing**
   - Different MCP server implementations
   - Various Jenkins versions
   - Network conditions
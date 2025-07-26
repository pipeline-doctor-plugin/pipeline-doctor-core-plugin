# Issue Analysis Strategy - Sequence Diagram

## Overview
This diagram shows how the Pipeline Doctor Plugin analyzes build issues using both built-in patterns and optional MCP server integration.

## Sequence Diagram

```mermaid
sequenceDiagram
    participant User
    participant Jenkins
    participant Plugin as Pipeline Doctor Plugin
    participant PM as Pattern Matcher
    participant MCP as MCP Client
    participant MCPS as MCP Server
    participant LLM as LLM API Client
    participant LLMS as LLM Service<br/>(OpenAI/Claude/Custom)
    participant Agg as Diagnostic Aggregator
    participant UI as Jenkins UI

    User->>Jenkins: Trigger Build
    Jenkins->>Jenkins: Execute Pipeline
    
    alt Build Fails or Has Issues
        Jenkins->>Plugin: Build Complete Event
        Plugin->>Plugin: Extract Build Log & Metadata
        
        %% Pattern Matching (Always Runs)
        Plugin->>PM: Analyze with Built-in Patterns
        PM->>PM: Apply Regex Patterns
        PM->>PM: Categorize Issues
        PM-->>Plugin: Pattern Match Results
        
        %% MCP Analysis (Optional)
        alt MCP Enabled in Config
            Plugin->>MCP: Check MCP Configuration
            MCP->>MCP: Validate Server Settings
            
            Plugin->>MCP: Prepare Analysis Request
            Note over MCP: Include:<br/>- Build Log<br/>- Job Metadata<br/>- Pattern Results
            
            MCP->>MCPS: Send Analysis Request
            MCPS->>MCPS: Custom Analysis Logic
            Note over MCPS: - Apply org-specific rules<br/>- Use ML models<br/>- Check internal docs
            MCPS-->>MCP: Analysis Response
            MCP-->>Plugin: MCP Results
        else MCP Disabled
            Note over Plugin: Skip MCP Analysis
        end
        
        %% LLM Enhancement (Optional - runs after pattern and/or MCP)
        alt LLM API Enabled
            Plugin->>LLM: Check LLM Configuration
            LLM->>LLM: Validate API Settings
            
            Plugin->>LLM: Prepare Enhancement Request
            Note over LLM: Include:<br/>- Pattern Results<br/>- MCP Results (if any)<br/>- Build Context<br/>- Log Excerpts
            
            LLM->>LLMS: Send to LLM API
            Note over LLMS: - Generate detailed explanations<br/>- Create step-by-step solutions<br/>- Add preventive measures<br/>- Prioritize fixes by impact
            LLMS-->>LLM: Enhanced Analysis
            LLM-->>Plugin: LLM Enhanced Results
        else LLM API Disabled
            Note over Plugin: Skip LLM Enhancement
        end
        
        %% Result Aggregation
        Plugin->>Agg: Combine All Results
        Note over Agg: Merge:<br/>- Pattern matches<br/>- MCP insights<br/>- LLM enhancements
        Agg->>Agg: Merge Pattern + MCP Findings
        Agg->>Agg: De-duplicate Issues
        Agg->>Agg: Rank Solutions by Confidence
        Agg-->>Plugin: Aggregated Diagnostics
        
        %% Store and Display Results
        Plugin->>Plugin: Store Diagnostic Report
        Plugin->>UI: Update Build Page
        UI->>UI: Display Diagnostic Widget
    end
    
    User->>UI: View Build Results
    UI-->>User: Show Diagnostics & Solutions
    
    opt User Reviews Solutions
        User->>UI: Click "Show Detailed Explanation"
        UI-->>User: Display LLM-Enhanced Details
    end
```

## Analysis Flow Details

### 1. Pattern Matching Phase (< 10ms)
- **Always executes** regardless of MCP configuration
- Uses pre-compiled regex patterns
- Handles common issues like:
  - Network timeouts
  - Docker registry errors
  - Permission failures
  - Compilation errors
  - Test failures

### 2. MCP Analysis Phase (1-5s)
- **Only runs if** MCP is enabled and configured
- Sends comprehensive context to MCP server
- Allows for:
  - Organization-specific pattern recognition
  - Complex multi-line log analysis
  - Historical context consideration
  - Custom ML model integration

### 3. LLM Enhancement Phase (2-10s)
- **Optional step** that runs after pattern matching and/or MCP analysis
- Can use OpenAI, Claude, or custom LLM endpoints
- Enhances results by:
  - Providing detailed explanations
  - Contextualizing solutions for specific environments
  - Generating step-by-step fix instructions
  - Prioritizing solutions based on context
  - Adding preventive recommendations

### 4. Result Aggregation
- Combines findings from all sources (Pattern, MCP, LLM)
- Prioritizes by confidence score
- Removes duplicate diagnoses
- Presents unified solution set with enhanced explanations

## Configuration Decision Points

```mermaid
flowchart TD
    A[Build Complete] --> B[Pattern Matching<br/>Always Runs]
    B --> C[Pattern Results]
    
    C --> D{MCP Enabled<br/>Globally?}
    D -->|No| H{LLM API<br/>Enabled?}
    D -->|Yes| E{MCP Enabled<br/>for Job?}
    E -->|No| H
    E -->|Yes| F{MCP Server<br/>Reachable?}
    F -->|No| G[Log MCP Warning]
    G --> H
    F -->|Yes| I[Run MCP Analysis]
    I --> J[MCP Results]
    J --> H
    
    H -->|No| M[Generate Report]
    H -->|Yes| K{LLM Provider<br/>Configured?}
    K -->|No| L[Log Config Error]
    L --> M
    K -->|Yes| N{LLM API<br/>Reachable?}
    N -->|No| O[Log LLM Warning]
    O --> M
    N -->|Yes| P{Budget<br/>Available?}
    P -->|No| Q[Log Budget Warning]
    Q --> M
    P -->|Yes| R[Enhance with LLM]
    R --> S[LLM Enhanced Results]
    S --> M
    
    style B fill:#90EE90
    style I fill:#87CEEB
    style R fill:#DDA0DD
```

### Configuration Levels

1. **Global Settings** (Jenkins System Configuration)
   - MCP Server URL and credentials
   - LLM Provider selection (OpenAI/Claude/Custom)
   - LLM API keys and endpoints
   - Default enable/disable for new jobs
   - Budget limits and rate limiting

2. **Folder-Level Settings**
   - Override global MCP/LLM settings
   - Apply to all jobs in folder
   - Useful for team-specific configurations

3. **Job-Level Settings**
   - Final override for specific jobs
   - Enable/disable MCP analysis
   - Enable/disable LLM enhancement
   - Custom timeout values

### Decision Logic Examples

```java
public class AnalysisDecisionMaker {
    public AnalysisStrategy determineStrategy(Job job) {
        AnalysisStrategy strategy = new AnalysisStrategy();
        
        // Pattern matching always enabled
        strategy.setPatternMatchingEnabled(true);
        
        // MCP decision
        if (globalConfig.isMcpEnabled() && 
            !job.getProperty(DisableMCPProperty.class).isDisabled() &&
            mcpClient.isReachable()) {
            strategy.setMcpEnabled(true);
        }
        
        // LLM decision
        if (globalConfig.isLlmEnabled() && 
            !job.getProperty(DisableLLMProperty.class).isDisabled() &&
            llmClient.isConfigured() &&
            budgetManager.hasRemainingBudget(job)) {
            strategy.setLlmEnabled(true);
        }
        
        return strategy;
    }
}
```

## Error Handling

```mermaid
sequenceDiagram
    participant Plugin
    participant MCP as MCP Client
    participant MCPS as MCP Server
    
    Plugin->>MCP: Send Analysis Request
    
    alt Timeout (30s default)
        MCP--xPlugin: Timeout Error
        Plugin->>Plugin: Log MCP Timeout
        Plugin->>Plugin: Continue with Pattern Results Only
    else Connection Error
        MCP--xPlugin: Connection Failed
        Plugin->>Plugin: Log Connection Error
        Plugin->>Plugin: Continue with Pattern Results Only
    else Invalid Response
        MCPS-->>MCP: Malformed Response
        MCP--xPlugin: Parse Error
        Plugin->>Plugin: Log Parse Error
        Plugin->>Plugin: Continue with Pattern Results Only
    else Success
        MCPS-->>MCP: Valid Response
        MCP-->>Plugin: MCP Results
    end
    
    %% LLM Error Handling
    alt LLM Enhancement Enabled
        Plugin->>LLM: Send Enhancement Request
        
        alt LLM Timeout (10s default)
            LLM--xPlugin: Timeout Error
            Plugin->>Plugin: Log LLM Timeout
            Plugin->>Plugin: Continue without LLM Enhancement
        else LLM API Error
            LLMS-->>LLM: API Error (rate limit, auth, etc)
            LLM--xPlugin: API Error
            Plugin->>Plugin: Log API Error
            Plugin->>Plugin: Continue without LLM Enhancement
        else Success
            LLMS-->>LLM: Enhanced Analysis
            LLM-->>Plugin: LLM Results
        end
    end
    
    Plugin->>Plugin: Merge All Available Results
```

## Performance Characteristics

| Component | Latency | Reliability |
|-----------|---------|-------------|
| Pattern Matching | < 10ms | 99.9% (local) |
| MCP Analysis | 1-5s | 95% (network dependent) |
| LLM Enhancement | 2-10s | 90% (API dependent) |
| Result Aggregation | < 5ms | 99.9% |
| Total (Pattern Only) | < 20ms | 99.9% |
| Total (Pattern + MCP) | 1-5s | 95% |
| Total (Pattern + LLM) | 2-10s | 90% |
| Total (All Three) | 3-15s | 85% |

## Key Benefits

1. **Fast Baseline**: Pattern matching provides immediate results
2. **Extensibility**: MCP allows custom analysis without plugin updates
3. **Intelligence**: Optional LLM enhancement for detailed explanations
4. **Graceful Degradation**: Works even if MCP or LLM fails
5. **User Control**: Per-job MCP and LLM enable/disable
6. **Privacy**: Sensitive data can stay on-premise with local MCP server
7. **Flexibility**: Choose between speed (pattern-only) or intelligence (with LLM)

## Example Analysis Flow

```mermaid
flowchart LR
    A[Build Log] --> B[Pattern Matcher]
    B --> C{Pattern Found?}
    C -->|Yes| D[Basic Diagnosis]
    C -->|No| E[Unknown Issue]
    
    D --> F{MCP Enabled?}
    E --> F
    F -->|Yes| G[MCP Analysis]
    F -->|No| H{LLM Enabled?}
    
    G --> I[Enhanced Diagnosis]
    I --> H
    
    H -->|Yes| J[LLM Enhancement]
    H -->|No| K[Final Report]
    
    J --> L[Detailed Solutions]
    L --> K
    
    style B fill:#90EE90
    style G fill:#87CEEB
    style J fill:#DDA0DD
```

## Configuration Examples

### Scenario 1: Pattern-Only (Fastest)
```yaml
MCP: Disabled
LLM: Disabled
Result: Basic pattern matching only (~20ms)
Use Case: High-volume builds, cost-sensitive
```

### Scenario 2: Pattern + MCP
```yaml
MCP: Enabled (custom server)
LLM: Disabled
Result: Org-specific analysis (~1-5s)
Use Case: Complex internal systems
```

### Scenario 3: Pattern + LLM
```yaml
MCP: Disabled
LLM: Enabled (OpenAI GPT-4)
Result: AI-enhanced explanations (~2-10s)
Use Case: Developer-friendly detailed guides
```

### Scenario 4: Full Stack
```yaml
MCP: Enabled
LLM: Enabled
Result: Complete analysis (~3-15s)
Use Case: Critical builds, maximum insight
```

## LLM Integration Examples

### Sample LLM Enhancement Flow

```mermaid
stateDiagram-v2
    [*] --> PatternMatch: Build Failed
    PatternMatch --> MCPAnalysis: Pattern Found
    PatternMatch --> LLMDirect: No Pattern
    MCPAnalysis --> LLMEnhanced: MCP Complete
    LLMDirect --> Report: LLM Analysis
    LLMEnhanced --> Report: Enhanced Report
    Report --> [*]: Show to User
    
    note right of LLMDirect
        LLM can work with
        just build logs
    end note
    
    note right of LLMEnhanced
        Best results with
        all context combined
    end note
```

### OpenAI Configuration
```java
// Jenkins Global Configuration
llmProvider = "openai"
llmApiKey = "${OPENAI_API_KEY}"
llmModel = "gpt-4"
llmTemperature = 0.3
llmMaxTokens = 2000
```

### Claude (Anthropic) Configuration
```java
// Jenkins Global Configuration for Claude
llmProvider = "anthropic"
llmApiKey = "${ANTHROPIC_API_KEY}"
llmModel = "claude-3-opus-20240229"  // or claude-3-sonnet, claude-3-haiku
llmMaxTokens = 4096
llmTemperature = 0.3

// Optional: Use Claude via AWS Bedrock
llmProvider = "bedrock-claude"
llmRegion = "us-east-1"
llmModel = "anthropic.claude-3-opus-20240229-v1:0"
// Uses AWS credentials from Jenkins credential store
```

### Custom LLM Endpoint
```java
// For self-hosted or alternative LLM services
llmProvider = "custom"
llmEndpoint = "https://llm.company.com/v1/analyze"
llmApiKey = "${CUSTOM_LLM_KEY}"
llmRequestFormat = "openai-compatible" // or "custom"
```

### LLM Request Examples

#### OpenAI Format
```json
{
  "messages": [
    {
      "role": "system",
      "content": "You are a Jenkins build failure expert. Analyze the issues and provide detailed solutions."
    },
    {
      "role": "user",
      "content": "Build failed with:\nPattern matches: [DOCKER_REGISTRY_TIMEOUT, NETWORK_UNREACHABLE]\nMCP analysis: {\"root_cause\": \"Corporate proxy blocking registry\"}\nProvide detailed fix steps."
    }
  ],
  "temperature": 0.3,
  "max_tokens": 2000
}
```

#### Claude (Anthropic) Format
```json
{
  "model": "claude-3-opus-20240229",
  "max_tokens": 4096,
  "temperature": 0.3,
  "system": "You are a Jenkins build failure expert. Analyze the issues and provide detailed solutions with clear, actionable steps.",
  "messages": [
    {
      "role": "user",
      "content": "Build failed with:\nPattern matches: [DOCKER_REGISTRY_TIMEOUT, NETWORK_UNREACHABLE]\nMCP analysis: {\"root_cause\": \"Corporate proxy blocking registry\"}\n\nPlease provide:\n1. Root cause explanation\n2. Step-by-step fix\n3. Prevention measures"
    }
  ]
}
```

#### Claude via AWS Bedrock Format
```json
{
  "modelId": "anthropic.claude-3-opus-20240229-v1:0",
  "contentType": "application/json",
  "accept": "application/json",
  "body": {
    "anthropic_version": "bedrock-2023-05-31",
    "max_tokens": 4096,
    "temperature": 0.3,
    "system": "You are a Jenkins build failure expert analyzing CI/CD issues.",
    "messages": [
      {
        "role": "user",
        "content": "Analyze this Jenkins build failure and provide solutions..."
      }
    ]
  }
}
```
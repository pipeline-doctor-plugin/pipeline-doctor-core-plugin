# Jenkins Pipeline Doctor Plugin - Overview

## Purpose

The Jenkins Pipeline Doctor Plugin helps diagnose pipeline build failures and performance issues by analyzing build logs and suggesting solutions. It's designed for Jenkins administrators managing multiple tenants who face recurring issues like NetworkPolicy misconfigurations, Debian archive problems, and Docker registry errors.

## Architecture Overview

### Three-Tier Analysis System

1. **Pattern Matching (Core Plugin)**
   - Built-in regex patterns for common issues
   - <10ms response time for instant feedback
   - No external dependencies

2. **External Analysis (Extension Point)**
   - DiagnosticAnalyzer extension point
   - Provider plugins implement MCP, custom AI, etc.
   - 1-5s response time for deeper analysis

3. **Solution Enhancement (Extension Point)**
   - SolutionEnhancer extension point
   - Provider plugins add LLM-powered explanations
   - 2-10s response time for detailed guidance

### Plugin Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Jenkins Pipeline Doctor                   │
│                       (Core Plugin)                          │
├─────────────────────────────────────────────────────────────┤
│  • Pattern Matching Engine                                  │
│  • Extension Point Management                               │
│  • Result Aggregation & UI                                  │
│  • Job/Folder Configuration                                 │
└──────────────────┬──────────────────────────┬──────────────┘
                   │                          │
         DiagnosticAnalyzer          SolutionEnhancer
         Extension Point             Extension Point
                   │                          │
    ┌──────────────┴───────────┐  ┌──────────┴──────────────┐
    │   Provider Plugins       │  │   Provider Plugins      │
    ├──────────────────────────┤  ├─────────────────────────┤
    │ • pipeline-doctor-mcp    │  │ • pipeline-doctor-openai│
    │ • pipeline-doctor-custom │  │ • pipeline-doctor-claude│
    │ • pipeline-doctor-ai     │  │ • pipeline-doctor-ollama│
    └──────────────────────────┘  └─────────────────────────┘
```

## Key Features

### Built-in Pattern Library
- NetworkPolicy configuration errors
- Debian archive repository issues
- Docker registry authentication failures
- Kubernetes RBAC permission problems
- Git clone failures
- Maven/Gradle dependency resolution
- Node.js package installation errors
- Python pip timeout issues

### Extension Points

#### DiagnosticAnalyzer
- Analyze build logs with external services
- Async processing with CompletableFuture
- Per-job enable/disable configuration
- Priority-based execution order

#### SolutionEnhancer
- Enhance solutions with detailed explanations
- Support for multiple LLM providers
- Budget and rate limiting controls
- Data sanitization before external calls

### Learning & Improvement
- Admin feedback collection
- Pattern accuracy tracking
- Continuous improvement based on resolved issues
- A/B testing for patterns and prompts

## Benefits of Extension Point Architecture

1. **Modularity**: Core plugin stays lightweight and focused
2. **Security**: API keys remain in provider plugins
3. **Flexibility**: Users install only needed providers
4. **Independent Updates**: Providers update without core changes
5. **Community**: Easy for third parties to add providers
6. **Licensing**: Different licenses for different providers

## Usage Scenarios

### Basic Installation (Pattern Matching Only)
```
jenkins-plugin install pipeline-doctor
```
- Get instant feedback on common issues
- No external dependencies or API keys
- Works offline

### With MCP Analysis
```
jenkins-plugin install pipeline-doctor
jenkins-plugin install pipeline-doctor-mcp
```
- Configure MCP servers for custom analysis
- Organization-specific pattern detection
- Integration with internal tools

### With LLM Enhancement
```
jenkins-plugin install pipeline-doctor
jenkins-plugin install pipeline-doctor-openai
```
- Get detailed explanations for issues
- Step-by-step fix instructions
- Context-aware suggestions

### Full Stack
```
jenkins-plugin install pipeline-doctor
jenkins-plugin install pipeline-doctor-mcp
jenkins-plugin install pipeline-doctor-openai
jenkins-plugin install pipeline-doctor-claude
```
- Complete analysis pipeline
- Multiple provider options
- Fallback strategies

## Configuration

### Global Level
Each provider plugin manages its own configuration:
- API endpoints and credentials
- Rate limits and budgets
- Default enable/disable settings

### Job/Folder Level
Fine-grained control per job or folder:
```groovy
pipeline {
    options {
        pipelineDoctor {
            analyzers {
                mcp {
                    enabled = true
                    server = 'internal-mcp'
                }
                openai {
                    enabled = true
                    maxTokens = 1000
                }
            }
        }
    }
}
```

## Performance Characteristics

| Component | Response Time | When Used |
|-----------|--------------|-----------|
| Pattern Matching | <10ms | Always |
| MCP Analysis | 1-5s | When configured |
| LLM Enhancement | 2-10s | When enabled & budget available |

## Security & Privacy

- **Data Sanitization**: Sensitive data removed before external calls
- **Credential Management**: Jenkins credential store integration
- **Audit Logging**: All external API calls logged
- **Per-job Permissions**: Control who can configure analyzers
- **Budget Controls**: Prevent runaway costs with LLM providers

## Future Roadmap

1. **More Provider Plugins**
   - AWS Bedrock integration
   - Google Vertex AI support
   - Azure OpenAI support

2. **Enhanced Learning**
   - ML-based pattern generation
   - Cross-organization pattern sharing
   - Success rate analytics

3. **Developer Tools**
   - Pattern testing framework
   - Provider plugin generator
   - Integration test suite

4. **Enterprise Features**
   - Centralized configuration management
   - Usage analytics dashboard
   - Cost allocation by team/project
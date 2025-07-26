# LLM Integration Design for Pipeline Doctor Plugin

## Overview

The LLM integration provides an optional enhancement layer that runs after pattern matching and MCP analysis. It uses AI language models to:
- Generate detailed explanations for identified issues
- Provide context-aware solution steps
- Prioritize fixes based on the specific environment
- Add preventive recommendations

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   Pipeline Doctor Plugin                      │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │
│  │   Pattern     │    │     MCP      │    │     LLM      │  │
│  │   Matcher     │    │   Client     │    │   Client     │  │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘  │
│         │                    │                    │          │
│         ▼                    ▼                    ▼          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │           Result Enhancement Pipeline                 │   │
│  │  Pattern Results → MCP Results → LLM Enhancement     │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
                   ┌────────────────────────┐
                   │   LLM Service          │
                   │ (OpenAI/Claude/Custom) │
                   └────────────────────────┘
```

## LLM Integration Flow

### 1. Input Preparation
The plugin prepares a comprehensive context for the LLM including:
- Identified issues from pattern matching
- MCP analysis results (if available)
- Build metadata (job name, build number, duration)
- Relevant log excerpts
- Historical context (if available)

### 2. LLM Request Format

```java
public class LLMEnhancementRequest {
    private List<DiagnosticIssue> patternIssues;
    private MCPAnalysisResult mcpResult;
    private BuildContext buildContext;
    private String logExcerpt;
    private Map<String, Object> metadata;
    
    public String toPrompt() {
        return String.format(
            "Analyze these Jenkins build issues and provide detailed solutions:\n\n" +
            "Issues found:\n%s\n\n" +
            "Build context:\n%s\n\n" +
            "Relevant logs:\n%s\n\n" +
            "Please provide:\n" +
            "1. Root cause explanation\n" +
            "2. Step-by-step fix instructions\n" +
            "3. Prevention recommendations\n" +
            "4. Priority ranking of fixes",
            formatIssues(),
            formatContext(),
            logExcerpt
        );
    }
}
```

### 3. LLM Response Processing

```java
public class LLMEnhancedDiagnostic {
    private String issueId;
    private String detailedExplanation;
    private List<String> stepByStepSolution;
    private String preventionAdvice;
    private int priorityScore;
    private Map<String, String> environmentSpecificNotes;
}
```

## Configuration

### Jenkins Global Configuration

```java
@Extension
public class LLMConfiguration extends GlobalConfiguration {
    // Provider selection
    private String llmProvider; // "openai", "anthropic", "custom"
    private String llmEndpoint;
    private Secret llmApiKey;
    
    // Model settings
    private String modelName; // e.g., "gpt-4", "claude-3"
    private double temperature = 0.3; // Lower for consistent outputs
    private int maxTokens = 2000;
    private int timeout = 10; // seconds
    
    // Feature toggles
    private boolean llmEnabled = false;
    private boolean llmForPatternOnly = true; // Use LLM even without MCP
    private boolean includePreventiveAnalysis = true;
    
    // Rate limiting
    private int maxRequestsPerMinute = 10;
    private boolean cacheResponses = true;
    private int cacheTTLMinutes = 60;
}
```

### Per-Job Configuration

```groovy
pipeline {
    options {
        pipelineDoctor {
            llmEnhancement true
            llmProvider 'custom'
            llmEndpoint 'https://internal-llm.company.com/v1/chat'
            llmPromptTemplate 'security-focused' // Use predefined templates
        }
    }
}
```

## Supported LLM Providers

### 1. OpenAI Integration
```java
public class OpenAILLMClient implements LLMClient {
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    
    public LLMResponse enhance(LLMEnhancementRequest request) {
        Map<String, Object> openAIRequest = Map.of(
            "model", config.getModel(), // e.g., "gpt-4", "gpt-3.5-turbo"
            "messages", List.of(
                Map.of(
                    "role", "system",
                    "content", buildSystemPrompt()
                ),
                Map.of(
                    "role", "user",
                    "content", request.toPrompt()
                )
            ),
            "temperature", config.getTemperature(),
            "max_tokens", config.getMaxTokens()
        );
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(
                gson.toJson(openAIRequest)
            ))
            .timeout(Duration.ofSeconds(timeout))
            .build();
            
        return processResponse(httpClient.send(httpRequest));
    }
}
```

### 2. Anthropic Claude Integration
```java
public class ClaudeLLMClient implements LLMClient {
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    
    public LLMResponse enhance(LLMEnhancementRequest request) {
        // Build Claude-specific request format
        Map<String, Object> claudeRequest = Map.of(
            "model", config.getModel(), // e.g., "claude-3-opus-20240229"
            "max_tokens", config.getMaxTokens(),
            "temperature", config.getTemperature(),
            "system", buildSystemPrompt(),
            "messages", List.of(
                Map.of(
                    "role", "user",
                    "content", request.toPrompt()
                )
            )
        );
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("x-api-key", apiKey)
            .header("anthropic-version", ANTHROPIC_VERSION)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(
                gson.toJson(claudeRequest)
            ))
            .timeout(Duration.ofSeconds(timeout))
            .build();
            
        return processResponse(httpClient.send(httpRequest));
    }
}

// AWS Bedrock Claude Integration
public class BedrockClaudeLLMClient implements LLMClient {
    private final BedrockRuntimeClient bedrockClient;
    
    public LLMResponse enhance(LLMEnhancementRequest request) {
        // Prepare request for Bedrock
        Map<String, Object> bedrockBody = Map.of(
            "anthropic_version", "bedrock-2023-05-31",
            "max_tokens", config.getMaxTokens(),
            "temperature", config.getTemperature(),
            "system", buildSystemPrompt(),
            "messages", List.of(
                Map.of(
                    "role", "user",
                    "content", request.toPrompt()
                )
            )
        );
        
        InvokeModelRequest invokeRequest = InvokeModelRequest.builder()
            .modelId(config.getModel()) // e.g., "anthropic.claude-3-opus-20240229-v1:0"
            .contentType("application/json")
            .accept("application/json")
            .body(SdkBytes.fromUtf8String(gson.toJson(bedrockBody)))
            .build();
            
        InvokeModelResponse response = bedrockClient.invokeModel(invokeRequest);
        return parseBedrockResponse(response);
    }
}
```

### 3. Custom LLM Endpoints
```java
public class CustomLLMClient implements LLMClient {
    // Flexible implementation supporting various formats:
    // - OpenAI-compatible APIs
    // - Custom REST endpoints
    // - GraphQL endpoints
    // - gRPC services
}
```

## Prompt Engineering

### System Prompts by Use Case

```java
public enum PromptTemplate {
    DEFAULT(
        "You are a Jenkins build failure expert. Analyze issues and provide " +
        "clear, actionable solutions. Focus on practical fixes."
    ),
    
    SECURITY_FOCUSED(
        "You are a DevSecOps expert. Analyze build failures with security " +
        "implications. Prioritize secure solutions and highlight risks."
    ),
    
    PERFORMANCE_FOCUSED(
        "You are a build optimization expert. Focus on performance issues " +
        "and suggest optimizations for faster builds."
    ),
    
    KUBERNETES(
        "You are a Kubernetes and Jenkins expert. Focus on container, " +
        "deployment, and cluster-related issues."
    ),
    
    CLAUDE_OPTIMIZED(
        "You are Claude, an AI assistant specialized in Jenkins and CI/CD. " +
        "Provide detailed, step-by-step solutions for build failures. " +
        "Use your understanding of software development best practices to " +
        "suggest both immediate fixes and long-term improvements. " +
        "Format your response with clear sections for diagnosis, solution, and prevention."
    );
}
```

### Dynamic Prompt Construction

```java
public String buildPrompt(BuildFailure failure, PromptTemplate template) {
    StringBuilder prompt = new StringBuilder();
    
    // Add system context
    prompt.append(template.getSystemPrompt()).append("\n\n");
    
    // Add structured issue data
    prompt.append("=== BUILD FAILURE ANALYSIS ===\n");
    prompt.append("Job: ").append(failure.getJobName()).append("\n");
    prompt.append("Build: #").append(failure.getBuildNumber()).append("\n");
    prompt.append("Duration: ").append(failure.getDuration()).append("\n\n");
    
    // Add pattern matches
    if (!failure.getPatternMatches().isEmpty()) {
        prompt.append("=== DETECTED PATTERNS ===\n");
        failure.getPatternMatches().forEach(match -> 
            prompt.append("- ").append(match.getDescription()).append("\n")
        );
    }
    
    // Add MCP insights
    if (failure.getMcpAnalysis() != null) {
        prompt.append("\n=== CUSTOM ANALYSIS ===\n");
        prompt.append(failure.getMcpAnalysis().getSummary()).append("\n");
    }
    
    // Add relevant log context
    prompt.append("\n=== LOG CONTEXT ===\n");
    prompt.append(failure.getRelevantLogExcerpt()).append("\n");
    
    // Add specific instructions
    prompt.append("\n=== REQUIRED OUTPUT ===\n");
    prompt.append("1. Root cause (1-2 sentences)\n");
    prompt.append("2. Fix steps (numbered list)\n");
    prompt.append("3. Prevention (bullet points)\n");
    prompt.append("4. Priority: HIGH/MEDIUM/LOW with justification\n");
    
    return prompt.toString();
}
```

## Security and Privacy

### 1. Data Sanitization
```java
public class LogSanitizer {
    private static final Pattern[] SENSITIVE_PATTERNS = {
        Pattern.compile("password\\s*=\\s*['\"]?([^'\"\\s]+)"),
        Pattern.compile("api[_-]?key\\s*=\\s*['\"]?([^'\"\\s]+)"),
        Pattern.compile("token\\s*=\\s*['\"]?([^'\"\\s]+)"),
        Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"), // emails
        Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"), // IP addresses
    };
    
    public String sanitize(String log) {
        String sanitized = log;
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            sanitized = pattern.matcher(sanitized).replaceAll("[REDACTED]");
        }
        return sanitized;
    }
}
```

### 2. API Key Management
- Store API keys in Jenkins Credentials
- Support for HashiCorp Vault integration
- Rotation reminders and expiry tracking

### 3. Audit Logging
```java
public void logLLMRequest(LLMRequest request, LLMResponse response) {
    auditLogger.log(Level.INFO, String.format(
        "LLM Request: job=%s, build=%d, provider=%s, tokens=%d, latency=%dms",
        request.getJobName(),
        request.getBuildNumber(),
        request.getProvider(),
        response.getTokensUsed(),
        response.getLatencyMs()
    ));
}
```

## Performance Optimization

### 1. Response Caching
```java
@Component
public class LLMResponseCache {
    private final Cache<String, LLMResponse> cache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(60, TimeUnit.MINUTES)
        .build();
    
    public Optional<LLMResponse> get(String cacheKey) {
        return Optional.ofNullable(cache.getIfPresent(cacheKey));
    }
}
```

### 2. Async Processing
```java
public CompletableFuture<LLMResponse> enhanceAsync(DiagnosticReport report) {
    return CompletableFuture.supplyAsync(() -> {
        // Check cache first
        String cacheKey = report.getCacheKey();
        Optional<LLMResponse> cached = cache.get(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }
        
        // Make LLM request
        LLMResponse response = llmClient.enhance(report);
        cache.put(cacheKey, response);
        return response;
    }, llmExecutor);
}
```

### 3. Batching for Multiple Issues
```java
public class LLMBatchProcessor {
    private final int batchSize = 5;
    private final int batchTimeoutMs = 2000;
    
    public List<LLMResponse> processBatch(List<DiagnosticIssue> issues) {
        // Group similar issues for efficient processing
        Map<String, List<DiagnosticIssue>> grouped = groupBySimilarity(issues);
        
        // Process each group with a single LLM call
        return grouped.entrySet().parallelStream()
            .map(entry -> processGroup(entry.getValue()))
            .collect(Collectors.toList());
    }
}
```

## Cost Management

### 1. Token Usage Tracking
```java
@Entity
public class LLMUsageMetrics {
    private String jobName;
    private LocalDate date;
    private String provider;
    private long totalTokens;
    private long totalRequests;
    private BigDecimal estimatedCost;
}
```

### 2. Budget Controls
```java
public class LLMBudgetManager {
    private final double monthlyBudget;
    private final Map<String, Double> providerRates = Map.of(
        "openai-gpt4", 0.03, // per 1K tokens
        "openai-gpt3.5", 0.002,
        "anthropic-claude-3-opus", 0.015, // per 1K input tokens
        "anthropic-claude-3-sonnet", 0.003,
        "anthropic-claude-3-haiku", 0.00025,
        "bedrock-claude-3-opus", 0.015, // AWS Bedrock pricing
        "bedrock-claude-3-sonnet", 0.003
    );
    
    public boolean canMakeRequest(String provider, int estimatedTokens) {
        double estimatedCost = calculateCost(provider, estimatedTokens);
        double monthlySpend = getMonthlySpend();
        return (monthlySpend + estimatedCost) <= monthlyBudget;
    }
    
    // Claude models have different input/output token pricing
    public double calculateClaudeCost(String model, int inputTokens, int outputTokens) {
        double inputRate = getInputRate(model);
        double outputRate = getOutputRate(model);
        return (inputTokens * inputRate + outputTokens * outputRate) / 1000.0;
    }
}
```

## Testing Strategy

### 1. Mock LLM Responses
```java
@TestConfiguration
public class MockLLMConfig {
    @Bean
    @Primary
    public LLMClient mockLLMClient() {
        return new MockLLMClient(
            loadMockResponses("test-resources/llm-mocks.json")
        );
    }
}
```

### 2. Integration Tests
```java
@Test
public void testLLMEnhancementFlow() {
    // Given a build with pattern matches
    BuildResult build = createFailedBuild();
    DiagnosticReport report = diagnosticEngine.analyze(build);
    
    // When LLM enhancement is enabled
    LLMEnhancedReport enhanced = llmEnhancer.enhance(report);
    
    // Then solutions should be more detailed
    assertThat(enhanced.getSolutions())
        .allMatch(s -> s.getSteps().size() > 3)
        .allMatch(s -> s.hasPreventionAdvice());
}
```

### 3. Prompt Testing Framework
```yaml
# test-cases/llm-prompts.yaml
test_cases:
  - name: "Docker registry timeout"
    input:
      pattern_matches:
        - type: "DOCKER_REGISTRY_TIMEOUT"
      log_excerpt: "Error: Get https://registry.docker.io/v2/: net/http: request canceled"
    expected_output:
      must_contain:
        - "registry.docker.io"
        - "timeout"
        - "retry"
      priority: "MEDIUM"
```
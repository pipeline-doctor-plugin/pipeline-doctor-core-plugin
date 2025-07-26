# LLM Integration Strategy Pattern Design

## Overview

This document outlines the Strategy pattern implementation for LLM integration in the Pipeline Doctor Plugin. The Strategy pattern allows us to define a family of LLM providers, encapsulate each one, and make them interchangeable without modifying the main analysis logic.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    LLMEnhancementService                      │
│                    (Context / Main Logic)                     │
├─────────────────────────────────────────────────────────────┤
│  - LLMStrategy strategy                                       │
│  - LLMConfiguration config                                    │
│  - enhance(DiagnosticReport): EnhancedDiagnosticReport      │
│  - setStrategy(LLMStrategy): void                            │
└──────────────────────────┬───────────────────────────────────┘
                           │ uses
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                  <<interface>> LLMStrategy                    │
├─────────────────────────────────────────────────────────────┤
│  + enhance(DiagnosticReport): EnhancedDiagnosticReport      │
│  + isAvailable(): boolean                                    │
│  + getName(): String                                         │
│  + estimateCost(int tokens): double                         │
└─────────────────────────────────────────────────────────────┘
                           △
                           │ implements
        ┌──────────────────┼──────────────────┬───────────────┐
        │                  │                  │               │
┌───────▼────────┐ ┌───────▼────────┐ ┌──────▼────────┐ ┌────▼─────┐
│ OpenAIStrategy │ │ ClaudeStrategy │ │BedrockStrategy│ │NullStrategy│
├────────────────┤ ├────────────────┤ ├───────────────┤ ├──────────┤
│ - apiClient    │ │ - apiClient    │ │ - bedrockClient│ │          │
│ - model        │ │ - model        │ │ - modelId      │ │          │
│ - temperature  │ │ - temperature  │ │ - region       │ │          │
└────────────────┘ └────────────────┘ └───────────────┘ └──────────┘
```

## Core Components

### 1. LLMStrategy Interface

```java
public interface LLMStrategy {
    /**
     * Enhance the diagnostic report with LLM-generated insights
     */
    EnhancedDiagnosticReport enhance(DiagnosticReport report) 
        throws LLMException;
    
    /**
     * Check if this strategy is properly configured and available
     */
    boolean isAvailable();
    
    /**
     * Get the name of this LLM provider
     */
    String getName();
    
    /**
     * Estimate the cost for processing the given number of tokens
     */
    double estimateCost(int estimatedTokens);
    
    /**
     * Get the maximum context window for this provider
     */
    int getMaxContextTokens();
    
    /**
     * Validate configuration before use
     */
    void validateConfiguration() throws ConfigurationException;
}
```

### 2. LLMEnhancementService (Context)

```java
@Service
public class LLMEnhancementService {
    private LLMStrategy strategy;
    private final LLMStrategyFactory strategyFactory;
    private final LLMConfiguration config;
    private final MetricsCollector metrics;
    private final CircuitBreaker circuitBreaker;
    
    @Inject
    public LLMEnhancementService(
            LLMStrategyFactory strategyFactory,
            LLMConfiguration config,
            MetricsCollector metrics) {
        this.strategyFactory = strategyFactory;
        this.config = config;
        this.metrics = metrics;
        this.circuitBreaker = new CircuitBreaker(config);
        
        // Initialize strategy based on configuration
        this.strategy = strategyFactory.createStrategy(config.getProvider());
    }
    
    public EnhancedDiagnosticReport enhance(DiagnosticReport report) {
        // Check if LLM enhancement is enabled
        if (!config.isEnabled()) {
            return EnhancedDiagnosticReport.from(report);
        }
        
        // Circuit breaker pattern for fault tolerance
        if (!circuitBreaker.allowRequest()) {
            logger.warn("LLM enhancement skipped due to circuit breaker");
            metrics.recordSkipped("circuit_breaker");
            return EnhancedDiagnosticReport.from(report);
        }
        
        try {
            // Check strategy availability
            if (!strategy.isAvailable()) {
                logger.warn("LLM strategy {} is not available", strategy.getName());
                return EnhancedDiagnosticReport.from(report);
            }
            
            // Estimate cost and check budget
            int estimatedTokens = estimateTokens(report);
            double estimatedCost = strategy.estimateCost(estimatedTokens);
            
            if (!budgetManager.canSpend(estimatedCost)) {
                logger.warn("LLM enhancement skipped due to budget constraints");
                metrics.recordSkipped("budget");
                return EnhancedDiagnosticReport.from(report);
            }
            
            // Perform enhancement
            long startTime = System.currentTimeMillis();
            EnhancedDiagnosticReport enhanced = strategy.enhance(report);
            long duration = System.currentTimeMillis() - startTime;
            
            // Record metrics
            metrics.recordSuccess(strategy.getName(), duration, estimatedTokens);
            budgetManager.recordSpend(estimatedCost);
            circuitBreaker.recordSuccess();
            
            return enhanced;
            
        } catch (LLMException e) {
            logger.error("LLM enhancement failed", e);
            metrics.recordFailure(strategy.getName(), e.getType());
            circuitBreaker.recordFailure();
            
            // Fallback to original report
            return EnhancedDiagnosticReport.from(report);
        }
    }
    
    /**
     * Switch strategy at runtime if needed
     */
    public void setStrategy(String provider) {
        this.strategy = strategyFactory.createStrategy(provider);
        logger.info("Switched LLM strategy to: {}", provider);
    }
    
    /**
     * Get current strategy information
     */
    public LLMStrategyInfo getCurrentStrategyInfo() {
        return new LLMStrategyInfo(
            strategy.getName(),
            strategy.isAvailable(),
            strategy.getMaxContextTokens()
        );
    }
}
```

### 3. Concrete Strategy Implementations

#### OpenAI Strategy

```java
public class OpenAIStrategy implements LLMStrategy {
    private final OpenAIClient client;
    private final OpenAIConfig config;
    private final PromptBuilder promptBuilder;
    private final ResponseParser responseParser;
    
    public OpenAIStrategy(OpenAIConfig config) {
        this.config = config;
        this.client = new OpenAIClient(config.getApiKey());
        this.promptBuilder = new OpenAIPromptBuilder();
        this.responseParser = new OpenAIResponseParser();
    }
    
    @Override
    public EnhancedDiagnosticReport enhance(DiagnosticReport report) 
            throws LLMException {
        try {
            // Build the request
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(config.getModel())
                .messages(promptBuilder.buildMessages(report))
                .temperature(config.getTemperature())
                .maxTokens(config.getMaxTokens())
                .build();
            
            // Make API call
            ChatCompletionResponse response = client.createChatCompletion(request);
            
            // Parse and return enhanced report
            return responseParser.parseResponse(response, report);
            
        } catch (OpenAIException e) {
            throw new LLMException("OpenAI API error", e);
        }
    }
    
    @Override
    public boolean isAvailable() {
        return config.isConfigured() && client.testConnection();
    }
    
    @Override
    public String getName() {
        return "OpenAI-" + config.getModel();
    }
    
    @Override
    public double estimateCost(int tokens) {
        // OpenAI pricing per 1K tokens
        double rate = switch (config.getModel()) {
            case "gpt-4" -> 0.03;
            case "gpt-3.5-turbo" -> 0.002;
            default -> 0.01;
        };
        return (tokens / 1000.0) * rate;
    }
    
    @Override
    public int getMaxContextTokens() {
        return switch (config.getModel()) {
            case "gpt-4-32k" -> 32768;
            case "gpt-4" -> 8192;
            case "gpt-3.5-turbo-16k" -> 16384;
            default -> 4096;
        };
    }
}
```

#### Claude Strategy

```java
public class ClaudeStrategy implements LLMStrategy {
    private final ClaudeClient client;
    private final ClaudeConfig config;
    private final ClaudePromptBuilder promptBuilder;
    private final ClaudeResponseParser responseParser;
    
    public ClaudeStrategy(ClaudeConfig config) {
        this.config = config;
        this.client = new ClaudeClient(config.getApiKey());
        this.promptBuilder = new ClaudePromptBuilder();
        this.responseParser = new ClaudeResponseParser();
    }
    
    @Override
    public EnhancedDiagnosticReport enhance(DiagnosticReport report) 
            throws LLMException {
        try {
            // Build Claude-specific request
            MessageRequest request = MessageRequest.builder()
                .model(config.getModel())
                .system(promptBuilder.buildSystemPrompt())
                .messages(promptBuilder.buildMessages(report))
                .maxTokens(config.getMaxTokens())
                .temperature(config.getTemperature())
                .build();
            
            // Make API call
            MessageResponse response = client.createMessage(request);
            
            // Parse and return enhanced report
            return responseParser.parseResponse(response, report);
            
        } catch (AnthropicException e) {
            throw new LLMException("Claude API error", e);
        }
    }
    
    @Override
    public boolean isAvailable() {
        return config.isConfigured() && client.testConnection();
    }
    
    @Override
    public String getName() {
        return "Claude-" + config.getModel();
    }
    
    @Override
    public double estimateCost(int tokens) {
        // Claude pricing varies by model
        double inputRate = switch (config.getModel()) {
            case "claude-3-opus" -> 0.015;
            case "claude-3-sonnet" -> 0.003;
            case "claude-3-haiku" -> 0.00025;
            default -> 0.01;
        };
        // Simplified - actual implementation would track input/output separately
        return (tokens / 1000.0) * inputRate;
    }
    
    @Override
    public int getMaxContextTokens() {
        // All Claude 3 models support 200K context
        return 200000;
    }
}
```

#### Null Strategy (Fallback)

```java
public class NullLLMStrategy implements LLMStrategy {
    @Override
    public EnhancedDiagnosticReport enhance(DiagnosticReport report) {
        // Simply return the original report without enhancement
        return EnhancedDiagnosticReport.from(report);
    }
    
    @Override
    public boolean isAvailable() {
        return true; // Always available as fallback
    }
    
    @Override
    public String getName() {
        return "None";
    }
    
    @Override
    public double estimateCost(int tokens) {
        return 0.0;
    }
    
    @Override
    public int getMaxContextTokens() {
        return Integer.MAX_VALUE;
    }
}
```

### 4. Strategy Factory

```java
@Component
public class LLMStrategyFactory {
    private final Map<String, Supplier<LLMStrategy>> strategies;
    
    @Inject
    public LLMStrategyFactory(GlobalConfiguration config) {
        this.strategies = new HashMap<>();
        
        // Register available strategies
        strategies.put("openai", () -> new OpenAIStrategy(config.getOpenAIConfig()));
        strategies.put("claude", () -> new ClaudeStrategy(config.getClaudeConfig()));
        strategies.put("bedrock-claude", () -> new BedrockClaudeStrategy(config.getBedrockConfig()));
        strategies.put("custom", () -> new CustomLLMStrategy(config.getCustomLLMConfig()));
        strategies.put("none", () -> new NullLLMStrategy());
    }
    
    public LLMStrategy createStrategy(String provider) {
        Supplier<LLMStrategy> supplier = strategies.get(provider.toLowerCase());
        if (supplier == null) {
            logger.warn("Unknown LLM provider: {}, using null strategy", provider);
            return new NullLLMStrategy();
        }
        
        try {
            LLMStrategy strategy = supplier.get();
            strategy.validateConfiguration();
            return strategy;
        } catch (Exception e) {
            logger.error("Failed to create LLM strategy for provider: " + provider, e);
            return new NullLLMStrategy();
        }
    }
    
    public Set<String> getAvailableProviders() {
        return strategies.keySet();
    }
}
```

### 5. Supporting Components

#### Circuit Breaker

```java
public class CircuitBreaker {
    private final int failureThreshold;
    private final long timeout;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private volatile State state = State.CLOSED;
    
    public boolean allowRequest() {
        if (state == State.OPEN) {
            if (System.currentTimeMillis() - lastFailureTime.get() > timeout) {
                state = State.HALF_OPEN;
                return true;
            }
            return false;
        }
        return true;
    }
    
    public void recordSuccess() {
        failureCount.set(0);
        state = State.CLOSED;
    }
    
    public void recordFailure() {
        lastFailureTime.set(System.currentTimeMillis());
        if (failureCount.incrementAndGet() >= failureThreshold) {
            state = State.OPEN;
        }
    }
}
```

#### Prompt Builder Interface

```java
public interface PromptBuilder {
    List<Message> buildMessages(DiagnosticReport report);
    String buildSystemPrompt();
}

public class OpenAIPromptBuilder implements PromptBuilder {
    @Override
    public List<Message> buildMessages(DiagnosticReport report) {
        List<Message> messages = new ArrayList<>();
        
        // System message
        messages.add(new Message("system", buildSystemPrompt()));
        
        // User message with diagnostic context
        String userPrompt = formatDiagnosticContext(report);
        messages.add(new Message("user", userPrompt));
        
        return messages;
    }
    
    private String formatDiagnosticContext(DiagnosticReport report) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Jenkins build failed with the following issues:\n\n");
        
        // Add pattern matches
        if (!report.getPatternMatches().isEmpty()) {
            prompt.append("Pattern Matches:\n");
            report.getPatternMatches().forEach(match -> 
                prompt.append("- ").append(match.getDescription()).append("\n")
            );
        }
        
        // Add MCP results if available
        if (report.getMcpAnalysis() != null) {
            prompt.append("\nMCP Analysis:\n");
            prompt.append(report.getMcpAnalysis().getSummary()).append("\n");
        }
        
        // Add log excerpt
        prompt.append("\nRelevant Log Excerpt:\n");
        prompt.append(report.getLogExcerpt()).append("\n");
        
        prompt.append("\nPlease provide:\n");
        prompt.append("1. Root cause explanation\n");
        prompt.append("2. Step-by-step solution\n");
        prompt.append("3. Prevention recommendations\n");
        
        return prompt.toString();
    }
}
```

## Usage Example

```java
// In Jenkins Pipeline Doctor Plugin main class
public class PipelineDoctorPlugin {
    private final DiagnosticEngine diagnosticEngine;
    private final LLMEnhancementService llmService;
    
    public DiagnosticReport analyzeBuild(Build build) {
        // Run pattern matching and MCP analysis
        DiagnosticReport basicReport = diagnosticEngine.analyze(build);
        
        // Enhance with LLM if enabled
        EnhancedDiagnosticReport enhancedReport = llmService.enhance(basicReport);
        
        // Store and return results
        reportStore.save(build.getId(), enhancedReport);
        return enhancedReport;
    }
}
```

## Benefits of Strategy Pattern

1. **Separation of Concerns**: Main logic doesn't need to know about specific LLM providers
2. **Easy Extension**: Add new providers by implementing the strategy interface
3. **Runtime Flexibility**: Switch providers without restarting Jenkins
4. **Testability**: Mock strategies for unit testing
5. **Graceful Degradation**: Null strategy ensures system works without LLM
6. **Provider-Specific Optimization**: Each strategy can optimize for its provider

## Configuration Management

```java
// Global Jenkins configuration
@Extension
public class LLMGlobalConfiguration extends GlobalConfiguration {
    private String defaultProvider = "none";
    private boolean enabledByDefault = false;
    private double monthlyBudgetLimit = 100.0;
    
    // Provider-specific configurations
    private OpenAIConfig openAIConfig = new OpenAIConfig();
    private ClaudeConfig claudeConfig = new ClaudeConfig();
    private BedrockConfig bedrockConfig = new BedrockConfig();
    private CustomLLMConfig customConfig = new CustomLLMConfig();
    
    // Job can override global settings
    public LLMStrategy getStrategyForJob(Job job) {
        JobLLMProperty property = job.getProperty(JobLLMProperty.class);
        if (property != null && property.isOverrideGlobal()) {
            return strategyFactory.createStrategy(property.getProvider());
        }
        return strategyFactory.createStrategy(defaultProvider);
    }
}
```

## Testing Strategy

```java
@Test
public class LLMEnhancementServiceTest {
    @Mock private LLMStrategy mockStrategy;
    @Mock private LLMConfiguration config;
    
    private LLMEnhancementService service;
    
    @Before
    public void setup() {
        when(config.isEnabled()).thenReturn(true);
        service = new LLMEnhancementService(config);
        service.setStrategy(mockStrategy);
    }
    
    @Test
    public void testSuccessfulEnhancement() {
        DiagnosticReport report = createTestReport();
        EnhancedDiagnosticReport expected = createEnhancedReport();
        
        when(mockStrategy.isAvailable()).thenReturn(true);
        when(mockStrategy.enhance(report)).thenReturn(expected);
        
        EnhancedDiagnosticReport result = service.enhance(report);
        
        assertEquals(expected, result);
        verify(mockStrategy).enhance(report);
    }
    
    @Test
    public void testFallbackOnStrategyFailure() {
        DiagnosticReport report = createTestReport();
        
        when(mockStrategy.isAvailable()).thenReturn(true);
        when(mockStrategy.enhance(report)).thenThrow(new LLMException("API Error"));
        
        EnhancedDiagnosticReport result = service.enhance(report);
        
        // Should return non-enhanced version
        assertFalse(result.isLLMEnhanced());
        assertEquals(report.getIssues(), result.getIssues());
    }
}
```
# Issue Identification Design - Hybrid Architecture

## Overview

The Pipeline Doctor plugin uses a hybrid approach combining pattern-based matching with LLM-enhanced analysis to identify issues from Jenkins build logs. This design leverages the experience of system administrators who can quickly identify common failure patterns while also handling complex, context-dependent issues.

## Architecture Components

### 1. Pattern-Based Fast Path (80% of cases)

Handles common, well-known issues with pre-defined patterns for immediate identification.

```java
public interface PatternMatcher {
    MatchResult match(String logLine, BuildContext context);
    List<Pattern> getPatterns();
    void addPattern(Pattern pattern);
}

public class MatchResult {
    private String issueType;
    private double confidence;  // 0.0 to 1.0
    private Map<String, String> extractedData;
    private String matchedPattern;
}
```

#### Common Pattern Categories

**Network Issues:**
```
- "Unable to connect to .* port .*" → NetworkPolicy/Firewall issue
- "Connection refused" → Service down/NetworkPolicy
- "Name or service not known" → DNS resolution issue
- "timeout" + "connect" → Network timeout
```

**Package Management:**
```
- "404  Not Found.*debian" → Debian archive issue
- "Unable to locate package" → Missing package
- "Hash Sum mismatch" → Corrupted package cache
- "NO_PUBKEY" → Missing GPG key
```

**Container/Docker:**
```
- "pull access denied" → Registry authentication
- "no space left on device" → Disk space
- "Cannot connect to the Docker daemon" → Docker service issue
```

**Kubernetes:**
```
- "forbidden: User.*cannot" → RBAC issue
- "ImagePullBackOff" → Image access issue
- "connection refused.*443" → API server issue
```

### 2. LLM-Enhanced Analysis (20% complex cases)

For patterns that require context understanding or are too complex for regex.

```java
public interface LLMAnalyzer {
    DiagnosticIssue analyze(BuildLog log, AnalysisContext context);
    void setModel(LLMConfig config);
}

public class AnalysisContext {
    private List<HistoricalIssue> similarFailures;
    private JobConfiguration jobConfig;
    private Map<String, String> environmentInfo;
    private List<RecentChange> recentChanges;
}
```

#### LLM Integration Options

1. **Local Models** (Privacy-first)
   - Ollama with CodeLlama/Mistral
   - Low latency, data stays on-premise
   - Model size: 7B-13B parameters

2. **API-based** (Better accuracy)
   - OpenAI API / Claude API
   - Azure OpenAI (enterprise)
   - Better for complex multi-step failures

#### Prompt Engineering

```java
public class LLMPromptBuilder {
    public String buildPrompt(BuildLog log, AnalysisContext context) {
        return String.format("""
            Analyze this Jenkins build failure and identify the root cause.
            
            Build Log (last 200 lines):
            %s
            
            Similar past failures in this environment:
            %s
            
            Job type: %s
            Environment: %s
            
            Identify:
            1. Root cause category (network/package/permission/resource/code)
            2. Specific issue
            3. Confidence level (0-100)
            4. Key error indicators
            
            Respond in JSON format.
            """, 
            log.getTailLines(200),
            formatSimilarFailures(context),
            context.getJobType(),
            context.getEnvironment()
        );
    }
}
```

### 3. Learning Loop

Continuously improve pattern matching based on LLM findings and admin feedback.

```java
public class LearningEngine {
    private PatternRepository patternRepo;
    private FeedbackCollector feedback;
    
    public void learn(DiagnosticIssue issue, AdminFeedback feedback) {
        if (feedback.isCorrect() && issue.getSource() == Source.LLM) {
            // Convert LLM finding to pattern
            Pattern newPattern = extractPattern(issue);
            if (validatePattern(newPattern)) {
                patternRepo.add(newPattern);
            }
        }
        
        // Track pattern effectiveness
        updatePatternMetrics(issue, feedback);
    }
}
```

## Implementation Flow

```java
public class HybridIssueIdentifier {
    private PatternMatcher patternMatcher;
    private LLMAnalyzer llmAnalyzer;
    private IssueHistoryDB historyDB;
    private MetricsCollector metrics;
    
    public DiagnosticIssue identify(BuildLog log, BuildContext context) {
        // Step 1: Fast pattern matching
        long startTime = System.currentTimeMillis();
        MatchResult patternMatch = patternMatcher.match(log, context);
        
        if (patternMatch.getConfidence() > 0.8) {
            metrics.recordPatternMatch(System.currentTimeMillis() - startTime);
            return createIssue(patternMatch, Source.PATTERN);
        }
        
        // Step 2: Check cache for similar issues
        CachedIssue cached = historyDB.findSimilar(log.getSignature());
        if (cached != null && cached.isRecent()) {
            return cached.getIssue();
        }
        
        // Step 3: LLM analysis with context
        AnalysisContext context = buildContext(log, context);
        DiagnosticIssue llmIssue = llmAnalyzer.analyze(log, context);
        
        // Step 4: Store for learning
        historyDB.store(log.getSignature(), llmIssue);
        metrics.recordLLMAnalysis(System.currentTimeMillis() - startTime);
        
        return llmIssue;
    }
}
```

## Tenant-Specific Learning

Track patterns per tenant/team to improve accuracy:

```java
public class TenantPatternManager {
    // Patterns specific to tenant's tech stack
    private Map<String, List<Pattern>> tenantPatterns;
    
    // Tenant-specific issue frequency
    private Map<String, IssueStatistics> tenantStats;
    
    public void learnFromTenant(String tenantId, DiagnosticIssue issue) {
        // Adjust pattern priority based on tenant's history
        updateTenantPatterns(tenantId, issue);
        
        // Track which issues are common for this tenant
        updateTenantStatistics(tenantId, issue);
    }
}
```

## Performance Considerations

1. **Pattern Matching**: < 10ms per build log
2. **LLM Analysis**: 1-5 seconds (async processing)
3. **Caching**: Recent issues cached for 1 hour
4. **Batch Processing**: Queue LLM requests to avoid overload

## Example Issue Detection Flow

```
Build fails with "E: Failed to fetch http://deb.debian.org/... 404 Not Found"
↓
1. Pattern matcher identifies: "404.*debian" → Archive Issue (confidence: 0.95)
↓
2. Issue created with:
   - Type: PACKAGE_REPOSITORY_ERROR
   - Root cause: "Debian version moved to archive"
   - Solution: "Update sources.list to use archive.debian.org"
↓
3. No LLM needed, instant diagnosis
```

```
Build fails with complex multi-stage error
↓
1. Pattern matcher: No clear match (confidence: 0.3)
↓
2. LLM analyzer examines:
   - Full log context
   - Similar tenant failures
   - Recent configuration changes
↓
3. LLM identifies: "NetworkPolicy blocking egress to RDS on port 5432 after K8s upgrade"
↓
4. Learning engine creates new pattern for future quick detection
```

## Configuration

```yaml
issueIdentification:
  patternMatching:
    enabled: true
    maxPatterns: 1000
    confidenceThreshold: 0.8
  
  llm:
    enabled: true
    provider: "ollama"  # or "openai", "azure"
    model: "codellama:7b"
    endpoint: "http://localhost:11434"
    maxTokens: 2000
    temperature: 0.3
    
  learning:
    enabled: true
    minConfidenceToLearn: 0.9
    feedbackWindow: 7d
    
  performance:
    patternCacheSize: 10000
    llmQueueSize: 100
    llmConcurrency: 5
```
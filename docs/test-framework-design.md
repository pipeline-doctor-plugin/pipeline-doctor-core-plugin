# Issue Identification Test Framework

## Overview

A CI-driven test framework for tuning pattern matching and LLM prompts using annotated Jenkins build logs. The framework enables continuous improvement through automated testing and human expert validation.

## Test Data Structure

### 1. Annotated Test Case Format

```yaml
# test-data/cases/network-policy-001.yaml
testCase:
  id: "network-policy-001"
  category: "network"
  description: "Kubernetes NetworkPolicy blocking external endpoint"
  
  input:
    buildLog: |
      [Pipeline] stage
      [Pipeline] { (Deploy)
      [Pipeline] sh
      + curl -X POST https://api.external-service.com/webhook
      curl: (7) Failed to connect to api.external-service.com port 443: Connection refused
      [Pipeline] }
      [Pipeline] // stage
      [Pipeline] stage
      [Pipeline] { (Declarative: Post Actions)
      [Pipeline] }
      ERROR: script returned exit code 7
    
    context:
      jobType: "kubernetes-deployment"
      environment: "production"
      recentChanges:
        - "Updated Kubernetes NetworkPolicy"
        - "Upgraded to K8s 1.28"
  
  expectedOutput:
    problematicStep:
      stage: "Deploy"
      command: "curl -X POST https://api.external-service.com/webhook"
      lineNumber: 5
      errorCode: 7
    
    rootCause:
      category: "NETWORK_ERROR"
      specificIssue: "NetworkPolicy blocking egress"
      confidence: 0.95
    
    solutions:
      - rank: 1
        title: "Update NetworkPolicy to allow egress to external API"
        confidence: 0.9
        steps:
          - "Check current NetworkPolicy: kubectl get networkpolicy -n <namespace>"
          - "Add egress rule for api.external-service.com on port 443"
          - "Apply updated NetworkPolicy"
        example: |
          spec:
            egress:
            - to:
              - namespaceSelector: {}
              ports:
              - protocol: TCP
                port: 443
      
      - rank: 2
        title: "Configure service mesh for external access"
        confidence: 0.7
        steps:
          - "Create ServiceEntry for external service"
          - "Configure DestinationRule if needed"
      
      - rank: 3
        title: "Use egress gateway or proxy"
        confidence: 0.6
        steps:
          - "Route external traffic through egress gateway"
          - "Configure HTTP_PROXY environment variables"
```

### 2. Test Dataset Organization

```
test-data/
├── cases/
│   ├── network/
│   │   ├── network-policy-001.yaml
│   │   ├── dns-resolution-001.yaml
│   │   └── timeout-001.yaml
│   ├── package/
│   │   ├── debian-archive-001.yaml
│   │   ├── missing-package-001.yaml
│   │   └── gpg-key-001.yaml
│   ├── docker/
│   │   ├── pull-denied-001.yaml
│   │   └── disk-space-001.yaml
│   └── kubernetes/
│       ├── rbac-001.yaml
│       └── image-pull-001.yaml
├── patterns/
│   └── patterns.yaml
└── prompts/
    └── issue-identification.txt
```

## Test Framework Components

### 1. Test Runner

```java
public class IssueIdentificationTestRunner {
    private PatternMatcher patternMatcher;
    private LLMAnalyzer llmAnalyzer;
    private TestReporter reporter;
    
    @Test
    public void testAllCases() {
        List<TestCase> testCases = loadTestCases("test-data/cases");
        TestResults results = new TestResults();
        
        for (TestCase testCase : testCases) {
            TestResult result = runTest(testCase);
            results.add(result);
        }
        
        reporter.generateReport(results);
        assertAccuracy(results);
    }
    
    private TestResult runTest(TestCase testCase) {
        // Test pattern matching
        PatternResult patternResult = testPatternMatching(testCase);
        
        // Test LLM analysis
        LLMResult llmResult = testLLMAnalysis(testCase);
        
        // Compare with expected output
        return evaluate(testCase, patternResult, llmResult);
    }
}
```

### 2. Evaluation Metrics

```java
public class TestEvaluator {
    
    public EvaluationMetrics evaluate(TestCase testCase, ActualOutput actual) {
        EvaluationMetrics metrics = new EvaluationMetrics();
        
        // 1. Problem identification accuracy
        metrics.problemIdentified = 
            actual.getProblematicStep().matches(testCase.getExpected().getProblematicStep());
        
        // 2. Root cause accuracy
        metrics.rootCauseScore = calculateSimilarity(
            actual.getRootCause(), 
            testCase.getExpected().getRootCause()
        );
        
        // 3. Solution ranking (NDCG - Normalized Discounted Cumulative Gain)
        metrics.solutionNDCG = calculateNDCG(
            actual.getSolutions(),
            testCase.getExpected().getSolutions()
        );
        
        // 4. Top-3 accuracy
        metrics.topThreeAccuracy = calculateTopKAccuracy(
            actual.getSolutions(),
            testCase.getExpected().getSolutions(),
            3
        );
        
        return metrics;
    }
}
```

### 3. Pattern Tuning

```yaml
# test-data/patterns/patterns.yaml
patterns:
  - id: "network-policy-egress"
    regex: "Failed to connect to .* port \\d+: Connection refused"
    context:
      - "kubernetes"
      - "NetworkPolicy"
    confidence: 0.9
    solution_template: "network-policy-egress-fix"
    
  - id: "debian-archive"
    regex: "404\\s+Not Found.*deb\\.debian\\.org"
    confidence: 0.95
    solution_template: "debian-archive-fix"
```

### 4. Prompt Tuning Framework

```python
# scripts/tune_prompts.py
class PromptTuner:
    def __init__(self, test_cases, llm_client):
        self.test_cases = test_cases
        self.llm = llm_client
        
    def tune_prompt(self, base_prompt):
        variations = self.generate_prompt_variations(base_prompt)
        results = {}
        
        for prompt in variations:
            score = self.evaluate_prompt(prompt)
            results[prompt] = score
            
        return self.select_best_prompt(results)
    
    def evaluate_prompt(self, prompt):
        total_score = 0
        for test_case in self.test_cases:
            response = self.llm.analyze(test_case.input, prompt)
            score = self.calculate_accuracy(response, test_case.expected)
            total_score += score
        return total_score / len(self.test_cases)
```

## CI Pipeline Integration

```yaml
# .github/workflows/test-issue-identification.yml
name: Test Issue Identification

on:
  push:
    paths:
      - 'src/main/java/io/jenkins/plugins/pipelinedoctor/analyzer/**'
      - 'test-data/**'
      - 'prompts/**'

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Test Environment
        run: |
          docker-compose up -d ollama
          ./scripts/load-test-model.sh
      
      - name: Run Pattern Tests
        run: mvn test -Dtest=PatternMatchingTest
        
      - name: Run LLM Tests
        run: mvn test -Dtest=LLMAnalysisTest
        
      - name: Generate Accuracy Report
        run: mvn test -Dtest=IssueIdentificationTestRunner
        
      - name: Upload Test Results
        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: target/issue-identification-report.html
          
      - name: Check Accuracy Threshold
        run: |
          accuracy=$(jq '.overall_accuracy' target/metrics.json)
          if (( $(echo "$accuracy < 0.85" | bc -l) )); then
            echo "Accuracy $accuracy is below threshold 0.85"
            exit 1
          fi
```

## Test Data Collection Helper

```java
// Tool to help you annotate new test cases
public class TestCaseBuilder {
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        TestCase testCase = new TestCase();
        
        System.out.println("=== Jenkins Log Annotation Tool ===");
        System.out.println("Paste the build log (end with '---'):");
        
        String log = readMultilineInput(scanner);
        testCase.setInputLog(log);
        
        System.out.println("\nIdentify the problematic step:");
        System.out.print("Stage name: ");
        String stage = scanner.nextLine();
        
        System.out.print("Failed command: ");
        String command = scanner.nextLine();
        
        System.out.print("Root cause category (NETWORK/PACKAGE/DOCKER/K8S/OTHER): ");
        String category = scanner.nextLine();
        
        System.out.println("\nProvide top 3 solutions:");
        for (int i = 1; i <= 3; i++) {
            System.out.printf("Solution %d title: ", i);
            String title = scanner.nextLine();
            
            System.out.printf("Solution %d confidence (0-1): ", i);
            double confidence = scanner.nextDouble();
            scanner.nextLine(); // consume newline
            
            // Add solution to test case
        }
        
        // Save as YAML
        saveTestCase(testCase);
    }
}
```

## Continuous Improvement Workflow

1. **Collect Failed Builds**
   ```bash
   ./scripts/collect-failed-builds.sh --days 7 --output test-data/raw/
   ```

2. **Annotate with Expert Knowledge**
   ```bash
   java -jar target/annotation-tool.jar test-data/raw/build-123.log
   ```

3. **Run Tests & Tune**
   ```bash
   mvn test -Dtest=IssueIdentificationTestRunner
   ./scripts/tune-patterns.py --threshold 0.9
   ./scripts/tune-prompts.py --model ollama/codellama
   ```

4. **Review Results**
   - Check accuracy metrics
   - Identify failure patterns
   - Update patterns/prompts

5. **Deploy Improvements**
   - Merge tuned patterns
   - Update LLM prompts
   - Tag new version

## Success Metrics

- **Pattern Matching**: >90% accuracy for known issues
- **LLM Analysis**: >80% accuracy for complex issues  
- **Top-3 Solutions**: >85% contain the correct fix
- **Response Time**: <100ms for patterns, <5s for LLM
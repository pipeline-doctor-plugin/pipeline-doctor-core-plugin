package io.jenkins.plugins.pipelinedoctor;

import org.junit.Test;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Simple tests for DiagnosticRegistry functionality without Jenkins integration.
 */
public class DiagnosticRegistrySimpleTest {
    
    @Test
    public void testDiagnosticResultBuilder() {
        DiagnosticResult result = new DiagnosticResult.Builder()
                .id("test-issue")
                .category("test")
                .severity(DiagnosticResult.Severity.HIGH)
                .summary("Test issue")
                .description("Test description")
                .providerId("test-provider")
                .confidence(90)
                .build();
        
        assertEquals("test-issue", result.getId());
        assertEquals("test", result.getCategory());
        assertEquals(DiagnosticResult.Severity.HIGH, result.getSeverity());
        assertEquals("Test issue", result.getSummary());
        assertEquals("Test description", result.getDescription());
        assertEquals("test-provider", result.getProviderId());
        assertEquals(90, result.getConfidence());
    }
    
    @Test
    public void testSolutionBuilder() {
        ActionStep step1 = new ActionStep("First step", "command1", false);
        ActionStep step2 = new ActionStep("Second step", "command2", true);
        
        Solution solution = new Solution.Builder()
                .id("test-solution")
                .title("Test Solution")
                .description("Test description")
                .steps(List.of(step1, step2))
                .priority(100)
                .build();
        
        assertEquals("test-solution", solution.getId());
        assertEquals("Test Solution", solution.getTitle());
        assertEquals("Test description", solution.getDescription());
        assertEquals(2, solution.getSteps().size());
        assertEquals(100, solution.getPriority());
    }
    
    @Test
    public void testActionStep() {
        ActionStep step = new ActionStep("Test step", "test command", true);
        
        assertEquals("Test step", step.getDescription());
        assertEquals("test command", step.getCommand());
        assertTrue(step.isOptional());
        assertTrue(step.hasCommand());
        
        ActionStep stepNoCommand = new ActionStep("No command step");
        assertFalse(stepNoCommand.hasCommand());
        assertFalse(stepNoCommand.isOptional());
    }
    
    @Test
    public void testBuildMetadata() {
        BuildMetadata metadata = new BuildMetadata(
                "test-job", 
                123, 
                "http://jenkins/job/test-job/123/", 
                "test-node", 
                System.currentTimeMillis(),
                "abc123",
                "main"
        );
        
        assertEquals("test-job", metadata.getJobName());
        assertEquals(123, metadata.getBuildNumber());
        assertEquals("http://jenkins/job/test-job/123/", metadata.getBuildUrl());
        assertEquals("test-node", metadata.getNodeName());
        assertEquals("abc123", metadata.getScmRevision());
        assertEquals("main", metadata.getBranch());
    }
    
    @Test
    public void testDiagnosticAction() {
        DiagnosticResult result1 = new DiagnosticResult.Builder()
                .id("issue-1")
                .category("test")
                .severity(DiagnosticResult.Severity.CRITICAL)
                .summary("Critical issue")
                .providerId("test-provider")
                .confidence(95)
                .build();
                
        DiagnosticResult result2 = new DiagnosticResult.Builder()
                .id("issue-2")
                .category("test")
                .severity(DiagnosticResult.Severity.LOW)
                .summary("Low issue")
                .providerId("test-provider")
                .confidence(80)
                .build();
        
        DiagnosticAction action = new DiagnosticAction(List.of(result1, result2));
        
        assertEquals(2, action.getResultCount());
        assertTrue(action.hasResults());
        assertEquals(DiagnosticResult.Severity.CRITICAL, action.getHighestSeverity());
        assertEquals(1, action.getCountBySeverity(DiagnosticResult.Severity.CRITICAL));
        assertEquals(1, action.getCountBySeverity(DiagnosticResult.Severity.LOW));
    }
}
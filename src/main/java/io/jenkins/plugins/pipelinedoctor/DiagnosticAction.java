package io.jenkins.plugins.pipelinedoctor;

import hudson.model.Action;
import hudson.model.Run;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Build action that stores diagnostic results for a build.
 * Provides access to analysis results in the Jenkins UI.
 */
public class DiagnosticAction implements Action {
    
    private final List<DiagnosticResult> results;
    private transient Run<?, ?> run;
    
    public DiagnosticAction(List<DiagnosticResult> results) {
        this.results = results != null ? List.copyOf(results) : Collections.emptyList();
    }
    
    @Override
    public String getIconFileName() {
        return "symbol-stethoscope";
    }
    
    @Override
    public String getDisplayName() {
        return "Pipeline Doctor";
    }
    
    @Override
    public String getUrlName() {
        return "pipeline-doctor";
    }
    
    /**
     * Get the diagnostic results.
     * 
     * @return List of diagnostic results
     */
    public List<DiagnosticResult> getResults() {
        return results;
    }
    
    /**
     * Get the number of results.
     * 
     * @return Number of diagnostic results
     */
    public int getResultCount() {
        return results.size();
    }
    
    /**
     * Check if there are any results.
     * 
     * @return true if there are diagnostic results
     */
    public boolean hasResults() {
        return !results.isEmpty();
    }
    
    /**
     * Get results by severity level.
     * 
     * @param severity The severity level
     * @return List of results with the specified severity
     */
    public List<DiagnosticResult> getResultsBySeverity(DiagnosticResult.Severity severity) {
        return results.stream()
                .filter(result -> result.getSeverity() == severity)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get count of results by severity.
     * 
     * @param severity The severity level
     * @return Number of results with the specified severity
     */
    public long getCountBySeverity(DiagnosticResult.Severity severity) {
        return results.stream()
                .filter(result -> result.getSeverity() == severity)
                .count();
    }
    
    /**
     * Get the highest severity level in the results.
     * 
     * @return Highest severity, or null if no results
     */
    public DiagnosticResult.Severity getHighestSeverity() {
        return results.stream()
                .map(DiagnosticResult::getSeverity)
                .min(DiagnosticResult.Severity::compareTo)  // CRITICAL comes first in enum
                .orElse(null);
    }
    
    /**
     * Set the owner run (called by Jenkins).
     * 
     * @param run The owning run
     */
    public void onAttached(Run<?, ?> run) {
        this.run = run;
    }
    
    /**
     * Get the owner run.
     * 
     * @return The owning run
     */
    public Run<?, ?> getRun() {
        return run;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiagnosticAction that = (DiagnosticAction) o;
        return Objects.equals(results, that.results);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(results);
    }
    
    @Override
    public String toString() {
        return "DiagnosticAction{" +
                "resultCount=" + results.size() +
                ", highestSeverity=" + getHighestSeverity() +
                '}';
    }
}
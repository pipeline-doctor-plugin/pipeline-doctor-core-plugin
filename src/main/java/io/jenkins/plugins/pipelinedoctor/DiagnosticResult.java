package io.jenkins.plugins.pipelinedoctor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a diagnostic result from analyzing build logs.
 * Contains the identified issue and suggested solutions.
 */
public class DiagnosticResult {
    
    public enum Severity {
        CRITICAL,   // Build fails, no workaround
        HIGH,       // Build fails, manual workaround exists  
        MEDIUM,     // Build succeeds but with issues
        LOW         // Performance or optimization suggestions
    }
    
    private final String id;
    private final String category;
    private final Severity severity;
    private final String summary;
    private final String description;
    private final List<Solution> solutions;
    private final Map<String, Object> metadata;
    private final String providerId;
    private final int confidence;
    
    public DiagnosticResult(
            String id,
            String category,
            Severity severity,
            String summary,
            String description,
            List<Solution> solutions,
            Map<String, Object> metadata,
            String providerId,
            int confidence) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.category = Objects.requireNonNull(category, "category cannot be null");
        this.severity = Objects.requireNonNull(severity, "severity cannot be null");
        this.summary = Objects.requireNonNull(summary, "summary cannot be null");
        this.description = description;
        this.solutions = solutions != null ? List.copyOf(solutions) : Collections.emptyList();
        this.metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
        this.providerId = Objects.requireNonNull(providerId, "providerId cannot be null");
        this.confidence = Math.max(0, Math.min(100, confidence)); // Clamp to 0-100
    }
    
    public String getId() {
        return id;
    }
    
    public String getCategory() {
        return category;
    }
    
    public Severity getSeverity() {
        return severity;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public String getDescription() {
        return description;
    }
    
    public List<Solution> getSolutions() {
        return solutions;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public String getProviderId() {
        return providerId;
    }
    
    public int getConfidence() {
        return confidence;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiagnosticResult that = (DiagnosticResult) o;
        return confidence == that.confidence &&
                Objects.equals(id, that.id) &&
                Objects.equals(category, that.category) &&
                severity == that.severity &&
                Objects.equals(summary, that.summary) &&
                Objects.equals(description, that.description) &&
                Objects.equals(solutions, that.solutions) &&
                Objects.equals(metadata, that.metadata) &&
                Objects.equals(providerId, that.providerId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, category, severity, summary, description, 
                          solutions, metadata, providerId, confidence);
    }
    
    @Override
    public String toString() {
        return "DiagnosticResult{" +
                "id='" + id + '\'' +
                ", category='" + category + '\'' +
                ", severity=" + severity +
                ", summary='" + summary + '\'' +
                ", confidence=" + confidence +
                ", providerId='" + providerId + '\'' +
                '}';
    }
    
    /**
     * Builder for creating DiagnosticResult instances.
     */
    public static class Builder {
        private String id;
        private String category;
        private Severity severity;
        private String summary;
        private String description;
        private List<Solution> solutions = Collections.emptyList();
        private Map<String, Object> metadata = Collections.emptyMap();
        private String providerId;
        private int confidence = 100;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder category(String category) {
            this.category = category;
            return this;
        }
        
        public Builder severity(Severity severity) {
            this.severity = severity;
            return this;
        }
        
        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder solutions(List<Solution> solutions) {
            this.solutions = solutions;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public Builder providerId(String providerId) {
            this.providerId = providerId;
            return this;
        }
        
        public Builder confidence(int confidence) {
            this.confidence = confidence;
            return this;
        }
        
        public DiagnosticResult build() {
            return new DiagnosticResult(id, category, severity, summary, description,
                                      solutions, metadata, providerId, confidence);
        }
    }
}
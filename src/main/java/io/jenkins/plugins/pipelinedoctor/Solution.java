package io.jenkins.plugins.pipelinedoctor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a solution for a diagnosed issue.
 * Contains steps to resolve the problem and optional examples.
 */
public class Solution {
    
    private final String id;
    private final String title;
    private final String description;
    private final List<ActionStep> steps;
    private final Map<String, String> examples;
    private final int priority;
    
    public Solution(
            String id,
            String title,
            String description,
            List<ActionStep> steps,
            Map<String, String> examples,
            int priority) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.title = Objects.requireNonNull(title, "title cannot be null");
        this.description = description;
        this.steps = steps != null ? List.copyOf(steps) : Collections.emptyList();
        this.examples = examples != null ? Map.copyOf(examples) : Collections.emptyMap();
        this.priority = priority;
    }
    
    public String getId() {
        return id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public List<ActionStep> getSteps() {
        return steps;
    }
    
    public Map<String, String> getExamples() {
        return examples;
    }
    
    public int getPriority() {
        return priority;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Solution solution = (Solution) o;
        return priority == solution.priority &&
                Objects.equals(id, solution.id) &&
                Objects.equals(title, solution.title) &&
                Objects.equals(description, solution.description) &&
                Objects.equals(steps, solution.steps) &&
                Objects.equals(examples, solution.examples);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, steps, examples, priority);
    }
    
    @Override
    public String toString() {
        return "Solution{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", priority=" + priority +
                '}';
    }
    
    /**
     * Builder for creating Solution instances.
     */
    public static class Builder {
        private String id;
        private String title;
        private String description;
        private List<ActionStep> steps = Collections.emptyList();
        private Map<String, String> examples = Collections.emptyMap();
        private int priority = 100;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder steps(List<ActionStep> steps) {
            this.steps = steps;
            return this;
        }
        
        public Builder examples(Map<String, String> examples) {
            this.examples = examples;
            return this;
        }
        
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }
        
        public Solution build() {
            return new Solution(id, title, description, steps, examples, priority);
        }
    }
}
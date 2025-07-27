package io.jenkins.plugins.pipelinedoctor;

import java.util.Objects;

/**
 * Represents an action step within a solution.
 * Contains a description and optional command to execute.
 */
public class ActionStep {
    
    private final String description;
    private final String command;
    private final boolean optional;
    
    public ActionStep(
            String description,
            String command,
            boolean optional) {
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.command = command;
        this.optional = optional;
    }
    
    public ActionStep(String description) {
        this(description, null, false);
    }
    
    public ActionStep(String description, String command) {
        this(description, command, false);
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getCommand() {
        return command;
    }
    
    public boolean isOptional() {
        return optional;
    }
    
    public boolean hasCommand() {
        return command != null && !command.trim().isEmpty();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionStep that = (ActionStep) o;
        return optional == that.optional &&
                Objects.equals(description, that.description) &&
                Objects.equals(command, that.command);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(description, command, optional);
    }
    
    @Override
    public String toString() {
        return "ActionStep{" +
                "description='" + description + '\'' +
                ", command='" + command + '\'' +
                ", optional=" + optional +
                '}';
    }
}
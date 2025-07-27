package io.jenkins.plugins.pipelinedoctor;

import hudson.EnvVars;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
// import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Implementation of BuildContext for Jenkins builds.
 * Extracts information from Jenkins Run objects.
 */
public class JenkinsBuildContext implements BuildContext {
    
    private static final Logger LOGGER = Logger.getLogger(JenkinsBuildContext.class.getName());
    
    private final Run<?, ?> run;
    private final String buildLog;
    private final Map<String, String> environment;
    private final BuildMetadata metadata;
    
    public JenkinsBuildContext(Run<?, ?> run) throws IOException {
        this.run = Objects.requireNonNull(run, "run cannot be null");
        this.buildLog = extractBuildLog(run);
        this.environment = extractEnvironment(run);
        this.metadata = createBuildMetadata(run);
    }
    
    @Override
    public String getBuildLog() {
        return buildLog;
    }
    
    @Override
    public Map<String, String> getEnvironment() {
        return environment;
    }
    
    @Override
    public BuildMetadata getMetadata() {
        return metadata;
    }
    
    @Override
    public Run<?, ?> getRun() {
        return run;
    }
    
    @Override
    public boolean isPipelineBuild() {
        // Check if this is a pipeline build by class name to avoid hard dependency
        return run.getClass().getName().contains("WorkflowRun");
    }
    
    @Override
    public String getBuildResult() {
        Result result = run.getResult();
        return result != null ? result.toString() : "UNKNOWN";
    }
    
    @Override
    public long getBuildDuration() {
        return run.getDuration();
    }
    
    private String extractBuildLog(Run<?, ?> run) throws IOException {
        try {
            return run.getLog();
        } catch (IOException e) {
            LOGGER.warning("Failed to extract build log for " + run.getFullDisplayName() + ": " + e.getMessage());
            return "";
        }
    }
    
    private Map<String, String> extractEnvironment(Run<?, ?> run) {
        Map<String, String> env = new HashMap<>();
        
        try {
            // Get build environment variables
            EnvVars envVars = run.getEnvironment(null);
            if (envVars != null) {
                env.putAll(envVars);
            }
            
            // Add node-specific environment variables if available
            Node node = run.getExecutor() != null ? run.getExecutor().getOwner().getNode() : null;
            if (node != null) {
                DescribableList<NodeProperty<?>, NodePropertyDescriptor> properties = node.getNodeProperties();
                for (NodeProperty<?> property : properties) {
                    if (property instanceof EnvironmentVariablesNodeProperty) {
                        EnvironmentVariablesNodeProperty envProperty = (EnvironmentVariablesNodeProperty) property;
                        env.putAll(envProperty.getEnvVars());
                    }
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Failed to extract environment for " + run.getFullDisplayName() + ": " + e.getMessage());
        }
        
        return env;
    }
    
    private BuildMetadata createBuildMetadata(Run<?, ?> run) {
        String jobName = run.getParent().getFullName();
        int buildNumber = run.getNumber();
        String buildUrl = run.getUrl();
        String nodeName = extractNodeName(run);
        long startTime = run.getStartTimeInMillis();
        String scmRevision = extractScmRevision(run);
        String branch = extractBranch(run);
        
        return new BuildMetadata(jobName, buildNumber, buildUrl, nodeName, startTime, scmRevision, branch);
    }
    
    private String extractNodeName(Run<?, ?> run) {
        try {
            if (run.getExecutor() != null && run.getExecutor().getOwner() != null) {
                Node node = run.getExecutor().getOwner().getNode();
                return node != null ? node.getNodeName() : "unknown";
            }
        } catch (Exception e) {
            LOGGER.fine("Could not extract node name: " + e.getMessage());
        }
        return "unknown";
    }
    
    private String extractScmRevision(Run<?, ?> run) {
        try {
            // Use reflection to avoid hard dependency on workflow plugins
            Object changeSets = run.getClass().getMethod("getChangeSets").invoke(run);
            if (changeSets instanceof Iterable) {
                for (Object changeSet : (Iterable<?>) changeSets) {
                    Boolean isEmpty = (Boolean) changeSet.getClass().getMethod("isEmptySet").invoke(changeSet);
                    if (!isEmpty) {
                        Object iterator = changeSet.getClass().getMethod("iterator").invoke(changeSet);
                        if (((java.util.Iterator<?>) iterator).hasNext()) {
                            Object entry = ((java.util.Iterator<?>) iterator).next();
                            return (String) entry.getClass().getMethod("getCommitId").invoke(entry);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.fine("Could not extract SCM revision: " + e.getMessage());
        }
        return null;
    }
    
    private String extractBranch(Run<?, ?> run) {
        try {
            // Try to get branch from environment variables
            Map<String, String> env = extractEnvironment(run);
            String branch = env.get("GIT_BRANCH");
            if (branch != null) {
                // Remove origin/ prefix if present
                return branch.startsWith("origin/") ? branch.substring(7) : branch;
            }
            
            // Try other common branch environment variables
            branch = env.get("BRANCH_NAME");
            if (branch != null) {
                return branch;
            }
            
        } catch (Exception e) {
            LOGGER.fine("Could not extract branch: " + e.getMessage());
        }
        return null;
    }
}
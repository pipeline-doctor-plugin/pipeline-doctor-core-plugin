package io.jenkins.plugins.pipelinedoctor;

import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Build listener that triggers diagnostic analysis when builds complete.
 * Integrates Pipeline Doctor analysis into the Jenkins build lifecycle.
 */
@Extension
public class DiagnosticBuildListener extends RunListener<Run<?, ?>> {
    
    private static final Logger LOGGER = Logger.getLogger(DiagnosticBuildListener.class.getName());
    
    @Override
    public void onCompleted(Run<?, ?> run, TaskListener listener) {
        // Only analyze failed or unstable builds by default
        Result result = run.getResult();
        if (result == null || result.isBetterThan(Result.UNSTABLE)) {
            LOGGER.fine("Skipping analysis for successful build: " + run.getFullDisplayName());
            return;
        }
        
        LOGGER.info("Starting diagnostic analysis for build: " + run.getFullDisplayName() + " (result: " + result + ")");
        
        try {
            // Create build context
            JenkinsBuildContext context = new JenkinsBuildContext(run);
            
            // Get diagnostic registry and run analysis
            DiagnosticRegistry registry = DiagnosticRegistry.getInstance();
            List<DiagnosticResult> results = registry.analyze(context);
            
            // Store results as build action
            if (!results.isEmpty()) {
                DiagnosticAction action = new DiagnosticAction(results);
                run.addAction(action);
                
                // Log summary to build console
                listener.getLogger().println();
                listener.getLogger().println("=== Pipeline Doctor Analysis ===");
                listener.getLogger().println("Found " + results.size() + " diagnostic result(s):");
                
                for (DiagnosticResult result1 : results) {
                    listener.getLogger().println("  - " + result1.getSeverity() + ": " + result1.getSummary() + 
                                               " (confidence: " + result1.getConfidence() + "%)");
                }
                
                listener.getLogger().println("View detailed analysis at: " + run.getAbsoluteUrl() + "pipeline-doctor/");
                listener.getLogger().println("===============================");
                
            } else {
                LOGGER.info("No diagnostic issues found for build: " + run.getFullDisplayName());
            }
            
        } catch (IOException e) {
            LOGGER.severe("Failed to create build context for " + run.getFullDisplayName() + ": " + e.getMessage());
            listener.getLogger().println("Pipeline Doctor analysis failed: " + e.getMessage());
            
        } catch (Exception e) {
            LOGGER.severe("Diagnostic analysis failed for " + run.getFullDisplayName() + ": " + e.getMessage());
            listener.getLogger().println("Pipeline Doctor analysis failed: " + e.getMessage());
        }
    }
    
    @Override
    public void onStarted(Run<?, ?> run, TaskListener listener) {
        LOGGER.fine("Build started: " + run.getFullDisplayName());
    }
    
    @Override
    public void onFinalized(Run<?, ?> run) {
        LOGGER.fine("Build finalized: " + run.getFullDisplayName());
    }
}
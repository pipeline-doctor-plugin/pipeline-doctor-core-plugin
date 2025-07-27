package io.jenkins.plugins.pipelinedoctor;

import hudson.model.Run;
import java.util.Map;

/**
 * Context information about a build for diagnostic analysis.
 * Provides access to build logs, environment, and metadata.
 */
public interface BuildContext {
    
    /**
     * Get the raw build log content.
     * 
     * @return Complete build log as string
     */
    String getBuildLog();
    
    /**
     * Get build environment variables.
     * 
     * @return Map of environment variables
     */
    Map<String, String> getEnvironment();
    
    /**
     * Get build metadata (job name, build number, etc.).
     * 
     * @return Build metadata
     */
    BuildMetadata getMetadata();
    
    /**
     * Get the Jenkins Run object for advanced access.
     * 
     * @return Jenkins Run instance
     */
    Run<?, ?> getRun();
    
    /**
     * Check if this is a pipeline build.
     * 
     * @return true if this is a pipeline build
     */
    boolean isPipelineBuild();
    
    /**
     * Get the build result status.
     * 
     * @return Build result (SUCCESS, FAILURE, UNSTABLE, etc.)
     */
    String getBuildResult();
    
    /**
     * Get build duration in milliseconds.
     * 
     * @return Build duration, or -1 if still running
     */
    long getBuildDuration();
}
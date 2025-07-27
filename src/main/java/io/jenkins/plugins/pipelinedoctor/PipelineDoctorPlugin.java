package io.jenkins.plugins.pipelinedoctor;

import hudson.Extension;
import hudson.Plugin;
import jenkins.model.Jenkins;
import java.util.logging.Logger;

/**
 * Main plugin class for Pipeline Doctor Core.
 * Provides the foundation for diagnostic analysis of Jenkins builds.
 */
public class PipelineDoctorPlugin extends Plugin {
    
    private static final Logger LOGGER = Logger.getLogger(PipelineDoctorPlugin.class.getName());
    
    @Override
    public void start() throws Exception {
        super.start();
        LOGGER.info("Pipeline Doctor Core Plugin started");
    }
    
    @Override
    public void stop() throws Exception {
        LOGGER.info("Pipeline Doctor Core Plugin stopped");
        super.stop();
    }
    
    /**
     * Get the plugin instance.
     * 
     * @return Plugin instance or null if not loaded
     */
    public static PipelineDoctorPlugin getInstance() {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        return jenkins != null ? jenkins.getPlugin(PipelineDoctorPlugin.class) : null;
    }
    
}
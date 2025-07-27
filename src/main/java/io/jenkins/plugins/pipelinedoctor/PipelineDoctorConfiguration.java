package io.jenkins.plugins.pipelinedoctor;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Global configuration for Pipeline Doctor plugin.
 * Allows administrators to configure plugin behavior system-wide.
 */
@Extension
public class PipelineDoctorConfiguration extends GlobalConfiguration {

    private boolean enabledByDefault = true;
    private int maxResultsPerBuild = 50;
    private boolean autoAnalyzeBuilds = true;
    private String excludedJobPatterns = "";
    private int analysisTimeoutSeconds = 300;
    private boolean enableDetailedLogging = false;

    public PipelineDoctorConfiguration() {
        load();
    }

    public static PipelineDoctorConfiguration get() {
        return GlobalConfiguration.all().get(PipelineDoctorConfiguration.class);
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws Descriptor.FormException {
        req.bindJSON(this, json);
        save();
        return super.configure(req, json);
    }

    @Override
    public String getDisplayName() {
        return "Pipeline Doctor";
    }

    // Getters and Setters

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    public void setEnabledByDefault(boolean enabledByDefault) {
        this.enabledByDefault = enabledByDefault;
    }

    public int getMaxResultsPerBuild() {
        return maxResultsPerBuild;
    }

    public void setMaxResultsPerBuild(int maxResultsPerBuild) {
        this.maxResultsPerBuild = maxResultsPerBuild;
    }

    public boolean isAutoAnalyzeBuilds() {
        return autoAnalyzeBuilds;
    }

    public void setAutoAnalyzeBuilds(boolean autoAnalyzeBuilds) {
        this.autoAnalyzeBuilds = autoAnalyzeBuilds;
    }

    public String getExcludedJobPatterns() {
        return excludedJobPatterns;
    }

    public void setExcludedJobPatterns(String excludedJobPatterns) {
        this.excludedJobPatterns = excludedJobPatterns;
    }

    public int getAnalysisTimeoutSeconds() {
        return analysisTimeoutSeconds;
    }

    public void setAnalysisTimeoutSeconds(int analysisTimeoutSeconds) {
        this.analysisTimeoutSeconds = analysisTimeoutSeconds;
    }

    public boolean isEnableDetailedLogging() {
        return enableDetailedLogging;
    }

    public void setEnableDetailedLogging(boolean enableDetailedLogging) {
        this.enableDetailedLogging = enableDetailedLogging;
    }

    // Form validation methods

    public FormValidation doCheckMaxResultsPerBuild(@QueryParameter int value) {
        if (value < 1) {
            return FormValidation.error("Maximum results must be at least 1");
        }
        if (value > 1000) {
            return FormValidation.warning("Very high values may impact performance");
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckAnalysisTimeoutSeconds(@QueryParameter int value) {
        if (value < 10) {
            return FormValidation.error("Timeout must be at least 10 seconds");
        }
        if (value > 3600) {
            return FormValidation.warning("Very long timeouts may cause builds to hang");
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckExcludedJobPatterns(@QueryParameter String value) {
        if (value == null || value.trim().isEmpty()) {
            return FormValidation.ok();
        }
        
        try {
            String[] patterns = value.split("[,\n]");
            for (String pattern : patterns) {
                pattern = pattern.trim();
                if (!pattern.isEmpty()) {
                    // Basic validation - check for obvious regex syntax issues
                    java.util.regex.Pattern.compile(pattern);
                }
            }
            return FormValidation.ok();
        } catch (Exception e) {
            return FormValidation.error("Invalid regex pattern: " + e.getMessage());
        }
    }
}
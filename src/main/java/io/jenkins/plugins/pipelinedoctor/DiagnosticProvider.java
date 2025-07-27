package io.jenkins.plugins.pipelinedoctor;

import hudson.ExtensionPoint;
import java.util.List;
import java.util.Set;

/**
 * Extension point for diagnostic providers that can analyze build logs
 * and provide diagnostic results.
 * 
 * Each plugin that provides diagnostic capabilities should implement this interface.
 * The core plugin will discover and invoke all registered providers.
 */
public interface DiagnosticProvider extends ExtensionPoint {
    
    /**
     * Analyze build logs and return diagnostic results.
     * 
     * @param context Build context containing logs and metadata
     * @return List of diagnostic results, can be empty if no issues found
     */
    List<DiagnosticResult> analyze(BuildContext context);
    
    /**
     * Get the provider's unique identifier.
     * This should be unique across all providers.
     * 
     * @return Unique provider ID (e.g., "pattern-matcher", "llm-analyzer")
     */
    String getProviderId();
    
    /**
     * Get human-readable provider name for UI display.
     * 
     * @return Display name (e.g., "Pattern Matcher", "LLM Analyzer")
     */
    String getProviderName();
    
    /**
     * Get supported categories for this provider.
     * 
     * @return Set of category names this provider can handle
     */
    Set<String> getSupportedCategories();
    
    /**
     * Check if this provider is enabled for the given build context.
     * Providers can implement their own logic for when they should run.
     * 
     * @param context Build context
     * @return true if provider should analyze this build
     */
    default boolean isEnabled(BuildContext context) {
        return true;
    }
    
    /**
     * Get provider priority for ordering analysis.
     * Higher priority providers run first.
     * 
     * @return Priority value (default: 100)
     */
    default int getPriority() {
        return 100;
    }
}
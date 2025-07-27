package io.jenkins.plugins.pipelinedoctor;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import jenkins.model.Jenkins;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Registry for managing diagnostic providers.
 * Automatically discovers and manages all DiagnosticProvider implementations.
 */
@Extension
public class DiagnosticRegistry {
    
    private static final Logger LOGGER = Logger.getLogger(DiagnosticRegistry.class.getName());
    
    private final List<DiagnosticProvider> providers = new CopyOnWriteArrayList<>();
    
    /**
     * Initialize the registry by discovering all DiagnosticProvider extensions.
     */
    @Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED)
    public void initialize() {
        LOGGER.info("Initializing Pipeline Doctor diagnostic registry");
        
        // Discover all DiagnosticProvider implementations
        ExtensionList<DiagnosticProvider> allProviders = Jenkins.get().getExtensionList(DiagnosticProvider.class);
        
        for (DiagnosticProvider provider : allProviders) {
            registerProvider(provider);
        }
        
        LOGGER.info("Registered " + providers.size() + " diagnostic providers");
    }
    
    /**
     * Register a diagnostic provider.
     * 
     * @param provider The provider to register
     */
    public synchronized void registerProvider(DiagnosticProvider provider) {
        if (provider == null) {
            LOGGER.warning("Attempted to register null provider");
            return;
        }
        
        String providerId = provider.getProviderId();
        if (providerId == null || providerId.trim().isEmpty()) {
            LOGGER.warning("Provider has null or empty ID, skipping registration: " + provider.getClass().getName());
            return;
        }
        
        // Check for duplicate provider IDs
        boolean isDuplicate = providers.stream()
                .anyMatch(p -> providerId.equals(p.getProviderId()));
        
        if (isDuplicate) {
            LOGGER.warning("Provider with ID '" + providerId + "' already registered, skipping: " + provider.getClass().getName());
            return;
        }
        
        providers.add(provider);
        LOGGER.info("Registered diagnostic provider: " + providerId + " (" + provider.getProviderName() + ")");
    }
    
    /**
     * Unregister a diagnostic provider.
     * 
     * @param provider The provider to unregister
     */
    public synchronized void unregisterProvider(DiagnosticProvider provider) {
        if (providers.remove(provider)) {
            LOGGER.info("Unregistered diagnostic provider: " + provider.getProviderId());
        }
    }
    
    /**
     * Get all registered providers, sorted by priority (highest first).
     * 
     * @return List of all providers
     */
    public List<DiagnosticProvider> getProviders() {
        return providers.stream()
                .sorted(Comparator.comparingInt(DiagnosticProvider::getPriority).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Get providers that support the specified category.
     * 
     * @param category The category to filter by
     * @return List of providers supporting the category
     */
    public List<DiagnosticProvider> getProviders(String category) {
        if (category == null) {
            return getProviders();
        }
        
        return providers.stream()
                .filter(provider -> provider.getSupportedCategories().contains(category))
                .sorted(Comparator.comparingInt(DiagnosticProvider::getPriority).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Get providers that are enabled for the given build context.
     * 
     * @param context Build context
     * @return List of enabled providers
     */
    public List<DiagnosticProvider> getEnabledProviders(BuildContext context) {
        return providers.stream()
                .filter(provider -> provider.isEnabled(context))
                .sorted(Comparator.comparingInt(DiagnosticProvider::getPriority).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Get a provider by its ID.
     * 
     * @param providerId The provider ID
     * @return The provider, or null if not found
     */
    public DiagnosticProvider getProvider(String providerId) {
        return providers.stream()
                .filter(provider -> providerId.equals(provider.getProviderId()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Get all supported categories across all providers.
     * 
     * @return Set of all supported categories
     */
    public Set<String> getAllSupportedCategories() {
        return providers.stream()
                .flatMap(provider -> provider.getSupportedCategories().stream())
                .collect(Collectors.toSet());
    }
    
    /**
     * Run analysis using all enabled providers.
     * 
     * @param context Build context
     * @return List of all diagnostic results
     */
    public List<DiagnosticResult> analyze(BuildContext context) {
        List<DiagnosticResult> allResults = new ArrayList<>();
        List<DiagnosticProvider> enabledProviders = getEnabledProviders(context);
        
        LOGGER.info("Running analysis with " + enabledProviders.size() + " providers for build: " + 
                   context.getMetadata().getJobName() + "#" + context.getMetadata().getBuildNumber());
        
        for (DiagnosticProvider provider : enabledProviders) {
            try {
                long startTime = System.currentTimeMillis();
                List<DiagnosticResult> results = provider.analyze(context);
                long duration = System.currentTimeMillis() - startTime;
                
                if (results != null && !results.isEmpty()) {
                    allResults.addAll(results);
                    LOGGER.info("Provider '" + provider.getProviderId() + "' found " + results.size() + 
                               " issues in " + duration + "ms");
                } else {
                    LOGGER.fine("Provider '" + provider.getProviderId() + "' found no issues in " + duration + "ms");
                }
                
            } catch (Exception e) {
                LOGGER.severe("Error running provider '" + provider.getProviderId() + "': " + e.getMessage());
                // Continue with other providers even if one fails
            }
        }
        
        LOGGER.info("Analysis complete: found " + allResults.size() + " total issues");
        return allResults;
    }
    
    /**
     * Get the singleton instance of the registry.
     * 
     * @return DiagnosticRegistry instance
     */
    public static DiagnosticRegistry getInstance() {
        return ExtensionList.lookupSingleton(DiagnosticRegistry.class);
    }
}
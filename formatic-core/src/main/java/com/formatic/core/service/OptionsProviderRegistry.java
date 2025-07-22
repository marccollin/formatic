package com.formatic.core.service;

import java.util.Optional;

/**
 * Registry to register and retrieve option providers.
 */
public interface OptionsProviderRegistry {

    /**
     * Registers a provider for a given method name.
     *
     * @param methodName the name of the method
     * @param provider the options provider to register
     */
    void registerProvider(String methodName, OptionsProvider provider);

    /**
     * Retrieves a provider by its method name.
     *
     * @param methodName the name of the method
     * @return an Optional containing the provider if found, or empty otherwise
     */
    Optional<OptionsProvider> getProvider(String methodName);

    /**
     * Removes a provider by its method name.
     *
     * @param methodName the name of the method to remove
     */
    void removeProvider(String methodName);

    /**
     * Clears all registered providers from the registry.
     */
    void clear();
}

package com.formatic.core.service;

/**
 * Interface for option providers.
 * <p>
 * Implementations supply option data that can be returned as various types,
 * such as List<SelectRadioOption>, List<Map>, Map<String, String>, etc.
 */
public interface OptionsProvider {
    /**
     * Returns the available options.
     *
     * @return options as List<SelectRadioOption>, List<Map>, Map<String, String>, or other supported types
     */
    Object getOptions();
}

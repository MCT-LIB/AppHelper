package com.mct.app.helper.iap.banner;

import androidx.annotation.NonNull;

/**
 * Customizer - Functional interface for customizing objects of type T.
 * <p>
 * This functional interface defines a single method {@link #customize(T)} that allows customization
 * of objects of type T. It is used to apply customizations to various components or objects
 * within the {@link IapBanner} and related classes.
 */
@FunctionalInterface
public interface Customizer<T> {

    /**
     * Performs the customizations on the input argument.
     *
     * @param t the input argument
     */
    void customize(T t);

    /**
     * Returns a default Customizer that does not alter the input argument.
     *
     * @param <T> The type of the input argument.
     * @return A Customizer that does nothing (no customizations).
     */
    @NonNull
    static <T> Customizer<T> withDefaults() {
        return (t) -> {
            // No customizations are applied, the input remains unchanged.
        };
    }

}

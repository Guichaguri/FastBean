package com.guichaguri.fastbean;

/**
 * Represents a bean conversion utility
 * @author Guichaguri
 */
public interface Bean<T> {

    /**
     * Creates a new class instance without using reflection
     * @return The instance
     */
    T create();

    /**
     * Fills the POJO instance properties using the getter
     * @param instance The instance
     * @param getter The data provider
     */
    void fill(T instance, IPropertyGetter getter);

    /**
     * Extracts data from the POJO instance properties
     * @param instance The instance
     * @param setter The data receiver
     */
    void extract(T instance, IPropertySetter setter);

}

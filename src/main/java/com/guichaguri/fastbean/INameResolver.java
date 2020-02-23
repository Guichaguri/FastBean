package com.guichaguri.fastbean;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Allows customizing which fields and methods will be mapped and what names they'll be mapped to.
 *
 * @author Guichaguri
 */
public interface INameResolver {

    /**
     * Retrieves the name from a method
     * @param method The method
     * @param setter Whether it's a setter or a getter
     * @return The name of the property or {@code null} to ignore this method
     */
    String getName(Method method, boolean setter);

    /**
     * Retrieves the name from a field
     * @param field The field
     * @return The name of the property or {@code null} to ignore this field
     */
    String getName(Field field);

}

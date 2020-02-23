package com.guichaguri.fastbean;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author Guichaguri
 */
public interface INameResolver {

    String getName(Method method, boolean setter);

    String getName(Field field);

}

package com.guichaguri.fastbean;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author Guichaguri
 */
public class FastBean {

    private static final BeanClassLoader classLoader = new BeanClassLoader();

    /**
     * Creates a {@link Bean} class based on the {@code clazz} parameter
     * @param clazz The base class
     * @param <T> The base class type
     * @return The generated {@link Bean}
     */
    public static <T> Bean<T> compile(Class<T> clazz) {
        return compile(clazz, (INameResolver) null);
    }

    /**
     * Creates a {@link Bean} class based on the {@code clazz} parameter
     * @param clazz The base class
     * @param resolver The name resolver
     * @param <T> The base class type
     * @return The generated {@link Bean}
     */
    public static <T> Bean<T> compile(Class<T> clazz, INameResolver resolver) {
        try {
            String className = clazz.getName() + "Bean";

            byte[] bytes = compileClass(clazz, className, resolver);
            Class<?> beanClass = classLoader.loadClass(className, bytes);

            return (Bean<T>) beanClass.newInstance();
        } catch(Exception ex) {
            // Should never be thrown
            throw new RuntimeException(ex);
        }
    }

    /**
     * Creates a {@link Bean} class based on the {@code clazz} parameter
     * @param clazz The base class
     * @param className The class name
     * @return The generated class bytes
     */
    public static byte[] compileClass(Class<?> clazz, String className) {
        return compileClass(clazz, className, null);
    }

    /**
     * Creates a {@link Bean} class based on the {@code clazz} parameter
     * @param clazz The base class
     * @param className The class name
     * @param resolver The name resolver
     * @return The generated class bytes
     */
    public static byte[] compileClass(Class<?> clazz, String className, INameResolver resolver) {
        BeanCompiler compiler = new BeanCompiler(clazz, className);

        addConstructor(compiler, clazz.getDeclaredConstructors());

        for (Method method : clazz.getDeclaredMethods()) {
            addMethod(compiler, resolver, method);
        }

        for(Field field : clazz.getDeclaredFields()) {
            addField(compiler, resolver, field);
        }

        return compiler.compile();
    }

    /**
     * Adds the constructor with the lowest amount of parameters as the default constructor for the create() method
     * @param compiler The compiler
     * @param constructors The list of possible constructors
     */
    private static void addConstructor(BeanCompiler compiler, Constructor<?>[] constructors) {
        Constructor<?> baseConstructor = null;
        int params = Integer.MAX_VALUE;

        for (Constructor<?> constructor : constructors) {
            int count = constructor.getParameterCount();

            if (count < params) {
                baseConstructor = constructor;
                params = count;
            }
        }

        compiler.generateCreateMethod(baseConstructor);
    }

    /**
     * Adds a method as a getter or setter to the compiler
     * @param compiler The compiler
     * @param resolver The name resolver
     * @param method The method
     */
    private static void addMethod(BeanCompiler compiler, INameResolver resolver, Method method) {
        int modifiers = method.getModifiers();

        // Static methods are not allowed
        if (Modifier.isStatic(modifiers)) return;

        int params = method.getParameterCount();
        Class<?> returnType = method.getReturnType();
        boolean setter;

        if (params == 1) {
            setter = true; // One argument = setter method
        } else if (params == 0 && returnType != void.class) {
            setter = false; // No arguments but a return value = getter method
        } else {
            return; // Not a valid getter or setter
        }

        String name;
        boolean accessible = Modifier.isPublic(modifiers);

        if (resolver == null) {

            // We'll ignore private methods if we don't have a resolver
            if (!accessible) return;
            name = method.getName();

            if (setter) {
                if (name.startsWith("set")) {
                    name = toLowerCamelCase(name, 3);
                }
            } else {
                if (name.startsWith("get")) {
                    name = toLowerCamelCase(name, 3);
                } else if (name.startsWith("is")) {
                    name = toLowerCamelCase(name, 2);
                }
            }

        } else {

            name = resolver.getName(method, setter);
            if (name == null) return;
            if (!accessible) method.setAccessible(true);

        }

        if (setter)
            compiler.addGetter(method.getParameterTypes()[0], name, method);
        else
            compiler.addSetter(returnType, name, method);
    }

    /**
     * Adds a field as a getter and setter to the compiler
     * @param compiler The compiler
     * @param resolver The name resolver
     * @param field The field
     */
    private static void addField(BeanCompiler compiler, INameResolver resolver, Field field) {
        int modifiers = field.getModifiers();

        // Static methods are not allowed
        if (Modifier.isStatic(modifiers)) return;

        String name;
        boolean accessible = Modifier.isPublic(modifiers);
        Class<?> type = field.getType();

        if (resolver == null) {

            // We'll ignore private fields if we don't have a resolver
            if (!accessible) return;
            name = field.getName();

        } else {

            name = resolver.getName(field);
            if (name == null) return;
            if (!accessible) field.setAccessible(true);

        }

        compiler.addGetter(type, name, field);
        compiler.addSetter(type, name, field);
    }

    /**
     * Converts a string to lower camel case (e.g. nameOfProperty)
     * @param name The string
     * @param start The start of the string
     * @return The final string
     */
    private static String toLowerCamelCase(String name, int start) {
        return name.substring(start, start + 1).toLowerCase() + name.substring(start + 1);
    }

    /**
     * A custom class loader that allows loading raw byte arrays as classes
     */
    private static class BeanClassLoader extends ClassLoader {

        private Class<?> loadClass(String className, byte[] bytes) {
            return defineClass(className, bytes, 0, bytes.length);
        }

    }

}

package com.guichaguri.fastbean;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author Guichaguri
 */
public class FastBean {

    private static final BeanClassLoader classLoader = new BeanClassLoader();

    public static <T> Bean<T> compile(Class<T> clazz) {
        return compile(clazz, (INameResolver)null);
    }

    public static <T> Bean<T> compile(Class<T> clazz, INameResolver resolver) {
        try {
            String className = clazz.getName() + "Bean";

            byte[] bytes = compile(clazz, className, resolver);
            Class beanClass = classLoader.loadClass(className, bytes);

            return (Bean<T>)beanClass.newInstance();
        } catch(Exception ex) {
            // Should never be thrown
            throw new RuntimeException(ex);
        }
    }

    public static byte[] compile(Class clazz, String className) {
        return compile(clazz, className, null);
    }

    public static byte[] compile(Class clazz, String className, INameResolver resolver) {
        BeanCompiler compiler = new BeanCompiler(clazz, className);

        for(Method method : clazz.getDeclaredMethods()) {
            if(method.getParameterCount() != 1 || Modifier.isStatic(method.getModifiers())) continue;

            String name;
            boolean accessible = Modifier.isPublic(method.getModifiers());

            if(resolver == null) {
                if(!accessible) continue;
                name = method.getName();

                if(name.startsWith("set")) {
                    name = name.substring(3, 4).toLowerCase() + name.substring(4);
                }
            } else {
                name = resolver.getName(method);
                if(name == null) continue;
                if(!accessible) method.setAccessible(true);
            }

            compiler.addMethodCall(method, name);
        }

        for(Field field : clazz.getDeclaredFields()) {
            if(Modifier.isStatic(field.getModifiers())) continue;

            String name;
            boolean accessible = Modifier.isPublic(field.getModifiers());

            if(resolver == null) {
                if(!accessible) continue;
                name = field.getName();
            } else {
                name = resolver.getName(field);
                if(name == null) continue;
                if(!accessible) field.setAccessible(true);
            }

            compiler.addFieldPut(field, name);
        }

        return compiler.compile();
    }


    private static class BeanClassLoader extends ClassLoader {

        private Class loadClass(String className, byte[] bytes) {
            return defineClass(className, bytes, 0, bytes.length);
        }

    }

}

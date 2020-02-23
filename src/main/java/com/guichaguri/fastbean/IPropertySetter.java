package com.guichaguri.fastbean;

/**
 * Sets a value from a property name
 *
 * This interface can be implemented to help map any object into a POJO
 *
 * @author Guichaguri
 */
@FunctionalInterface
public interface IPropertySetter {

    void setObject(String name, Object value);

    default void setShort(String name, short value) {
        setObject(name, value);
    }

    default void setInt(String name, int value) {
        setObject(name, value);
    }

    default void setLong(String name, long value) {
        setObject(name, value);
    }

    default void setDouble(String name, double value) {
        setObject(name, value);
    }

    default void setFloat(String name, float value) {
        setObject(name, value);
    }

    default void setByte(String name, byte value) {
        setObject(name, value);
    }

    default void setBoolean(String name, boolean value) {
        setObject(name, value);
    }

    default void setChar(String name, char value) {
        setObject(name, value);
    }

    default void setString(String name, String value) {
        setObject(name, value);
    }

}

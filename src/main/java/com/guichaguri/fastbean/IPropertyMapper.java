package com.guichaguri.fastbean;

/**
 * Gets a object from a property name
 *
 * This interface can be implemented to help map any object into a POJO
 *
 * @author Guichaguri
 */
@FunctionalInterface
public interface IPropertyMapper {

    Object getObject(String property);

    default short getShort(String property) {
        return (short)getObject(property);
    }

    default int getInt(String property) {
        return (int)getObject(property);
    }

    default long getLong(String property) {
        return (long)getObject(property);
    }

    default double getDouble(String property) {
        return (double)getObject(property);
    }

    default float getFloat(String property) {
        return (float)getObject(property);
    }

    default byte getByte(String property) {
        return (byte)getObject(property);
    }

    default boolean getBoolean(String property) {
        return (boolean)getObject(property);
    }

    default char getChar(String property) {
        return (char)getObject(property);
    }

    default String getString(String property) {
        return (String)getObject(property);
    }

}

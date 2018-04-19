package com.guichaguri.fastbean;

/**
 * @author Guichaguri
 */
public interface Bean<T> {

    T create(IPropertyMapper mapper);

}

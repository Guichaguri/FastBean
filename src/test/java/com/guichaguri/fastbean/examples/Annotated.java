package com.guichaguri.fastbean.examples;

import com.guichaguri.fastbean.Bean;
import com.guichaguri.fastbean.FastBean;
import com.guichaguri.fastbean.INameResolver;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class Annotated {

    public static void main(String[] args) {
        // Compiles the bean class using our name processor
        Bean<User> bean = FastBean.compile(User.class, new NameProcessor());

        // Creates the user instance
        User user = bean.create();

        // Sets the initial data
        user.name = "Carl";

        Map<String, Object> map = new HashMap<>();

        // Extracts the initial date to the Map
        bean.extract(user, map::put);

        System.out.println(map.get("username"));

        // Adds more dummy data, but this time is inside the Map
        map.put("ageNumber", 25);

        // Fills the user instance with the data from the Map
        bean.fill(user, map::get);

        System.out.println(user.age);
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface PropertyName {
        String value();
    }

    public static class NameProcessor implements INameResolver {

        @Override
        public String getName(Method method, boolean setter) {
            PropertyName name = method.getAnnotation(PropertyName.class);
            if (name == null) return null; // null means "do not map this method"
            return name.value();
        }

        @Override
        public String getName(Field field) {
            PropertyName name = field.getAnnotation(PropertyName.class);
            if (name == null) return null; // null means "do not map this field"
            return name.value();
        }
    }

    public static class User {

        @PropertyName("username")
        public String name;

        private int age;

        @PropertyName("ageNumber")
        public int getAge() {
            return age;
        }

        @PropertyName("ageNumber")
        public void setAge(int age) {
            this.age = age;
        }

    }

}

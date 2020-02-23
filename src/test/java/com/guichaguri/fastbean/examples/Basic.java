package com.guichaguri.fastbean.examples;

import com.guichaguri.fastbean.Bean;
import com.guichaguri.fastbean.FastBean;
import java.util.HashMap;
import java.util.Map;

public class Basic {

    public static void main(String[] args) {
        // Compiles the user into a bean mapper
        Bean<User> bean = FastBean.compile(User.class);

        // Initializes the User class
        User user = bean.create();

        Map<String, Object> data = new HashMap<>();
        data.put("name", "John");
        data.put("age", 20);

        // Fills the class with data from a Map
        bean.fill(user, data::get);

        System.out.println(user.name);
        System.out.println(user.age);

        // Change a property
        user.name = "Mark";

        // Extract all data into the Map
        bean.extract(user, data::put);

        System.out.println(data.get("name"));
    }


    public static class User {
        // Works with fields
        public String name;
        private int age;

        // Works with getters
        public int getAge() {
            return age;
        }

        // Works with setters
        public void setAge(int age) {
            this.age = age;
        }
    }

}

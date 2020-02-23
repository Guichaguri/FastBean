package com.guichaguri.fastbean.examples;

import com.guichaguri.fastbean.Bean;
import com.guichaguri.fastbean.FastBean;
import com.guichaguri.fastbean.IPropertyGetter;
import com.guichaguri.fastbean.IPropertySetter;

public class CustomSource {

    public static void main(String[] args) {
        Bean<TeamConfig> bean = FastBean.compile(TeamConfig.class);

        TeamConfig config = new TeamConfig();
        DummyData data = new DummyData();

        bean.fill(config, data);

        bean.extract(config, data);
    }

    public static class DummyData implements IPropertyGetter, IPropertySetter {

        @Override
        public Object getObject(String property) {
            // Returns random data
            return (int) Math.floor(Math.random() * 1000);
        }

        @Override
        public String getString(String property) {
            // Returns random data
            return Integer.toString((int) Math.floor(Math.random() * Integer.MAX_VALUE), 16);
        }

        @Override
        public void setObject(String name, Object value) {
            // Prints out the data
            System.out.println(name + " = " + value);
        }
    }

    public static class TeamConfig {

        public String name;
        public int members;
        public int projects;

    }

}

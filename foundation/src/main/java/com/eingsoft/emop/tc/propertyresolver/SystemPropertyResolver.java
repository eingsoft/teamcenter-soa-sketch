package com.eingsoft.emop.tc.propertyresolver;

public class SystemPropertyResolver extends PropertyResolver {

    @Override
    public String getProperty(String key) {
        return System.getProperty(key);
    }

    @Override
    public void setProperty(String key, String value) {
        System.setProperty(key, value);
    }

}

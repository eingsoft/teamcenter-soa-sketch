package com.eingsoft.emop.tc.propertyresolver;

import lombok.extern.log4j.Log4j2;

/**
 * used to resolve property value
 * 
 * @author beam
 *
 */
@Log4j2
public abstract class PropertyResolver {

  private static PropertyResolver instance;

  public abstract String getProperty(String key);

  public abstract void setProperty(String key, String value);

  public String getProperty(String key, String defaultVal) {
    if (getProperty(key) == null) {
      return defaultVal;
    } else {
      return getProperty(key);
    }
  }

  public int getIntProperty(String key, int defaultVal) {
    if (getProperty(key) == null) {
      return defaultVal;
    } else {
      return Integer.parseInt(getProperty(key));
    }
  }

  public boolean getBooleanProperty(String key, boolean defaultVal) {
    if (getProperty(key) == null) {
      return defaultVal;
    } else {
      return Boolean.parseBoolean(getProperty(key));
    }
  }

  public static void setPropertyResolver(PropertyResolver resolver) {
    if (instance != null) {
      throw new RuntimeException("PropertyResolver instance already exists.");
    }
    instance = resolver;
    log.info("using " + resolver.getClass().getName() + " as property resolver.");
  }

  public static PropertyResolver getPropertyResolver() {
    if (instance == null) {
      return new SystemPropertyResolver();
    } else {
      return instance;
    }
  }
}

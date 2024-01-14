package com.eingsoft.emop.tc.service;

import static com.eingsoft.emop.tc.connection.ConnectionBuilderFactory.PASSWORD;
import static com.eingsoft.emop.tc.connection.ConnectionBuilderFactory.POOLED;
import static com.eingsoft.emop.tc.connection.ConnectionBuilderFactory.USERNAME;
import static com.eingsoft.emop.tc.propertyresolver.PropertyResolver.getPropertyResolver;
import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.pool.TcSessionPool;
import lombok.SneakyThrows;

/**
 * interface to provide a default implementation to get {@link TcContextHolder} instance
 * 
 * @author beam
 *
 */
public interface TcContextHolderAware {

  default TcContextHolder getTcContextHolder() {
    return SOAExecutionContext.current().getTcContextHolder();
  }

  @SneakyThrows
  default void initEphemeralContextManually() {
    if (getPropertyResolver().getBooleanProperty(POOLED, true)) {
      SOAExecutionContext.current().init(getPropertyResolver().getProperty(USERNAME), TcSessionPool.getInstance().borrowObject(20 * 1000));
    } else {
      SOAExecutionContext.current().init(getPropertyResolver().getProperty(USERNAME),
          new EphemeralCredentialContextHolder(getPropertyResolver().getProperty(USERNAME), getPropertyResolver().getProperty(PASSWORD)));
    }
  }

  @SneakyThrows
  default void initContextManually() {
    if (getPropertyResolver().getBooleanProperty(POOLED, true)) {
      SOAExecutionContext.current().init(getPropertyResolver().getProperty(USERNAME), TcSessionPool.getInstance().borrowObject(20 * 1000));
    } else {
      SOAExecutionContext.current().init(getPropertyResolver().getProperty(USERNAME),
          new CredentialTcContextHolder(getPropertyResolver().getProperty(USERNAME), getPropertyResolver().getProperty(PASSWORD)));
    }
  }
}

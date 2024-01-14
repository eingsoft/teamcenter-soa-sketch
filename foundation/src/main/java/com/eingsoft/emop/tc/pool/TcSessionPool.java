package com.eingsoft.emop.tc.pool;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import com.eingsoft.emop.tc.propertyresolver.PropertyResolver;
import com.eingsoft.emop.tc.service.CredentialTcContextHolder;

public class TcSessionPool extends GenericObjectPool<CredentialTcContextHolder> {

  private static TcSessionPool pool;

  public TcSessionPool(PooledObjectFactory<CredentialTcContextHolder> factory, GenericObjectPoolConfig<CredentialTcContextHolder> config) {
    super(factory, config);
  }

  public static TcSessionPool getInstance() {
    GenericObjectPoolConfig<CredentialTcContextHolder> poolConfig = new GenericObjectPoolConfig<>();
    poolConfig.setMaxIdle(PropertyResolver.getPropertyResolver().getIntProperty("tc.pool.maxIdle", 5));
    poolConfig.setMaxTotal(PropertyResolver.getPropertyResolver().getIntProperty("tc.pool.maxTotal", 10));
    poolConfig.setMinIdle(PropertyResolver.getPropertyResolver().getIntProperty("tc.pool.minIdle", 2));
    poolConfig.setMaxWaitMillis(PropertyResolver.getPropertyResolver().getIntProperty("tc.pool.maxWaitMillis", 60000));
    poolConfig.setTimeBetweenEvictionRunsMillis(1000 * 60 * 30);
    poolConfig.setTestOnBorrow(PropertyResolver.getPropertyResolver().getBooleanProperty("tc.pool.testOnBorrow", false));
    poolConfig.setTestWhileIdle(PropertyResolver.getPropertyResolver().getBooleanProperty("tc.pool.testWhileIdle", false));
    poolConfig.setTestOnCreate(PropertyResolver.getPropertyResolver().getBooleanProperty("tc.pool.testOnCreate", false));
    poolConfig.setTestOnReturn(PropertyResolver.getPropertyResolver().getBooleanProperty("tc.pool.testOnReturn", false));
    if (pool == null) {
      pool = new TcSessionPool(new TcSessionPoolFactory(), poolConfig);
    }
    return pool;
  }
}

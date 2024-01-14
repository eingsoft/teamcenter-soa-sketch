package com.eingsoft.emop.tc;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.eingsoft.emop.tc.pool.TcSessionPool;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcContextHolderAware;
import com.eingsoft.emop.tc.service.impl.SessionIdTcContextHolder;
import com.eingsoft.emop.tc.util.MockDataUtil;

public class SOAExecutionContextTest implements TcContextHolderAware {

    @Before
    public void setup() {
        MockDataUtil.initConnectionBuilderInfo();
    }

    @After
    public void destory() {
        SOAExecutionContext.current().cleanupSiliently();
    }

    @Test
    public void testInitContext() {
        TcContextHolder contextHolder = new SessionIdTcContextHolder(UUID.randomUUID().toString());
        SOAExecutionContext.current().init("infodba", contextHolder);
        Assert.assertEquals(contextHolder, SOAExecutionContext.current().getTcContextHolder());
    }

    @Test
    public void testContextIsThreadLocalScope() throws InterruptedException, ExecutionException {
        TcContextHolder contextHolder = new SessionIdTcContextHolder(UUID.randomUUID().toString());
        SOAExecutionContext.current().init("infodba", contextHolder);
        Future<Void> f = Executors.newSingleThreadExecutor().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    SOAExecutionContext.current().getTcContextHolder();
                    throw new RuntimeException("not reachable");
                } catch (IllegalStateException e) {
                }
                return null;
            }
        });
        f.get();
    }

    @Test
    public void testInitWithSameCache() throws InterruptedException, ExecutionException {
        TcContextHolder contextHolder = new SessionIdTcContextHolder(UUID.randomUUID().toString());
        SOAExecutionContext.current().init("infodba", contextHolder);
        final SOAExecutionContext execContext = SOAExecutionContext.current();
        Future<Void> f = Executors.newSingleThreadExecutor().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    SOAExecutionContext.current().getTcContextHolder();
                    throw new RuntimeException("not reachable");
                } catch (IllegalStateException e) {
                }
                SOAExecutionContext.current().initWithSameCache(execContext);
                Assert.assertEquals(contextHolder, SOAExecutionContext.current().getTcContextHolder());
                return null;
            }
        });
        f.get();
    }

    @Test(expected = IllegalStateException.class)
    public void testDifferentSOAExecutionContextCanOnlyBeInitializedOnce() {
        SOAExecutionContext.current().init("infodba", new SessionIdTcContextHolder(UUID.randomUUID().toString()));
        SOAExecutionContext.current().init("infodba", new SessionIdTcContextHolder(UUID.randomUUID().toString()));
    }

    public void testDifferentSOAExecutionContextCanOnlyBeInitializedOnce2() {
        SOAExecutionContext.current().init("infodba", "infodba");
        SOAExecutionContext.current().init("infodba", "infodba2");
    }
    
    @Test
    public void testPool() {
      System.setProperty("tc.username", "infodba");
      System.setProperty("tc.password", "infodba");
      System.setProperty("tc.pooled", "true");
      initEphemeralContextManually();
      Assert.assertEquals(1, TcSessionPool.getInstance().getNumActive());
      initEphemeralContextManually();
      Assert.assertEquals(2, TcSessionPool.getInstance().getNumActive());
      initEphemeralContextManually();
      Assert.assertEquals(3, TcSessionPool.getInstance().getNumActive());
      SOAExecutionContext.current().cleanupSiliently();
      initEphemeralContextManually();
      Assert.assertEquals(3, TcSessionPool.getInstance().getNumActive());
    }
}

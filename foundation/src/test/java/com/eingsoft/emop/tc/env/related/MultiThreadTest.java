package com.eingsoft.emop.tc.env.related;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.eingsoft.emop.tc.model.ModelObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.connection.ConnectionBuilderFactory;
import com.eingsoft.emop.tc.service.TcContextAwareTask;
import lombok.Data;

@Ignore
public class MultiThreadTest {

  @Before
  public void setup() {
    ConnectionBuilderFactory.setProperties("172.17.253.115", 7001);
    System.setProperty("tc.username", "infodba");
    System.setProperty("tc.password", "infodba");
  }

  @Test
  public void testMultiThreadSupport() {
    SOAExecutionContext.current().init();
    List<String> uids = Arrays.asList("wRT5Q19BpMNbrA", "QLY1vOEQpMNbrA", "A3W1vOEQpMNbrA", "AHc1vOEQpMNbrA");
    try {
      final SOAExecutionContext ctx = SOAExecutionContext.current();
      List<String> names = uids.parallelStream().map(uid -> {
        try {
          return new TcContextAwareTask<String>(new RetrieveNameTask(uid), ctx).call();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }).collect(Collectors.toList());
      System.out.println(names);
      Assert.assertEquals(uids.size(), names.size());
    } finally {
      SOAExecutionContext.current().cleanupSiliently();
    }
  }

  @Data
  public static class RetrieveNameTask implements Callable<String> {

    private final String uid;

    @Override
    public String call() throws Exception {
      ModelObject modelObject = SOAExecutionContext.current().getTcContextHolder().getTcLoadService().loadObjectWithProperties(uid);
      return modelObject == null ? null : modelObject.getObjectName();
    }

  }
}

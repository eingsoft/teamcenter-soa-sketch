package com.eingsoft.emop.tc.service;

import java.util.concurrent.Callable;
import com.eingsoft.emop.tc.SOAExecutionContext;
import lombok.Data;

@Data
public class TcContextAwareTask<V> implements Callable<V> {

  private final Callable<V> task;
  private final SOAExecutionContext ctx;

  @Override
  public V call() throws Exception {
    SOAExecutionContext.current().initWithSameCache(ctx);
    try {
      return task.call();
    } finally {
      SOAExecutionContext.current().cleanupSiliently(false);
    }
  }
}

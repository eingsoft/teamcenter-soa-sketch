package com.eingsoft.emop.tc.env.related;

import com.eingsoft.emop.tc.model.ModelObject;
import com.google.common.collect.Lists;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.service.CredentialTcContextHolder;
import com.eingsoft.emop.tc.service.TcContextHolder;

@Ignore
public class TcWorkflowServiceTest {

  @Before
  public void init() {}

  private void initContext() {
    System.setProperty("tc.protocol", "http");
    System.setProperty("tc.host", "10.16.4.151");
    System.setProperty("tc.port", "7001");
    System.setProperty("tc.appName", "tc");
    System.setProperty("tc.username", "zhicheng");
    System.setProperty("tc.password", "1");
    SOAExecutionContext.current().init("king", "king");
  }

  @After
  public void teardown() {
    SOAExecutionContext.current().cleanupSiliently();
  }

  @Test
  public void testSignOff() {
    initContext();
    TcContextHolder contextHolder = SOAExecutionContext.current().getTcContextHolder();
    contextHolder.getTcWorkflowService().signOffTask("h$U1cKMvJMlDLB", true, "ok");
    contextHolder.getTcWorkflowService().signOffTask("xBS193_BJMlDLB", false, "reject");
  }

  @Test
  public void testSetStatus() {
    initContext();
    TcContextHolder contextHolder = SOAExecutionContext.current().getTcContextHolder();
    ItemRevision revision = contextHolder.getTcDataManagementService().findLatestItemRevision("000034");
    contextHolder.getTcWorkflowService().setStatus(Lists.newArrayList((com.teamcenter.soa.client.model.ModelObject) revision),"Release");
  }
}

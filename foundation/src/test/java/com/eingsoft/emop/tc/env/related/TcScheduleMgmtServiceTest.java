package com.eingsoft.emop.tc.env.related;

import java.util.Calendar;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.teamcenter.soa.client.model.strong.Schedule;
import com.teamcenter.soa.client.model.strong.ScheduleTask;

@Ignore
public class TcScheduleMgmtServiceTest {

  @Before
  public void init() {}

  private void initContext() {
    System.setProperty("tc.protocol", "http");
    System.setProperty("tc.host", "10.16.4.138");
    System.setProperty("tc.port", "7001");
    System.setProperty("tc.appName", "tc");
    System.setProperty("tc.username", "king");
    System.setProperty("tc.password", "king");
    SOAExecutionContext.current().initWithoutPool("king", "king");
  }

  @After
  public void teardown() {
    SOAExecutionContext.current().cleanupSiliently();
  }

  @Test
  public void testAssignWorkflowTemplate() {
    initContext();
    TcContextHolder contextHolder = SOAExecutionContext.current().getTcContextHolder();
    ModelObject scheduleTask = contextHolder.getTcLoadService().loadObject("RQtB0O7dJI9lWB");
    String workflowTemplateUid = "yCZt8hUyJI9lWB";
    contextHolder.getTcScheduleMgmtService().updateScheduleTaskWorkflowTemplate((ScheduleTask) scheduleTask, workflowTemplateUid);
  }

  @Test
  public void testLaunchWorkflow() {
    initContext();
    TcContextHolder contextHolder = SOAExecutionContext.current().getTcContextHolder();
    ModelObject jOb = contextHolder.getTcScheduleMgmtService().launchScheduledWorkflow("Amd1gIwfJI9lWB");
    System.out.println(jOb.getUid());
  }

  @Test
  public void testCreateSchedule() {
    initContext();
    TcContextHolder contextHolder = SOAExecutionContext.current().getTcContextHolder();
    Calendar finishDate = Calendar.getInstance();
    finishDate.set(Calendar.HOUR_OF_DAY, finishDate.get(Calendar.HOUR_OF_DAY) + 8);
    ModelObject schedule =
        contextHolder.getTcScheduleMgmtService().createSchedule("", "king-schedule", "king", Calendar.getInstance(), finishDate, "", "");
    Calendar taskEndDate = Calendar.getInstance();
    taskEndDate.set(Calendar.HOUR_OF_DAY, taskEndDate.get(Calendar.HOUR_OF_DAY) + 4);
    ModelObject task1 = contextHolder.getTcScheduleMgmtService().createScheduleTask((Schedule) schedule, null, "", "first-task", "",
        Calendar.getInstance(), taskEndDate, "1", false);
    ModelObject task2 = contextHolder.getTcScheduleMgmtService().createScheduleTask((Schedule) schedule, (ScheduleTask) task1, "",
        "second-task", "", Calendar.getInstance(), taskEndDate, "1", false);
  }

  @Test
  public void testCreateBaselines() {
    initContext();
    TcContextHolder contextHolder = SOAExecutionContext.current().getTcContextHolder();
    ModelObject schedule = contextHolder.getTcLoadService().loadObject("wDgB6vB3JI9lWB");
    contextHolder.getTcScheduleMgmtService().createBaselines("king-test", (Schedule) schedule, null, true);
  }

}

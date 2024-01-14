package com.eingsoft.emop.tc.service.impl;

import com.eingsoft.emop.tc.BMIDE;
import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;
import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcScheduleMgmtService;
import com.eingsoft.emop.tc.util.ProxyUtil;
import com.google.common.collect.Lists;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.strong.projectmanagement.ScheduleManagementService;
import com.teamcenter.services.strong.projectmanagement._2007_06.ScheduleManagement.TaskDeliverableContainer;
import com.teamcenter.services.strong.projectmanagement._2008_06.ScheduleManagement.CreateScheduleResponse;
import com.teamcenter.services.strong.projectmanagement._2008_06.ScheduleManagement.NewScheduleContainer;
import com.teamcenter.services.strong.projectmanagement._2008_06.ScheduleManagement.ScheduleDeliverableData;
import com.teamcenter.services.strong.projectmanagement._2008_06.ScheduleManagement.StringValContainer;
import com.teamcenter.services.strong.projectmanagement._2011_06.ScheduleManagement.*;
import com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement;
import com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.*;
import com.teamcenter.services.strong.projectmanagement._2012_09.ScheduleManagement.AssignmentCreateContainer;
import com.teamcenter.services.strong.projectmanagement._2007_01.ScheduleManagement.CreateBaselineContainer;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.*;
import com.teamcenter.soa.exceptions.NotLoadedException;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.stream.Collectors;

import static com.eingsoft.emop.tc.BMIDE.*;
import static com.eingsoft.emop.tc.util.ProxyUtil.spy;

@Log4j2
@ScopeDesc(Scope.TcContextHolder)
public class TcScheduleMgmtServiceImpl implements TcScheduleMgmtService {
  @Getter
  private TcContextHolder tcContextHolder;

  public TcScheduleMgmtServiceImpl(TcContextHolder tcContextHolder) {
    this.tcContextHolder = tcContextHolder;
  }

  @Override
  public void findAllTasksBySchedule() {
    log.info("start");
  }

  @Override
  public ModelObject createSchedule(String name) {
    NewScheduleContainer newSchedule = new NewScheduleContainer();
    newSchedule.name = name;
    newSchedule.percentLinked = true;
    newSchedule.startDate = Calendar.getInstance();
    newSchedule.finishDate = Calendar.getInstance();
    newSchedule.isTemplate = false;
    newSchedule.type = "Schedule";
    CreateScheduleResponse response =
        tcContextHolder.getScheduleManagementService().createSchedule(new NewScheduleContainer[]{newSchedule});
    ServiceData serviceData = response.serviceData;

    return serviceData.sizeOfCreatedObjects() >= 1 ? ProxyUtil.spy(serviceData.getCreatedObject(0), tcContextHolder) : null;
  }

  @Override
  public void updateSchedule(Schedule schedule, List<AttributeUpdateContainer> attributeUpdateContainers) {

    ScheduleManagementService scheduleManagementService = tcContextHolder.getScheduleManagementService();
    ObjectUpdateContainer objectUpdateContainer = new ObjectUpdateContainer();
    objectUpdateContainer.object = schedule;
    objectUpdateContainer.updates = attributeUpdateContainers.toArray(new AttributeUpdateContainer[]{});

    try {
      ServiceData serviceData = scheduleManagementService.updateSchedules(new ScheduleManagement.ObjectUpdateContainer[]{objectUpdateContainer});
      tcContextHolder.printAndLogMessageFromServiceData(serviceData, true);
    } catch (ServiceException e) {
      e.printStackTrace();
    }
  }

  @Override
  public ModelObject createSchedule(String scheduleId, String scheduleName, String scheduleDesc, Calendar startDate, Calendar finishDate,
                                    String customerNumber, String customerName) {
    NewScheduleContainer input = new NewScheduleContainer();
    input.type = "Schedule";
    input.id = scheduleId;
    input.name = scheduleName;
    input.description = scheduleDesc;
    input.revID = "";
    input.customerNumber = customerNumber;
    input.customerName = customerName;
    input.billCode = "";
    input.billSubCode = "";
    input.billType = "";
    input.startDate = startDate;
    input.finishDate = finishDate;
    input.isPublic = false;
    input.isTemplate = false;
    input.notificationsEnabled = true;
    input.percentLinked = false;
    input.published = true;
    input.taskFixedType = 0;
    input.priority = 3;
    input.status = 0;
    input.billRate = null;
    input.stringValueContainer = new StringValContainer[6];
    input.stringValueContainer[0] = new StringValContainer();
    input.stringValueContainer[0].key = "dates_linked";
    input.stringValueContainer[0].type = 5;
    input.stringValueContainer[0].value = "false";
    input.stringValueContainer[1] = new StringValContainer();
    input.stringValueContainer[1].key = "end_date_scheduling";
    input.stringValueContainer[1].type = 5;
    input.stringValueContainer[1].value = "false";
    input.stringValueContainer[2] = new StringValContainer();
    input.stringValueContainer[2].key = "fnd0allowExecUpdates";
    input.stringValueContainer[2].type = 5;
    input.stringValueContainer[2].value = "false";
    input.stringValueContainer[3] = new StringValContainer();
    input.stringValueContainer[3].key = "wbsformat";
    input.stringValueContainer[3].type = 7;
    input.stringValueContainer[3].value = "N.N";
    input.stringValueContainer[4] = new StringValContainer();
    input.stringValueContainer[4].key = "wbsvalue";
    input.stringValueContainer[4].type = 7;
    input.stringValueContainer[4].value = "1";
    input.stringValueContainer[5] = new StringValContainer();
    input.stringValueContainer[5].key = "fnd0TimeZone";
    input.stringValueContainer[5].type = 7;
    input.stringValueContainer[5].value = "Asia/Shanghai";

    CreateScheduleResponse response = tcContextHolder.getScheduleManagementService().createSchedule(new NewScheduleContainer[]{input});
    tcContextHolder.printAndLogMessageFromServiceData(response.serviceData, true);
    return response.serviceData.sizeOfCreatedObjects() >= 1 ? ProxyUtil.spy(response.serviceData.getCreatedObject(0), tcContextHolder)
        : null;
  }

  private ScheduleLoadResponse[] loadSchedulesResp(String[] uids) {
    if (uids == null || uids.length == 0) {
      log.warn("the param of uids must neither empty nor null when load schedules. ");
      return new ScheduleLoadResponse[]{};
    }
    LoadScheduleContainer loadScheduleContainer = new LoadScheduleContainer();
    SchMgtOptions schmgtOptions = new SchMgtOptions();
    List<SchMgtIntegerOption> schInterOptionList = new ArrayList<>();
    List<SchMgtLogicalOption> schLogicalOptionList = new ArrayList<>();
    List<SchMgtStringOption> schStringOptionList = new ArrayList<>();
    SchMgtIntegerOption schMgtIntegerOption1 = new SchMgtIntegerOption();
    schMgtIntegerOption1.name = SM_STRUCTURE_PARTIAL_CONTEXT;
    schMgtIntegerOption1.value = 0;
    SchMgtIntegerOption schMgtIntegerOption2 = new SchMgtIntegerOption();
    schMgtIntegerOption2.name = SM_STRUCTURE_LOAD_CONTEXT;
    schMgtIntegerOption2.value = 0;
    SchMgtIntegerOption schMgtIntegerOption3 = new SchMgtIntegerOption();
    schMgtIntegerOption3.name = SM_STRUCTURE_CLIENT_CONTEXT;
    schMgtIntegerOption3.value = 0;
    schInterOptionList.add(schMgtIntegerOption1);
    schInterOptionList.add(schMgtIntegerOption2);
    schInterOptionList.add(schMgtIntegerOption3);

    schmgtOptions.integerOptions = schInterOptionList.toArray(new SchMgtIntegerOption[schInterOptionList.size()]);
    schmgtOptions.logicalOptions = schLogicalOptionList.toArray(new SchMgtLogicalOption[schLogicalOptionList.size()]);
    schmgtOptions.stringOptions = schStringOptionList.toArray(new SchMgtStringOption[schStringOptionList.size()]);

    loadScheduleContainer.schmgtOptions = schmgtOptions;
    loadScheduleContainer.scheduleUids = uids;
    MultipleScheduleLoadResponses schLoadResp =
        tcContextHolder.getScheduleManagementService().loadSchedules(new LoadScheduleContainer[]{loadScheduleContainer});
    ScheduleLoadResponse[] schLoadRepArray = schLoadResp.scheduleData;

    return schLoadRepArray;
  }

  /**
   * 根据schedule uids获取时间表和时间表任务
   *
   * @param uids
   * @return
   */
  @Override
  public Map<ModelObject, List<ModelObject>> getSchedulesWithTasks(String... uids) {
    ScheduleLoadResponse[] loadSchedulesResp = loadSchedulesResp(uids);
    ModelObject[] schedules = Arrays.stream(loadSchedulesResp).map(o -> spy(o.schedule, tcContextHolder)).toArray(ModelObject[]::new);

    Map<ModelObject, List<ModelObject>> objMapOut = new HashMap<ModelObject, List<ModelObject>>();

    for (int i = 0; i < schedules.length; i++) {
      objMapOut.put(schedules[i], Arrays.stream(loadSchedulesResp[i].scheduleTasks).map((o) -> spy(o, tcContextHolder)).filter(o -> {
        try {
          return o.get(PROP_FND0PARENTTASK) != null;
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }).collect(Collectors.toList()));
    }
    return objMapOut;

  }

  @Override
  public Map<ModelObject, List<ModelObject>> getSchedulesWithAllTasks(String[] uids) {
    ScheduleLoadResponse[] loadSchedulesResp = loadSchedulesResp(uids);
    ModelObject[] schedules = Arrays.stream(loadSchedulesResp).map(o -> spy(o.schedule, tcContextHolder)).toArray(ModelObject[]::new);

    Map<ModelObject, List<ModelObject>> objMapOut = new HashMap<ModelObject, List<ModelObject>>();

    for (int i = 0; i < schedules.length; i++) {
      objMapOut.put(schedules[i],
          Arrays.stream(loadSchedulesResp[i].scheduleTasks).map((o) -> spy(o, tcContextHolder)).collect(Collectors.toList()));
    }
    return objMapOut;
  }

  @Override
  public ModelObject launchScheduledWorkflow(String scheduleTaskUid) {
    List<ModelObject> jobs = launchScheduledWorkflows(new String[]{scheduleTaskUid});
    return jobs.isEmpty() ? null : jobs.get(0);
  }

  @Override
  public List<ModelObject> launchScheduledWorkflows(String... scheduleTaskUids) {
    List<ModelObject> scheduleTasks = tcContextHolder.getTcLoadService().loadObjects(Arrays.asList(scheduleTaskUids));
    List<ScheduleTask> filterTasks =
        scheduleTasks.stream().filter(t -> t instanceof ScheduleTask).map(t -> (ScheduleTask) t).collect(Collectors.toList());
    if (filterTasks.isEmpty()) {
      log.warn("skip launch schedule workflow because the input parameters({}) were not Schedule Task type.", scheduleTaskUids.toString());
    }
    try {
      LaunchedWorkflowContainer container =
          tcContextHolder.getScheduleManagementService().launchScheduledWorkflow(filterTasks.toArray(new ScheduleTask[]{}));
      tcContextHolder.printAndLogMessageFromServiceData(container.serviceData, true);
      EPMJob[] jobs = container.launchedWorkflows;
      if (jobs == null || jobs.length == 0) {
        return Collections.emptyList();
      }
      return Arrays.asList(jobs).stream().map(j -> spy(j, getTcContextHolder())).collect(Collectors.toList());
    } catch (ServiceException e) {
      e.printStackTrace();
    }
    return Collections.emptyList();
  }

  /**
   * @param attrName
   * @param attrValue
   * @param attrType  1: String, 2: Integer, 4: Double, 6: Boolean
   * @return
   */
  @Override
  public AttributeUpdateContainer buildAttributeUpdateContainer(String attrName, String attrValue, int attrType) {
    AttributeUpdateContainer container = new AttributeUpdateContainer();
    container.attrName = attrName;
    container.attrType = attrType;
    container.attrValue = attrValue;
    return container;
  }

  @Override
  public ModelObject createScheduleTask(Schedule schedule, ScheduleTask parentTask, String taskId, String taskName, String taskDesc,
                                        Calendar startDate, Calendar finishDate, String fixedType, Boolean isMileStone) {
    ScheduleManagementService smService = tcContextHolder.getScheduleManagementService();

    TaskCreateContainer[] taskCreate = new TaskCreateContainer[1];
    taskCreate[0] = new TaskCreateContainer();
    taskCreate[0].name = taskName;
    taskCreate[0].desc = taskDesc;
    taskCreate[0].objectType = "ScheduleTask";
    taskCreate[0].start = startDate;
    taskCreate[0].finish = finishDate;
    taskCreate[0].prevSibling = null;
    if (isMileStone) {
      taskCreate[0].workEstimate = 0;
    }

    if (parentTask == null) {
      taskCreate[0].parent = (ScheduleTask) ProxyUtil.spy(schedule, getTcContextHolder()).get("fnd0SummaryTask");
    } else {
      taskCreate[0].parent = parentTask;
    }

    int len = isMileStone ? 8 : 7;
    AttributeUpdateContainer[] otherAttributes = new AttributeUpdateContainer[len];
    otherAttributes[0] = buildAttributeUpdateContainer("fixed_type", fixedType, 2);
    otherAttributes[1] = buildAttributeUpdateContainer("item_id", taskId, 1);
    otherAttributes[2] = buildAttributeUpdateContainer("work_complete", "0", 2);
    otherAttributes[3] = buildAttributeUpdateContainer("duration", isMileStone ? "0" : "-1", 2);
    otherAttributes[4] = buildAttributeUpdateContainer("priority", "3", 2);
    otherAttributes[5] = buildAttributeUpdateContainer("auto_complete", "false", 6);
    otherAttributes[6] = buildAttributeUpdateContainer("constraint", "0", 2);

    if (isMileStone) {
      otherAttributes[7] = buildAttributeUpdateContainer("task_type", "1", 2);
    }

    taskCreate[0].otherAttributes = otherAttributes;
    try {
      CreatedObjectsContainer response = smService.createTasks(schedule, taskCreate);
      tcContextHolder.printAndLogMessageFromServiceData(response.serviceData, true);
      return response.serviceData.sizeOfCreatedObjects() >= 1 ? ProxyUtil.spy(response.createdObjects[0], tcContextHolder) : null;
    } catch (ServiceException e) {
      e.printStackTrace();
    }

    return null;
  }


  /**
   * 删除 Schedule 对象
   *
   * @param schedule 即将删除的时间表对象
   * @param force    尝试开启旁路，以及修改 owner 来强制删除时间表
   * @throws ServiceException
   */
  @Override
  public void delete(Schedule schedule, boolean force) throws NotLoadedException {

    // 已经拥有旁路权限？
    boolean hasPassBefore = tcContextHolder.getTcSOAService().hasByPass();
    if (force) {
      if (!hasPassBefore && tcContextHolder.getTcSOAService().getUser().get_is_member_of_dba()) {
        tcContextHolder.getTcSOAService().openByPass();
      }
      tcContextHolder.getTcDataManagementService().changeOwnership(Lists.newArrayList(getTcContextHolder().getTcLoadService().loadObject(schedule.getUid())), getTcContextHolder().getTcSOAService().getUser());
    }

    com.teamcenter.services.strong.projectmanagement._2007_01.ScheduleManagement.ScheduleObjDeleteContainer scheduleObjDeleteContainer = new com.teamcenter.services.strong.projectmanagement._2007_01.ScheduleManagement.ScheduleObjDeleteContainer();
    scheduleObjDeleteContainer.object = schedule;
    ServiceData serviceData = tcContextHolder.getScheduleManagementService().deleteSchedulingObjects(new com.teamcenter.services.strong.projectmanagement._2007_01.ScheduleManagement.ScheduleObjDeleteContainer[]{scheduleObjDeleteContainer});

    // 在该方法中开启了旁路
    if (!hasPassBefore && tcContextHolder.getTcSOAService().hasByPass()) {
      getTcContextHolder().getTcSOAService().closeByPass();
    }
    tcContextHolder.printAndLogMessageFromServiceData(serviceData, true);
  }

  @Override
  public void deleteScheduleTask(ScheduleTask task) {
    deleteScheduleTasks(Lists.newArrayList(task));
  }

  @Override
  public void deleteScheduleTasks(List<ScheduleTask> tasks) {
    if (tasks.isEmpty()) {
      return;
    }
    ModelObject schedule = getScheduleByTask(tasks.get(0));

    com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.DeleteTaskContainer response;
    try {
      response = tcContextHolder.getScheduleManagementService().deleteTasks((Schedule) schedule, tasks.toArray(new WorkspaceObject[]{}));
      tcContextHolder.printAndLogMessageFromServiceData(response.serviceData, true);
    } catch (ServiceException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void updateScheduleTaskCompletePercent(ScheduleTask scheduleTask, String completePercent) {
    AttributeUpdateContainer container = buildAttributeUpdateContainer("complete_percent", completePercent, 4);
    updateScheduleTask(scheduleTask, Lists.newArrayList(container));
  }

  @Override
  public void updateScheduleTaskWorkflowTemplate(ScheduleTask scheduleTask, String workflowTemplateUid) {
    AttributeUpdateContainer container = buildAttributeUpdateContainer("workflow_template", workflowTemplateUid, 1);
    updateScheduleTask(scheduleTask, Lists.newArrayList(container));
  }

  @Override
  public ModelObject getScheduleByTask(ScheduleTask scheduleTask) {
    if (Objects.isNull(scheduleTask)) {
      return null;
    }
    ModelObject schedule = ProxyUtil.spy(scheduleTask, tcContextHolder).getModelObject(BMIDE.PROP_Schedule_Tag);
    return schedule;
  }

  @Override
  public void updateScheduleTask(ScheduleTask scheduleTask, List<AttributeUpdateContainer> containers) {
    ModelObject schedule = getScheduleByTask(scheduleTask);
    ObjectUpdateContainer[] updateContainer = new ObjectUpdateContainer[1];
    updateContainer[0] = new ObjectUpdateContainer();
    updateContainer[0].object = (POM_object) scheduleTask;
    updateContainer[0].updates = containers.toArray(new AttributeUpdateContainer[]{});
    try {
      ServiceData serviceData = tcContextHolder.getScheduleManagementService().updateTasks((Schedule) schedule, updateContainer);
      tcContextHolder.printAndLogMessageFromServiceData(serviceData, true);
    } catch (ServiceException e) {
      e.printStackTrace();
    }
  }

  @Override
  public ModelObject addScheduleDeliverable(Schedule schedule, com.teamcenter.soa.client.model.ModelObject deliverable) {
    ScheduleManagementService smService = tcContextHolder.getScheduleManagementService();
    ModelObject object = ProxyUtil.spy(deliverable, getTcContextHolder());
    ScheduleDeliverableData[] input = new ScheduleDeliverableData[1];
    input[0] = new ScheduleDeliverableData();
    input[0].schedule = schedule;
    input[0].deliverableName = object.getDisplayVal(BMIDE.PROP_OBJECT_NAME);
    input[0].deliverableType = object.getTypeObject().getName();
    input[0].deliverableReference = (WorkspaceObject) deliverable;

    ServiceData serviceData = smService.createScheduleDeliverableTemplates(input);
    tcContextHolder.printAndLogMessageFromServiceData(serviceData, true);

    return serviceData.sizeOfCreatedObjects() >= 1 ? ProxyUtil.spy(serviceData.getCreatedObject(0), tcContextHolder) : null;
  }


  @Override
  public ModelObject addTaskDeliverable(ScheduleTask task, com.teamcenter.soa.client.model.ModelObject deliverable) {

    ScheduleManagementService smService = tcContextHolder.getScheduleManagementService();

    TaskDeliverableContainer[] input = new TaskDeliverableContainer[1];
    input[0] = new TaskDeliverableContainer();
    input[0].scheduleTask = task;
    input[0].scheduleDeliverable = deliverable;

    ServiceData serviceData = smService.createTaskDeliverableTemplates(input);
    tcContextHolder.printAndLogMessageFromServiceData(serviceData, true);
    return serviceData.sizeOfCreatedObjects() >= 1 ? ProxyUtil.spy(serviceData.getCreatedObject(0), tcContextHolder) : null;
  }

  @Override
  public void assignTaskResources(ScheduleTask task, List<com.teamcenter.soa.client.model.ModelObject> resources) {
    ModelObject schedule = getScheduleByTask(task);
    ScheduleManagementService smService = tcContextHolder.getScheduleManagementService();
    AssignmentCreateContainer[] assignParam = new AssignmentCreateContainer[resources.size()];
    for (int i = 0; i < resources.size(); i++) {
      assignParam[i] = new AssignmentCreateContainer();
      assignParam[i].task = task;
      assignParam[i].resource = (POM_object) resources.get(i);
      assignParam[i].assignedPercent = 100 / resources.size();
    }
    try {
      CreatedObjectsContainer container = smService.assignResources((Schedule) schedule, assignParam);
      tcContextHolder.printAndLogMessageFromServiceData(container.serviceData, true);
    } catch (ServiceException e) {
      e.printStackTrace();
    }
  }

    @Override
    public void createBaselines(String baselineName, Schedule schedule, Schedule parentBaseline, boolean isActive) {
        CreateBaselineContainer container = new CreateBaselineContainer();
        container.isActive = isActive;
        container.parentBaseline = parentBaseline;
        container.schedule = schedule;
        container.name = baselineName;
        ServiceData serviceData = getTcContextHolder().getScheduleManagementService().createNewBaselines(new CreateBaselineContainer[]{container});
        tcContextHolder.printAndLogMessageFromServiceData(serviceData, true);
    }

}

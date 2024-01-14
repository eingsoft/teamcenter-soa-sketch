package com.eingsoft.emop.tc.service.impl;

import com.eingsoft.emop.tc.BMIDE;
import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;
import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcWorkflowService;
import com.eingsoft.emop.tc.util.EPM_attachement;
import com.eingsoft.emop.tc.util.ICCTArgUtil;
import com.eingsoft.emop.tc.util.ProxyUtil;
import com.google.common.collect.Lists;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.internal.strong.core._2011_06.ICT;
import com.teamcenter.services.strong.workflow._2007_06.Workflow.ReleaseStatusInput;
import com.teamcenter.services.strong.workflow._2007_06.Workflow.ReleaseStatusOption;
import com.teamcenter.services.strong.workflow._2008_06.Workflow;
import com.teamcenter.services.strong.workflow._2008_06.Workflow.AttachmentInfo;
import com.teamcenter.services.strong.workflow._2008_06.Workflow.ContextData;
import com.teamcenter.services.strong.workflow._2008_06.Workflow.InstanceInfo;
import com.teamcenter.services.strong.workflow._2008_06.Workflow.Tasks;
import com.teamcenter.services.strong.workflow._2013_05.Workflow.GetWorkflowTemplatesInputInfo;
import com.teamcenter.services.strong.workflow._2013_05.Workflow.GetWorkflowTemplatesResponse;
import com.teamcenter.services.strong.workflow._2014_06.Workflow.PerformActionInputInfo;
import com.teamcenter.services.strong.workflow._2007_06.Workflow.SetReleaseStatusResponse;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.*;
import com.teamcenter.soa.common.ObjectPropertyPolicy;
import com.teamcenter.soa.common.PolicyProperty;
import com.teamcenter.soa.common.PolicyType;
import com.teamcenter.soa.exceptions.NotLoadedException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@ScopeDesc(Scope.TcContextHolder)
public class TcWorkflowServiceImpl implements TcWorkflowService {
  @Data
  @AllArgsConstructor
  public static class PerformActionResult {
    private boolean ok = false;
    private String msg = "";
    private boolean taskDone = false;

    public PerformActionResult(boolean ok, String msg) {
      this.ok = ok;
      this.msg = msg;
    }
  }

  @Getter
  private TcContextHolder tcContextHolder;

  public TcWorkflowServiceImpl(TcContextHolder tcContextHolder) {
    this.tcContextHolder = tcContextHolder;
  }

  /**
   * 如果ServiceData 中有错误， 则打印出来，并记录LOG, 并返回true
   *
   * @param serviceData
   */
  private boolean isExistErrorMessagesWithPrintAndLog(ServiceData serviceData) {
    boolean isExistErrors = false;
    for (int i = 0; i < serviceData.sizeOfPartialErrors(); i++) {
      String errMessage = Arrays.toString(serviceData.getPartialError(i).getMessages());
      log.error(errMessage);
      isExistErrors = true;
    }
    return isExistErrors;
  }

  /**
   * 获取所有工作流模版， 并返回加载所有属性的对象
   *
   * @return
   */
  @Override
  public List<EPMTaskTemplate> getAllWorkflowTemplatesList() {
    GetWorkflowTemplatesInputInfo inputInfo = new GetWorkflowTemplatesInputInfo();
    inputInfo.getFiltered = false;
    GetWorkflowTemplatesResponse response =
        tcContextHolder.getWorkflowService().getWorkflowTemplates(new GetWorkflowTemplatesInputInfo[] {inputInfo});
    ServiceData serviceData = response.serviceData;

    List<EPMTaskTemplate> objs = Lists.newArrayList();
    if (!isExistErrorMessagesWithPrintAndLog(serviceData)) {
      EPMTaskTemplate[] templates = response.templatesOutput[0].workflowTemplates;
      for (ModelObject obj : tcContextHolder.getTcLoadService().loadProperties(Arrays.asList(templates))) {
        objs.add((EPMTaskTemplate) obj);
      }
    }
    return objs;
  }

  /**
   * 获取所有工作流模版， 并返回加载所有属性的对象
   *
   * @return
   */
  @Override
  public List<EPMTaskTemplate> getAllWorkflowTemplates() {
    return getAllWorkflowTemplatesList();
  }

  /**
   * 获取当前工作流对象（加载所有属性后的对象）的所有任务列表
   *
   * @param process
   * @param state 2 or 4
   * @return
   */
  @Override
  public List<EPMTask> getAllTasks(EPMJob process, int state) {
    Tasks taskResponse = tcContextHolder.getWorkflowService().getAllTasks(process, state);
    ServiceData serviceData = taskResponse.serviceData;
    List<ModelObject> tasks = new ArrayList<>();
    List<EPMTask> loadedTasks = Lists.newArrayList();
    if (!isExistErrorMessagesWithPrintAndLog(serviceData)) {
      tasks = tcContextHolder.getTcLoadService().loadProperties(Arrays.asList(taskResponse.allTasks));
      for (ModelObject epmTask : tasks) {
        loadedTasks.add((EPMTask) epmTask);
      }
    }
    return loadedTasks;
  }

  /**
   * 创建新流程 This method is designed to create a New Process as if you used the Rich client interface
   * New Process dialog <ctrl-p>.
   *
   * @param targetObjs 需要走流程的对象
   * @param wfName 流程名
   * @param templateName 流程模板名
   * @return
   */
  @Override
  public boolean createNewProcess(List<? extends com.teamcenter.soa.client.model.ModelObject> targetObjs, String wfName,
      String templateName) {
    setObjectPolicy();
    ContextData contextData = new ContextData();
    String observerKey = "";
    String name = wfName;
    String subject = wfName;
    String description = wfName + templateName;
    String[] attachmentsString = new String[targetObjs.size()];
    int[] attachmentTypesInt = new int[targetObjs.size()];
    int i = 0;
    for (com.teamcenter.soa.client.model.ModelObject obj : targetObjs) {
      if (obj != null) {
        attachmentsString[i] = obj.getUid();
        attachmentTypesInt[i] = EPM_attachement.target.value();
        i++;
      }
    }
    contextData.processTemplate = templateName; // update withyour
                                                // ownProcessTemplate.
    contextData.subscribeToEvents = false;
    contextData.subscriptionEventCount = 0;
    contextData.attachmentCount = targetObjs.size();
    contextData.attachments = attachmentsString;
    contextData.attachmentTypes = attachmentTypesInt;

    InstanceInfo instanceInfo =
        tcContextHolder.getWorkflowService().createInstance(true, observerKey, name, subject, description, contextData);
    if (!isExistErrorMessagesWithPrintAndLog(instanceInfo.serviceData)) {
      log.info("New WorkFlow Instance info: workflow name [{}], template name [{}]", wfName, templateName);
      return true;
    }
    return false;
  }

  /**
   * 创建新流程 This method is different from createNewProcess in that the attachment is added at a later
   * time.
   *
   * @return
   */
  @Override
  public boolean createNewProcess2(String itemRevUid, String wfName, String processTemplate) {
    EPMJob job = createNewProcess(wfName, processTemplate);
    return addJobAttachements(job, itemRevUid);
  }

  @Override
  public EPMJob createNewProcess(String wfName, String processTemplate) {
    setObjectPolicy();
    ContextData contextData = new ContextData();
    String observerKey = "";
    String name = wfName;
    String subject = wfName;
    String description = wfName + processTemplate;
    contextData.processTemplate = processTemplate; // update with your own
                                                   // Process Template.
    contextData.subscribeToEvents = false;
    contextData.subscriptionEventCount = 0;
    contextData.attachmentCount = 0;
    InstanceInfo instanceInfo =
        tcContextHolder.getWorkflowService().createInstance(false, observerKey, name, subject, description, contextData);
    EPMJob job = null;
    if (!isExistErrorMessagesWithPrintAndLog(instanceInfo.serviceData)) {
      for (int i = 0; i < instanceInfo.serviceData.sizeOfCreatedObjects(); i++) {
        com.teamcenter.soa.client.model.ModelObject obj = instanceInfo.serviceData.getCreatedObject(i);
        if (obj instanceof EPMJob) {
          job = (EPMJob) obj;
        }
      }
    }
    return job;
  }

  @Override
  public PerformActionResult signOffTask(String taskUID, boolean approve, String commentVal) {
    try {
      EPMTask task = (EPMTask) tcContextHolder.getTcLoadService().loadObjectWithProperties(taskUID);

      // check type
      if (!(task instanceof EPMPerformSignoffTask)) {
        return new PerformActionResult(false, "任务类型不正确");
      }

      // check privilege
      com.teamcenter.soa.client.model.ModelObject[] signoffs = task.get_user_all_signoffs();
      if (signoffs == null || signoffs.length <= 0) {
        return new PerformActionResult(false, "系统错误");
      }

      Signoff signOff = (Signoff) signoffs[0];
      if (signOff == null) {
        return new PerformActionResult(false, "系统错误");
      }

      signOff = (Signoff) tcContextHolder.getTcLoadService().loadProperty(signOff, "is_mine_to_perform");
      if (signOff == null) {
        return new PerformActionResult(false, "系统错误");
      }

      boolean canPerform = signOff.get_is_mine_to_perform();
      if (!canPerform) {
        return new PerformActionResult(false, "没有权限做这个操作");
      }

      // check status
      String status = task.get_real_state();
      if (status == null || !status.equals("Started")) {
        if ("Completed".equals(status)) {
          return new PerformActionResult(false, "任务已完成", true);
        } else {
          return new PerformActionResult(false, "任务状态不对");
        }
      }

      PerformActionInputInfo inputInfo = new PerformActionInputInfo();
      if (approve) {
        inputInfo.action = "SOA_EPM_approve_action";
        inputInfo.supportingValue = "SOA_EPM_approve";
      } else {
        inputInfo.action = "SOA_EPM_reject_action";
        inputInfo.supportingValue = "SOA_EPM_reject";
      }
      inputInfo.actionableObject = signOff;
      // inputInfo.supportingObject = tcContextHolder.getTcSOAService().getUser();
      Map<String, String[]> map = new HashMap<String, String[]>();
      map.put("comments", new String[] {commentVal});
      inputInfo.propertyNameValues = map;

      if (isExistErrorMessagesWithPrintAndLog(
          tcContextHolder.getWorkflowService().performAction3(new PerformActionInputInfo[] {inputInfo}))) {
        return new PerformActionResult(false, "操作执行失败");
      } ;

      /*
       * if (isExistErrorMessagesWithPrintAndLog(
       * tcContextHolder.getWorkflowService().performAction2(task, "SOA_EPM_complete_action",
       * commentVal, null, approve ? "SOA_EPM_approve" : "SOA_EPM_reject", signOff))) { return new
       * PerformActionResult(false, "操作执行失败"); }
       */
      return new PerformActionResult(true, "操作成功");
    } catch (Exception e) {
      log.error(e);
      return new PerformActionResult(false, "系统错误");
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public PerformActionResult finishDoTask(String taskUID, String commentVal) {
    try {
      EPMTask task = (EPMTask) tcContextHolder.getTcLoadService().loadObjectWithProperties(taskUID);

      // check type
      if (!(task instanceof EPMDoTask)) {
        return new PerformActionResult(false, "任务类型不正确");
      }

      // check status
      String status = task.get_real_state();
      if (status == null || !status.equals("Started")) {
        if ("Completed".equals(status)) {
          return new PerformActionResult(false, "任务已完成", true);
        } else {
          return new PerformActionResult(false, "任务状态不对");
        }
      }

      if (isExistErrorMessagesWithPrintAndLog(tcContextHolder.getWorkflowService().performAction2(task, "SOA_EPM_complete_action",
          commentVal, null, "SOA_EPM_completed", null))) {
        return new PerformActionResult(false, "操作执行失败");
      }
      return new PerformActionResult(true, "操作成功");
    } catch (Exception e) {
      log.error(e);
      return new PerformActionResult(false, "系统错误");
    }
  }

  @Override
  public PerformActionResult finishAcknowledgeTask(String taskUID, String commentVal) {
    try {
      EPMTask currentTask = (EPMTask) tcContextHolder.getTcLoadService().loadObjectWithProperties(taskUID);
      // check type
      if (!(currentTask instanceof EPMAcknowledgeTask)) {
        return new PerformActionResult(false, "任务类型不正确");
      }

      // get perform-signoffs task
      String uid = null;
      com.teamcenter.soa.client.model.ModelObject[] childTasks = currentTask.get_child_tasks();
      for (com.teamcenter.soa.client.model.ModelObject childTask : childTasks) {
        if (childTask instanceof EPMPerformSignoffTask) {
          uid = childTask.getUid();
          break;
        }
      }
      if (uid != null) {
        return signOffTask(uid, true, commentVal);
      }
      return new PerformActionResult(false, "任务结构不正确");
    } catch (Exception e) {
      log.error(e);
      return new PerformActionResult(false, "系统错误");
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public PerformActionResult setConditionTask(String taskUID, String condVal, String commentVal) {
    try {
      EPMTask task = (EPMTask) tcContextHolder.getTcLoadService().loadObjectWithProperties(taskUID);

      // check type
      if (!(task instanceof EPMConditionTask)) {
        return new PerformActionResult(false, "任务类型不正确");
      }

      // check status
      String status = task.get_real_state();
      if (status == null || !status.equals("Started")) {
        if ("Completed".equals(status)) {
          return new PerformActionResult(false, "任务已完成", true);
        } else {
          return new PerformActionResult(false, "任务状态不对");
        }
      }

      if (Boolean.parseBoolean(condVal)) {
        condVal = "SOA_EPM_true";
      } else {
        condVal = "SOA_EPM_false";
      }

      if (isExistErrorMessagesWithPrintAndLog(
          tcContextHolder.getWorkflowService().performAction2(task, "SOA_EPM_complete_action", commentVal, null, condVal, null))) {
        return new PerformActionResult(false, "操作执行失败");
      }
      return new PerformActionResult(true, "操作成功");
    } catch (Exception e) {
      log.error(e);
      return new PerformActionResult(false, "系统错误");
    }
  }

  @Override
  public boolean addJobAttachements(EPMJob job, String itemRevUid) {
    setObjectPolicy();
    if (job != null) {
      try {
        ItemRevision itemRev = tcContextHolder.getTcLoadService().getItemRevision(itemRevUid);
        AttachmentInfo attachInfo = new AttachmentInfo();
        attachInfo.attachment = new com.teamcenter.soa.client.model.ModelObject[] {itemRev};
        attachInfo.attachmentType = new int[] {EPM_attachement.target.value()};
        ServiceData sData = tcContextHolder.getWorkflowService().addAttachments(job.get_root_task(), attachInfo);
        if (!isExistErrorMessagesWithPrintAndLog(sData)) {
          return true;
        }
      } catch (NotLoadedException e) {
        e.printStackTrace();
      }
    }
    return false;
  }

  /**
   * 提前设置object policy, 相当于提前load property <br>
   * TODO: 经测试，无效果，待研究
   */
  private void setObjectPolicy() {
    ObjectPropertyPolicy policy = new ObjectPropertyPolicy();
    PolicyType itemRevType = new PolicyType("ItemRevision");
    PolicyProperty property = new PolicyProperty("process_stage_list");
    property.setModifier(PolicyProperty.WITH_PROPERTIES, true);
    itemRevType.addProperty(property);
    policy.addType(itemRevType);
    policy.addType("EPMTask", new String[] {"object_type", "object_name", "task_type", "task_template", "parent_process"});
    policy.addType("EPMJob", new String[] {"root_task"});
    tcContextHolder.getSessionService().setObjectPropertyPolicy(policy);
  }

  @Override
  public List<ModelObject> getTemplateList() {
    List<ModelObject> result = new ArrayList<>();
    List<String> argList = Arrays.asList("EPMTaskTemplate", "TYPE::EPMTaskTemplate::EPMTaskTemplate::POM_application_object", "0");
    ServiceData svcData =
        getTcContextHolder().getTcSOAService().invokeICTMethod("ICCTTaskTemplate", "extentTemplates", argList).serviceData;
    if (svcData.sizeOfPartialErrors() > 0) {
      return result;
    }
    for (int i = 0; i < svcData.sizeOfPlainObjects(); ++i) {
      result.add(ProxyUtil.spy(svcData.getPlainObject(i), getTcContextHolder()));
    }
    return result;
  }

  /**
   * 提升 或重新开始任务， 提升任务需要开旁路； 开始任务需要先将task责任人转为当前执行任务的人 <br>
   *
   * @param taskType
   * @param taskUid
   * @param action 5代表提升操作 2代表开始
   */
  private void executeTask(String taskType, String taskUid, int action, String comment) {
    List<String> params = Lists.newArrayList();
    params.add("EPMTask");
    params.add("TYPE::" + taskType + "::EPMTask::EPMTask");
    params.add(taskUid);
    params.add(String.valueOf(action));
    params.add(comment);// 注释 提升
    getTcContextHolder().getTcSOAService().invokeICTMethod("ICCTTask", "performAction", Lists.newArrayList(params));
  }

  @Override
  public void promoteTask(String taskType, String taskUid) {
    executeTask(taskType, taskUid, 5, "Auto promote task in workflow");
  }

  @Override
  public void startTask(String taskType, String taskUid) {
    executeTask(taskType, taskUid, 2, "Auto start task in workflow");
  }



  /**
   * 重新指派task任务的责任人, 较高权限才能指派
   *
   * @param taskUid 流程执行人
   * @param targetUserUid task任务重新指派后的新责任人
   */
  @Override
  public void reassignResponsiblePart(String taskUid, String targetUserUid) {
    ICT.Arg[] args = new ICT.Arg[6];
    for (int i = 0; i < args.length; i++) {
      args[i] = new ICT.Arg();
    }
    args[0].val = "EPMTask";
    args[1].val = "TYPE::EPMTask::EPMTask::WorkspaceObject";
    args[2].val = taskUid;
    args[3].val = BMIDE.PROP_RESP_USER;
    args[4].array = ICCTArgUtil.createEntry(targetUserUid).array;
    args[5].val = "-1";
    getTcContextHolder().getTcSOAService().invokeICTMethod("ICCT", "insertRelated", args);
  }

  /**
   * Reivew task 子节点也需要先提升选择人节点 再开始签审节点
   *
   * @param task 流程中的任务
   * @param currentLoginUserUid 高权限用户的UID
   */
  @SuppressWarnings("unchecked")
  private void promoteAndStartReviewSubTasks(ModelObject task, String currentLoginUserUid) {
    if (BMIDE.TYPE_REVIEW_TASK.equals(task.getTypeObject().getName())) {
      List<ModelObject> child_tasks = (List<ModelObject>) task.get(BMIDE.PROP_CHILD_TASKS);
      Map<String, ModelObject> type2TaskMap = new HashMap<>();
      for (ModelObject child : child_tasks) {
        log.info(">> {} - {}", child.getDisplayVal(BMIDE.PROP_OBJECT_NAME), child.getTypeObject().getName());
        type2TaskMap.put(child.getTypeObject().getName(), child);
      }
      ModelObject selectTask = type2TaskMap.get(BMIDE.TYPE_SELECT_SIGN_OFF_TASK);
      // 先提升任务 需要开旁路
      getTcContextHolder().getTcSOAService().openByPass();
      promoteTask(selectTask.getTypeObject().getName(), selectTask.getUid());
      getTcContextHolder().getTcSOAService().closeByPass();

      ModelObject signOffTask = type2TaskMap.get(BMIDE.TYPE_PERFORM_SIGN_OFF_TASK);
      ModelObject signOffRespUser = signOffTask.getModelObject(BMIDE.PROP_RESP_USER);
      reassignResponsiblePart(signOffTask.getUid(), currentLoginUserUid);
      // 再开始任务
      startTask(signOffTask.getTypeObject().getName(), signOffTask.getUid());
      reassignResponsiblePart(signOffTask.getUid(), signOffRespUser.getUid());
    }
  }

  /**
   * 为未开始流程提供API， task状态仅为 【待处理】时，才能将task开始，否则都会失败 <br>
   * EPMReviewTask 执行开始时，签审节中包含2个子结点，选择人（先提升）和审核（再开始）
   *
   * @param task 任务状态为待处理时，开始任务才会有效
   */
  @Override
  public void startToDoTask(com.teamcenter.soa.client.model.ModelObject taskObj) {
    ModelObject task = ProxyUtil.spy(taskObj, getTcContextHolder());
    ModelObject currentLoginUser =
        ProxyUtil.spy(SOAExecutionContext.current().getTcContextHolder().getTcSOAService().getUser(), getTcContextHolder());
    log.info("current login User info: {} / {} - {}", currentLoginUser.get(BMIDE.PROP_USER_ID), currentLoginUser.get(BMIDE.PROP_USER_NAME),
        currentLoginUser.getUid());
    // 必需优先获取原来的责任人，后续指派新人
    ModelObject responPartUser = task.getModelObject(BMIDE.PROP_RESP_USER);
    String userId = (String) responPartUser.get(BMIDE.PROP_USER_ID);
    String userName = (String) responPartUser.get(BMIDE.PROP_USER_NAME);
    log.info("the task ({}} responsible party user info: {} / {} - {}", task.getDisplayVal(BMIDE.PROP_TASK_STATE), userId, userName,
        responPartUser.getUid());

    // 将责任人指派给 当前登入 (wJftn7kaJMlDLB) DBA账号
    reassignResponsiblePart(task.getUid(), currentLoginUser.getUid());
    startTask(task.getTypeObject().getName(), task.getUid());
    // 将责任人重新指派给 原来的责任人
    reassignResponsiblePart(task.getUid(), responPartUser.getUid());
    promoteAndStartReviewSubTasks(task, currentLoginUser.getUid());
  }

  @Override
  public void addSignOff(EPMTask task, List<GroupMember> groupMembers) {
    List<Workflow.CreateSignoffInfo> signoffs = groupMembers.stream().map(user -> {
      Workflow.CreateSignoffInfo signoffInfo = new Workflow.CreateSignoffInfo();
      signoffInfo.signoffMember = user;
      signoffInfo.signoffAction = "SOA_EPM_Review";
      signoffInfo.originType = "SOA_EPM_ORIGIN_UNDEFINED";
      return signoffInfo;
    }).collect(Collectors.toList());
    Workflow.CreateSignoffInfo[] arrayOfCreateSignoffInfo = signoffs.toArray(new Workflow.CreateSignoffInfo[signoffs.size()]);
    Workflow.CreateSignoffs createSignoffs = new Workflow.CreateSignoffs();
    createSignoffs.signoffInfo = arrayOfCreateSignoffInfo;
    createSignoffs.task = task;
    Workflow.CreateSignoffs[] arrayOfCreateSignoffs = new Workflow.CreateSignoffs[1];
    arrayOfCreateSignoffs[0] = createSignoffs;
    try {
      getTcContextHolder().getWorkflowService().addSignoffs(arrayOfCreateSignoffs);
    } catch (ServiceException e) {
      e.printStackTrace();
    }
  }

    @Override
    public boolean setStatus(List<com.teamcenter.soa.client.model.ModelObject> modelObjects, String statusName) {
        ReleaseStatusOption[] releaseStatusOptions = new ReleaseStatusOption[1];
        releaseStatusOptions[0] = new ReleaseStatusOption();
        releaseStatusOptions[0].newReleaseStatusTypeName = statusName;// "Released";
        releaseStatusOptions[0].operation = "Append";// "Append";

        WorkspaceObject[] objectsToRelease = new WorkspaceObject[modelObjects.size()];
        for (int i = 0; i < modelObjects.size(); i++) {
            objectsToRelease[i] = (WorkspaceObject) modelObjects.get(i);
        }

        ReleaseStatusInput ip = new ReleaseStatusInput();
        ip.operations = releaseStatusOptions;
        ip.objects = objectsToRelease;

        ReleaseStatusInput[] releaseStatusInput = new ReleaseStatusInput[1];
        releaseStatusInput[0] = ip;
        try {
            getTcContextHolder().getWorkflowService().setReleaseStatus(releaseStatusInput);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


}

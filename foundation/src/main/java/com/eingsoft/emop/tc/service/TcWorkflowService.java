package com.eingsoft.emop.tc.service;

import java.util.List;
import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.service.impl.TcWorkflowServiceImpl.PerformActionResult;
import com.teamcenter.soa.client.model.strong.EPMJob;
import com.teamcenter.soa.client.model.strong.EPMTask;
import com.teamcenter.soa.client.model.strong.EPMTaskTemplate;
import com.teamcenter.soa.client.model.strong.GroupMember;

public interface TcWorkflowService extends TcService {
  /**
   * 获取所有工作流模版， 并返回加载所有属性的对象
   * 
   * @return
   */
  List<EPMTaskTemplate> getAllWorkflowTemplatesList();

  /**
   * 获取所有工作流模版， 并返回加载所有属性的对象
   * 
   * @return
   */
  List<EPMTaskTemplate> getAllWorkflowTemplates();

  /**
   * 获取当前工作流对象（加载所有属性后的对象）的所有任务列表
   * 
   * @param process
   * @param state 2 or 4
   * @return
   */
  List<EPMTask> getAllTasks(EPMJob process, int state);

  /**
   * 创建一个空的流程对象， 若创建失败 返回null
   * 
   * @param wfName 流程名
   * @param processTemplate 流程模板名
   * @return
   */
  EPMJob createNewProcess(String wfName, String processTemplate);

  /**
   * 创建新流程 This method is designed to create a New Process as if you used the Rich client interface
   * New Process dialog <ctrl-p>.
   * 
   * @param targetObjs 需要走流程的对象
   * @param wfName 流程名
   * @param templateName 流程模板名
   * @return
   */
  boolean createNewProcess(List<? extends com.teamcenter.soa.client.model.ModelObject> targetObjs, String wfName, String templateName);

  /**
   * 创建新流程 This method is different from createNewProcess in that the attachment is added at a later
   * time.
   * 
   * @return
   */
  boolean createNewProcess2(String itemRevUid, String wfName, String processTemplate);

  /**
   * 基于流程对象，给目标对象赋值
   * 
   * @param job
   * @param itemRevUid
   * @return
   */
  boolean addJobAttachements(EPMJob job, String itemRevUid);

  PerformActionResult signOffTask(String taskUID, boolean approve, String commentVal);

  PerformActionResult finishDoTask(String taskUID, String commentVal);

  PerformActionResult finishAcknowledgeTask(String taskUID, String commentVal);

  PerformActionResult setConditionTask(String taskUID, String condVal, String commentVal);

  /**
   * 获取当前最新的工作流程模板列表
   * 
   * @return
   */
  List<ModelObject> getTemplateList();

  void reassignResponsiblePart(String taskUid, String targetUserUid);

  void promoteTask(String taskType, String taskUid);

  void startTask(String taskType, String taskUid);

  void startToDoTask(com.teamcenter.soa.client.model.ModelObject task);

  /**
   * 添加签审
   */
  void addSignOff(EPMTask task, List<GroupMember> groupMembers);

  /***
   * 设置状态
   * @param modelObjects 设置状态的对象
   * @param statusName 状态名称 eg: Released, Approved
   */
  boolean setStatus(List<com.teamcenter.soa.client.model.ModelObject> modelObjects, String statusName);
}

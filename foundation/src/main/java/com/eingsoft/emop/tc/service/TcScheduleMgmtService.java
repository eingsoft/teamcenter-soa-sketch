package com.eingsoft.emop.tc.service;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import com.eingsoft.emop.tc.model.ModelObject;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.AttributeUpdateContainer;
import com.teamcenter.soa.client.model.strong.Schedule;
import com.teamcenter.soa.client.model.strong.ScheduleTask;
import com.teamcenter.soa.exceptions.NotLoadedException;

public interface TcScheduleMgmtService extends TcService {

  void findAllTasksBySchedule();

  /**
   * 针对 时间表任务发起流程
   *
   * @param scheduleTaskUid
   * @return 返回流程的job 对象
   */
  ModelObject launchScheduledWorkflow(String scheduleTaskUid);

  /**
   * 根据多个时间表任务发起多个流程
   *
   * @param scheduleTaskUids
   * @return 返回多个流程实例的job对象列表
   */
  List<ModelObject> launchScheduledWorkflows(String... scheduleTaskUids);

  /**
   * 根据schedule uids获取时间表和时间表任务
   *
   * @param uids
   * @return
   */
  Map<ModelObject, List<ModelObject>> getSchedulesWithTasks(String... uids);

  Map<ModelObject, List<ModelObject>> getSchedulesWithAllTasks(String[] uids);

  /**
   * 创建时间表任务
   *
   * @param schedule 时间表
   * @param parentTask 父任务，如为时间表第一层任务，则此处为Null
   * @param taskId 任务ID ""则自动指派
   * @param taskName 任务名称
   * @param taskDesc 任务描述
   * @param startDate 任务的开始时间
   * @param finishDate 任务的结束时间
   * @param fixedType 任务类型 FIXED_WORK = 0, FIXED_DURATION = 1, FIXED_RESOURCES=2, 里程碑需要设置为0
   * @param isMileStone 是否是里程碑
   * @return 若创建成功 则返回 ScheduleTask, 若创建失败 则返回 null
   */
  ModelObject createScheduleTask(Schedule schedule, ScheduleTask parentTask, String taskId, String taskName, String taskDesc,
      Calendar startDate, Calendar finishDate, String fixedType, Boolean isMileStone);

  ModelObject createSchedule(String name);


  /**
   * 创建时间表,如自动指派ID，则scheduleId=""
   *
   * @param scheduleId 时间表ID
   * @param scheduleName 时间表名称
   * @param scheduleDesc 时间表描述
   * @param startDate 开始时间
   * @param finishDate 结束时间
   * @param customerNumber 客户编码 可以为空
   * @param customerName 客户名称 可以为空
   * @return
   */
  ModelObject createSchedule(String scheduleId, String scheduleName, String scheduleDesc, Calendar startDate, Calendar finishDate,
      String customerNumber, String customerName);

  /**
   * 更新时间表
   *
   * @param schedule 时间表对象
   * @param containers 属性容器列表
   */
  void updateSchedule(Schedule schedule, List<AttributeUpdateContainer> containers);

  /**
   * 更新时间表任务的完成百分比
   *
   * @param scheduleTask 时间表任务
   * @param completePercent 完成百分比
   */
  void updateScheduleTaskCompletePercent(ScheduleTask scheduleTask, String completePercent);


  /**
   * 删除 Schedule 对象
   * @param schedule 即将删除的时间表对象
   * @param force 尝试开启旁路，以及修改 owner 来强制删除时间表
   * @throws ServiceException
   */
  void delete(Schedule schedule, boolean force) throws ServiceException, NotLoadedException;

  /**
   * 在时间表中删除时间表任务（单个）
   *
   * @param task
   */
  void deleteScheduleTask(ScheduleTask task);

  /**
   * 在时间表中删除时间表任务（多个）
   *
   * @param tasks
   */
  void deleteScheduleTasks(List<ScheduleTask> tasks);

  /**
   * [时间表]-任务交付件
   *
   * @param task 时间表任务
   * @param deliverable 时间表交付件
   * @return SchTaskDeliverable
   */
  ModelObject addTaskDeliverable(ScheduleTask task, com.teamcenter.soa.client.model.ModelObject deliverable);

  /**
   * [时间表]-时间表交付件
   *
   * @param schedule 时间表
   * @param deliverable 添加为交付件的对象
   * @return SchDeliverable
   */
  ModelObject addScheduleDeliverable(Schedule schedule, com.teamcenter.soa.client.model.ModelObject deliverable);

  /**
   * 为时间表任务指派资源
   *
   * @param task 时间表任务
   * @param resources 资源,可以是用户，也可以是学科
   */
  void assignTaskResources(ScheduleTask task, List<com.teamcenter.soa.client.model.ModelObject> resources);

  /**
   * 给时间表任务赋值 工作流模板
   *
   * @param scheduleTask
   * @param workflowTemplateUid
   */
  void updateScheduleTaskWorkflowTemplate(ScheduleTask scheduleTask, String workflowTemplateUid);

  AttributeUpdateContainer buildAttributeUpdateContainer(String attrName, String attrValue, int attrType);

  void updateScheduleTask(ScheduleTask scheduleTask, List<AttributeUpdateContainer> containers);

  ModelObject getScheduleByTask(ScheduleTask scheduleTask);

  /**
   * 针对时间表对象 打基线， 如果失败则抛出异常
   *
   * @param baselineName   时间表基线名称
   * @param schedule       时间表对象，不可为 null
   * @param parentBaseline 可为 null
   * @param isActive       是否活动的
   */
  void createBaselines(String baselineName, Schedule schedule, Schedule parentBaseline, boolean isActive);
}

package com.eingsoft.emop.tc.service;

import java.util.List;
import java.util.Map;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.GroupMember;
import com.teamcenter.soa.client.model.strong.TC_Project;
import com.teamcenter.soa.client.model.strong.User;
import com.teamcenter.soa.exceptions.NotLoadedException;

/**
 * TC project 指派 移除API
 *
 * @author king
 */
public interface TcProjectService extends TcService {

  /**
   * 将TC对象 与项目对象 指派关联
   * 
   * @param assignObjects
   * @param tcProjects 注意 对象类型 必需是 TC_Project
   * @return
   */
  boolean assignObjects(List<? extends ModelObject> assignObjects, List<? extends ModelObject> tcProjects);

  /**
   * 将TC对象 与 项目对象 移除关联
   * 
   * @param removeObjects
   * @param tcProjects 注意 对象类型 必需是 TC_Project
   * @return
   */
  boolean removeObjects(List<? extends ModelObject> removeObjects, List<? extends ModelObject> tcProjects);

  /**
   * TC中的对象与项目对象（TC_Project）进行指派关联、或移除关联
   * 
   * @param assignObjects
   * @param removeObjects
   * @param tcProjects 注意 对象类型 必需是 TC_Project
   * @return
   */
  boolean assignOrRemoveObjects(List<? extends ModelObject> assignObjects, List<? extends ModelObject> removeObjects,
      List<? extends ModelObject> tcProjects);

  /**
   * 创建项目对象，并且以当前登入用户所在的组作为项目成员
   * 
   * @param projectId
   * @param projectName
   * @param projectDesc
   * @param propMap
   * @return
   */
  TC_Project createSingleProject(String projectId, String projectName, String projectDesc, Map<String, List<String>> propMap);

  /**
   * 修改项目信息
   * 
   * @param project 项目对象
   * @param projectId 项目ID
   * @param projectName 项目名称
   * @param projectDesc 项目描述
   * @param teamMembers 项目成员
   * @param privileges 特权用户
   * @param teamAdministrators 小组管理员
   * @throws ServiceException
   */
  void modifyProjectTeams(TC_Project project, String projectId, String projectName, String projectDesc, GroupMember[] teamMembers,
      User[] privileges, User[] teamAdministrators);

  /**
   * 创建项目
   * 
   * @param projectId 项目ID
   * @param projectName 项目名称
   * @param projectDesc 项目描述
   * @param teamMembers 项目成员
   * @param privileges 特权用户
   * @param teamAdministrators 小组管理员
   * @return
   * @throws Exception
   */
  com.eingsoft.emop.tc.model.ModelObject createProject(String projectId, String projectName, String projectDesc, GroupMember[] teamMembers,
      User[] privileges, User[] teamAdministrators);

  /**
   * 删除 project 对象
   * tc 删除 project 对象需要拥有当前 project 的 项目管理员权限，即为当前项目的 owner
   * @param tc_project 即将删除的项目对象
   * @param force if true 将 project 的 owner 强制修改为当前 session 用户
   * @throws ServiceException
   */
  void delete(TC_Project tc_project, boolean force) throws ServiceException, NotLoadedException;
}

package com.eingsoft.emop.tc.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.util.ICCTArgUtil;
import com.google.common.collect.Lists;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.internal.strong.core._2011_06.ICT;
import com.teamcenter.soa.client.model.strong.Group;
import com.teamcenter.soa.exceptions.NotLoadedException;
import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcProjectService;
import com.eingsoft.emop.tc.util.ProxyUtil;
import com.teamcenter.services.strong.core.ProjectLevelSecurityService;
import com.teamcenter.services.strong.core._2007_09.ProjectLevelSecurity.AssignedOrRemovedObjects;
import com.teamcenter.services.strong.core._2010_09.DataManagement;
import com.teamcenter.services.strong.core._2012_09.ProjectLevelSecurity.ModifyProjectsInfo;
import com.teamcenter.services.strong.core._2012_09.ProjectLevelSecurity.ProjectInformation;
import com.teamcenter.services.strong.core._2012_09.ProjectLevelSecurity.ProjectOpsResponse;
import com.teamcenter.services.strong.core._2012_09.ProjectLevelSecurity.TeamMemberInfo;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.GroupMember;
import com.teamcenter.soa.client.model.strong.TC_Project;
import com.teamcenter.soa.client.model.strong.User;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ScopeDesc(Scope.TcContextHolder)
public class TcProjectServiceImpl implements TcProjectService {

  @Getter
  private final TcContextHolder tcContextHolder;

  public TcProjectServiceImpl(TcContextHolder tcContextHolder) {
    this.tcContextHolder = tcContextHolder;
  }

  @Override
  public boolean assignOrRemoveObjects(List<? extends ModelObject> assignObjects, List<? extends ModelObject> removeObjects,
      List<? extends ModelObject> tcProjects) {
    if (tcProjects == null || tcProjects.isEmpty()) {
      log.warn("the TC_Project param must not be null or empty, return false.");
      return false;
    }

    List<ModelObject> projects = tcProjects.stream().filter(pro -> pro instanceof TC_Project).collect(Collectors.toList());

    AssignedOrRemovedObjects input = new AssignedOrRemovedObjects();
    input.projects = projects.toArray(new TC_Project[tcProjects.size()]);

    if (assignObjects != null && !assignObjects.isEmpty()) {
      input.objectToAssign = assignObjects.toArray(new ModelObject[assignObjects.size()]);
    }

    if (removeObjects != null && !removeObjects.isEmpty()) {
      input.objectToRemove = removeObjects.toArray(new ModelObject[removeObjects.size()]);
    }

    ProjectLevelSecurityService service = getTcContextHolder().getProjectLevelSecurityService();
    ServiceData serviceData = service.assignOrRemoveObjects(new AssignedOrRemovedObjects[] {input});
    getTcContextHolder().printAndLogMessageFromServiceData(serviceData, false);
    return !(serviceData.sizeOfPartialErrors() > 0);
  }

  @Override
  public boolean assignObjects(List<? extends ModelObject> assignObjects, List<? extends ModelObject> tcProjects) {
    return assignOrRemoveObjects(assignObjects, null, tcProjects);
  }

  @Override
  public boolean removeObjects(List<? extends ModelObject> removeObjects, List<? extends ModelObject> tcProjects) {
    return assignOrRemoveObjects(null, removeObjects, tcProjects);
  }

  /**
   * 创建普通TC对象
   *
   * @param projectId
   * @param projectName
   * @param projectDesc
   * @param propMap 创建自定义属性
   * @return
   */
  @Override
  public TC_Project createSingleProject(String projectId, String projectName, String projectDesc, Map<String, List<String>> propMap) {
    User teamAdminUser = getTcContextHolder().getTcSOAService().getUser();
    GroupMember teamMember = getTcContextHolder().getTcSOAService().getGroupMember();
    ProjectInformation projectinformation = new ProjectInformation();

    projectinformation.clientId = projectinformation.toString();
    projectinformation.projectDescription = projectDesc;
    projectinformation.projectId = projectId;
    projectinformation.projectName = projectName;
    projectinformation.active = true;
    projectinformation.visible = true;
    projectinformation.useProgramContext = false;

    projectinformation.teamMembers = new TeamMemberInfo[2];

    // Add dba/dba_user grpmemeber as Regular Member
    projectinformation.teamMembers[0] = new TeamMemberInfo();
    projectinformation.teamMembers[0].teamMember = teamMember;
    projectinformation.teamMembers[0].teamMemberType = 0;

    // Add dba_user user as Project TeamAdmininstrator user
    projectinformation.teamMembers[1] = new TeamMemberInfo();
    projectinformation.teamMembers[1].teamMember = teamAdminUser;
    projectinformation.teamMembers[1].teamMemberType = 2;

    ProjectOpsResponse propsResp =
        getTcContextHolder().getProjectLevelSecurityService().createProjects(new ProjectInformation[] {projectinformation});

    getTcContextHolder().printAndLogMessageFromServiceData(propsResp.serviceData, true);

    // 给自定义属性设置值
    ModelObject modelObject = propsResp.serviceData.getCreatedObject(0);
    if (!propMap.isEmpty() && modelObject!=null) {
      DataManagement.PropInfo propInfo = getTcContextHolder().getTcSOAService().buildPropInfo(modelObject, propMap);
      getTcContextHolder().getTcSOAService().setProperties(Arrays.asList(propInfo));
    }

    return (TC_Project) ProxyUtil.spy(propsResp.projectOpsOutputs[0].project, tcContextHolder);
  }


  @Override
  public com.eingsoft.emop.tc.model.ModelObject createProject(String projectId, String projectName, String projectDesc,
      GroupMember[] teamMembers, User[] privileges, User[] teamAdministrators) {

    TeamMemberInfo[] allTeamMembers = getProjectTeams(teamMembers, privileges, teamAdministrators);

    ProjectLevelSecurityService projectService = tcContextHolder.getProjectLevelSecurityService();
    ProjectInformation[] projectInfos = new ProjectInformation[1];
    ProjectInformation projectInfo = new ProjectInformation();
    projectInfo.active = true;
    projectInfo.clientId = "PLS-RAC-SESSION";
    projectInfo.projectId = projectId;
    projectInfo.projectName = projectName;
    projectInfo.projectDescription = projectDesc;
    projectInfo.useProgramContext = false;
    projectInfo.visible = true;
    projectInfo.teamMembers = allTeamMembers;
    projectInfos[0] = projectInfo;
    ProjectOpsResponse response = projectService.createProjects(projectInfos);

    tcContextHolder.printAndLogMessageFromServiceData(response.serviceData, true);
    return response.serviceData.sizeOfCreatedObjects() > 0 ? ProxyUtil.spy(response.serviceData.getCreatedObject(0), tcContextHolder)
        : null;
  }


  @Override
  public void modifyProjectTeams(TC_Project project, String projectId, String projectName, String projectDesc, GroupMember[] teamMembers,
      User[] privileges, User[] teamAdministrators) {

    TeamMemberInfo[] allTeamMembers = getProjectTeams(teamMembers, privileges, teamAdministrators);

    ProjectLevelSecurityService projectService = tcContextHolder.getProjectLevelSecurityService();

    ModifyProjectsInfo[] input = new ModifyProjectsInfo[1];
    input[0] = new ModifyProjectsInfo();
    input[0].clientId = "PLS-RAC-SESSION";
    input[0].sourceProject = project;
    input[0].projectInfo = new ProjectInformation();
    input[0].projectInfo.active = true;
    input[0].projectInfo.clientId = "";
    input[0].projectInfo.useProgramContext = false;
    input[0].projectInfo.visible = true;
    input[0].projectInfo.projectId = projectId;
    input[0].projectInfo.projectName = projectName;
    input[0].projectInfo.projectDescription = projectDesc;
    input[0].projectInfo.teamMembers = allTeamMembers;

    ProjectOpsResponse response = projectService.modifyProjects(input);
    tcContextHolder.printAndLogMessageFromServiceData(response.serviceData, true);
  }

  /**
   * 解析项目团队成员
   * 
   * @param teamMembers 普通成员
   * @param privileges 特权用户
   * @param teamAdministrators 小组管理员
   * @return
   */
  private TeamMemberInfo[] getProjectTeams(GroupMember[] teamMembers, User[] privileges, User[] teamAdministrators) {
    List<TeamMemberInfo> teamMemberList = new ArrayList<TeamMemberInfo>();

    // 项目成员
    if (teamMembers != null) {
      for (GroupMember member : teamMembers) {
        // Team member (teamMember is a GroupMember or Group object)
        TeamMemberInfo info = new TeamMemberInfo();
        info.teamMemberType = 0;
        info.teamMember = member;
        teamMemberList.add(info);
      }
    }

    // 特权用户
    if (privileges != null) {
      for (User privilege : privileges) {
        // Privileged user (teamMember is a User object)
        TeamMemberInfo info = new TeamMemberInfo();
        info.teamMemberType = 1;
        info.teamMember = privilege;
        teamMemberList.add(info);
      }
    }

    // 小组管理员
    if (teamAdministrators != null) {
      for (User teamAdmin : teamAdministrators) {
        // Team administrator (teamMember is a User object)
        TeamMemberInfo info = new TeamMemberInfo();
        info.teamMemberType = 2;
        info.teamMember = teamAdmin;
        teamMemberList.add(info);
      }
    }

    TeamMemberInfo[] allTeamMembers = new TeamMemberInfo[teamMemberList.size()];
    for (int i = 0; i < teamMemberList.size(); i++) {
      TeamMemberInfo member = teamMemberList.get(i);
      allTeamMembers[i] = member;
    }
    return allTeamMembers;
  }

  /**
   * 删除 project 对象
   * tc 删除 project 对象需要拥有当前 project 的 项目管理员权限，即为当前项目的 owner
   * @param tc_project 即将删除的项目对象
   * @param force if true 将 project 的 owner 强制修改为当前 session 用户
   * @throws ServiceException
   */
  @Override
  public void delete(TC_Project tc_project, boolean force) throws ServiceException, NotLoadedException {

    // 已经拥有旁路权限？
    boolean hasPassBefore = tcContextHolder.getTcSOAService().hasByPass();
    if (force) {
      if (!hasPassBefore && tcContextHolder.getTcSOAService().getUser().get_is_member_of_dba()) {
        tcContextHolder.getTcSOAService().openByPass();
      }
      Group group = (Group) getTcContextHolder().getTcOrgnizationService().getUserGroups(getTcContextHolder().getTcSOAService().getUser()).stream().filter(
                      g -> g.get("display_name", String.class).equals("项目管理")
              ).findFirst().orElseThrow(
                      () -> new RuntimeException("Current session user isn't one of group (项目管理) members, can not change project owner to it.")
              );

      SOAExecutionContext.current().getTcContextHolder().getTcDataManagementService().changeOwnership(
              Lists.newArrayList(tc_project),
              getTcContextHolder().getTcSOAService().getUser(), group);
    }

    ICT.Arg[] args = new ICT.Arg[]{
            ICCTArgUtil.createArg("TC_Project"),
            ICCTArgUtil.createArg("TYPE::TC_Project::TC_Project::POM_application_object"),
            ICCTArgUtil.createArg(tc_project.getUid())};

    ICT.InvokeICTMethodResponse response = SOAExecutionContext.current().getTcContextHolder().getICTService().invokeICTMethod("ICCT", "destroy", args);
    ServiceData data = response.serviceData;
    // 在该方法中开启了旁路
    if (!hasPassBefore && tcContextHolder.getTcSOAService().hasByPass()) {
      getTcContextHolder().getTcSOAService().closeByPass();
    }
    SOAExecutionContext.current().getTcContextHolder().printAndLogMessageFromServiceData(data, true);
  }
}

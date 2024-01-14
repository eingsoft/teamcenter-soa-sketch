package com.eingsoft.emop.tc.service.impl;

import static com.eingsoft.emop.tc.util.ProxyUtil.proxy;
import static com.eingsoft.emop.tc.util.ProxyUtil.spy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;
import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcOrgnizationService;
import com.eingsoft.emop.tc.service.cache.ModelObjectCache;
import com.eingsoft.emop.tc.util.ICCTArgUtil;
import com.eingsoft.emop.tc.util.ProxyUtil;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.internal.strong.core._2011_06.ICT;
import com.teamcenter.services.internal.strong.core._2011_06.ICT.InvokeICTMethodResponse;
import com.teamcenter.services.strong.administration.UserManagementService;
import com.teamcenter.services.strong.administration._2012_09.UserManagement.GetUserGroupMembersInputData;
import com.teamcenter.services.strong.administration._2012_09.UserManagement.GetUserGroupMembersResponse;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.ImanQuery;
import com.teamcenter.soa.client.model.strong.User;
import com.teamcenter.soa.exceptions.NotLoadedException;

@Log4j2
@ScopeDesc(Scope.TcContextHolder)
@RequiredArgsConstructor
public class TcOrgnizationServiceImpl implements TcOrgnizationService {

    @Getter
    private final TcContextHolder tcContextHolder;

    @Override
    public List<ModelObject> getRootGroups() {
        ICT.Arg[] args = new ICT.Arg[2];
        for (int i = 0; i < args.length; i++) {
            args[i] = new ICT.Arg();
        }
        args[0].val = "Group";
        args[1].val = "TYPE::Group::Group::POM_group";
        try {
            InvokeICTMethodResponse response =
                tcContextHolder.getICTService().invokeICTMethod("ICCTGroup", "getRootGroups", args);
            ServiceData data = response.serviceData;
            tcContextHolder.printAndLogMessageFromServiceData(data);
            List<ModelObject> groups = new ArrayList<>();
            for (int i = 0; i < data.sizeOfPlainObjects(); i++) {
                groups.add(ProxyUtil.spy(data.getPlainObject(i), tcContextHolder));
            }
            return groups;
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ModelObject> getChildGroups(String groupUid) {
        ICT.Arg[] args = new ICT.Arg[3];
        for (int i = 0; i < args.length; i++) {
            args[i] = new ICT.Arg();
        }
        args[0].val = "Group";
        args[1].val = "TYPE::Group::Group::POM_group";
        args[2].val = groupUid;
        try {
            InvokeICTMethodResponse response =
                tcContextHolder.getICTService().invokeICTMethod("ICCTGroup", "getChildGroups", args);
            ServiceData data = response.serviceData;
            tcContextHolder.printAndLogMessageFromServiceData(data);
            List<ModelObject> groups = new ArrayList<>();
            for (int i = 0; i < data.sizeOfPlainObjects(); i++) {
                groups.add(ProxyUtil.spy(data.getPlainObject(i), tcContextHolder));
            }
            return groups;
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ModelObject> getGroupMembers(String groupUid, String roleUid) {
        ICT.Arg[] args = new ICT.Arg[5];
        for (int i = 0; i < args.length; i++) {
            args[i] = new ICT.Arg();
        }
        args[0].val = "GroupMember";
        args[1].val = "TYPE::GroupMember::GroupMember::POM_member";
        args[2] = ICCTArgUtil.createStructure("");
        args[3] = ICCTArgUtil.createStructure(groupUid);
        args[4] = ICCTArgUtil.createStructure(roleUid);
        try {
            InvokeICTMethodResponse response =
                tcContextHolder.getICTService().invokeICTMethod("ICCTGroupMember", "getGroupMembers", args);
            ServiceData data = response.serviceData;
            tcContextHolder.printAndLogMessageFromServiceData(data);
            List<ModelObject> groups = new ArrayList<>();
            for (int i = 0; i < data.sizeOfPlainObjects(); i++) {
                groups.add(ProxyUtil.spy(data.getPlainObject(i), tcContextHolder));
            }
            return groups.stream().map(g -> g.getModelObject("the_user")).collect(Collectors.toList());
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ModelObject> getUserGroups(User user) {
        UserManagementService service = UserManagementService.getService(getTcContextHolder().getConnection());
        GetUserGroupMembersInputData data = new GetUserGroupMembersInputData();
        data.user = user;
        data.includeInactive = false;
        try {
            GetUserGroupMembersResponse response =
                service.getUserGroupMembers(new GetUserGroupMembersInputData[] {data});
            return Arrays.asList(response.outputs[0].memebrs).stream().map(g -> spy(g.group, tcContextHolder))
                .collect(Collectors.toList());
        } catch (ServiceException e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    private ImanQuery findUserQuery = null;

    private ImanQuery getFindUserQuery() {
        if (findUserQuery == null) {
            findUserQuery = tcContextHolder.getTcQueryService().getQueryByName("__WEB_find_user");
            if (findUserQuery == null) {
                throw new IllegalStateException("cannot find tc embedded query __WEB_find_user");
            }
        }
        return findUserQuery;
    }

    @Override
    public User findUserByUserId(String userId) {
        ModelObjectCache cache = SOAExecutionContext.current().getModelObjectCache();
        if (cache.retrieve(userId) != null) {
            return (User)cache.retrieve(userId);
        }
        List<User> users =
            tcContextHolder.getTcQueryService().executeQuery(getFindUserQuery(), Arrays.asList("用户 ID"),
                Arrays.asList(userId), User.class);
        if (users.size() > 0) {
            User user = proxy(users.get(0), tcContextHolder);
            cache.put(user.getUid(), (ModelObject)user);
            try {
                cache.put(user.get_user_id(), (ModelObject)user);
            } catch (NotLoadedException e) {
                // ignore
                log.info("not expected, fail to load user by userId " + userId, e);
            }
            return user;
        } else {
            return null;
        }
    }
}

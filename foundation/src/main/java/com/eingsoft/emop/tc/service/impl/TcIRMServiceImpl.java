package com.eingsoft.emop.tc.service.impl;

import com.eingsoft.emop.tc.BMIDE;
import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcIRMService;
import com.eingsoft.emop.tc.util.ProxyUtil;
import com.google.common.base.Strings;
import com.teamcenter.services.strong.administration.IRMService;
import com.teamcenter.services.strong.administration._2006_03.IRM.CheckAccessorPrivilegesResponse;
import com.teamcenter.services.strong.administration._2006_03.IRM.Privilege;
import com.teamcenter.services.strong.administration._2006_03.IRM.PrivilegeReport;
import com.teamcenter.services.strong.core.SessionService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.User;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ScopeDesc(Scope.TcContextHolder)
public class TcIRMServiceImpl implements TcIRMService {
    private static final String WRITE = "WRITE";

    @Getter
    private final TcContextHolder tcContextHolder;

    public TcIRMServiceImpl(TcContextHolder tcContextHolder) {
        this.tcContextHolder = tcContextHolder;
    }

    private boolean hasSpecificyPermission(ModelObject object, String privilegeName) {
        IRMService irmService = getTcContextHolder().getIRMService();
        SessionService sessionService = getTcContextHolder().getSessionService();
        try {
            ModelObject groupMember = sessionService.getSessionGroupMember().groupMember;
            if (groupMember == null) {
                return false;
            }

            CheckAccessorPrivilegesResponse response = irmService.checkAccessorsPrivileges(groupMember,
                new ModelObject[] {object}, new String[] {privilegeName});

            getTcContextHolder().printAndLogMessageFromServiceData(response.serviceData);

            PrivilegeReport[] report = response.privilegeReports;
            Privilege[] privileges = report[0].privilegeInfos;
            if ((privileges == null) || (privileges.length == 0)) {
                return false;
            }
            Privilege privilege = privileges[0];
            String name = privilege.privilegeName;
            if (!name.equalsIgnoreCase(privilegeName)) {
                return false;
            }
            boolean flag = privilege.verdict;
            User user = sessionService.getTCSessionInfo().user;
            log.info("the user {}({}) with {} permisssion is {} for the tc object {}.", user.get_user_id(),
                user.get_user_name(), privilegeName, flag,
                ProxyUtil.spy(object, tcContextHolder).get(BMIDE.PROP_OBJECT_STRING));
            return flag;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean hasPrivilege(ModelObject object, String privilegeName) {
        if (object == null || Strings.isNullOrEmpty(privilegeName)) {
            return false;
        }
        boolean flag = hasSpecificyPermission(object, privilegeName);

        return flag;
    }

    @Override
    public boolean hasPrivilege(String uid, String privilegeName) {
        ModelObject object = getTcContextHolder().getTcLoadService().loadObject(uid);
        return hasPrivilege(object, privilegeName);
    }

    @Override
    public boolean hasWritePrivilege(String uid) {
        return hasPrivilege(uid, WRITE);
    }

    @Override
    public boolean hasWritePrivilege(ModelObject object) {
        return hasPrivilege(object, WRITE);
    }

}
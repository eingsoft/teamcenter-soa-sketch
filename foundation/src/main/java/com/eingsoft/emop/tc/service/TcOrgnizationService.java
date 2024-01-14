package com.eingsoft.emop.tc.service;

import java.util.List;

import com.eingsoft.emop.tc.model.ModelObject;
import com.teamcenter.soa.client.model.strong.User;

public interface TcOrgnizationService extends TcService {

    /**
     * The groups under root node, it is the first level groups
     */
    List<ModelObject> getRootGroups();

    /**
     * The groups under the given group
     */
    List<ModelObject> getChildGroups(String groupUid);

    /**
     * get users under the given group and role
     */
    List<ModelObject> getGroupMembers(String groupUid, String roleUid);

    /**
     * Get the user's groups
     */
    List<ModelObject> getUserGroups(User user);

    /**
     * find user
     */
    User findUserByUserId(String userId);
}

package com.eingsoft.emop.tc.service;

import com.teamcenter.soa.client.model.ModelObject;

/**
 * ACL 权限问题
 *
 * @author king
 */
public interface TcIRMService extends TcService {
    /**
     * 判断对象是否有指定权限 <br>
     * 
     * ADD_CONTENT ASSIGN_TO_PROJECT Administer_ADA_Licenses BATCH_PRINT CHANGE CHANGE_OWNER CICO COPY DELETE DEMOTE
     * DIGITAL_SIGN EXPORT IMPORT IP_ADMIN IP_Classifier ITAR_ADMIN ITAR_Classifier MARKUP PROMOTE PUBLISH READ
     * REMOTE_CICO REMOVE_CONTENT REMOVE_FROM_PROJECT SUBSCRIBE TRANSFER_IN TRANSFER_OUT TRANSLATION UNMANAGE WRITE
     * WRITE_ICOS
     * 
     * @param uid 对象的UID
     * @param privilegeName 权限名称
     * @return
     */
    boolean hasPrivilege(String uid, String privilegeName);

    /**
     * 判断对象是否有指定权限 <br>
     * 
     * ADD_CONTENT ASSIGN_TO_PROJECT Administer_ADA_Licenses BATCH_PRINT CHANGE CHANGE_OWNER CICO COPY DELETE DEMOTE
     * DIGITAL_SIGN EXPORT IMPORT IP_ADMIN IP_Classifier ITAR_ADMIN ITAR_Classifier MARKUP PROMOTE PUBLISH READ
     * REMOTE_CICO REMOVE_CONTENT REMOVE_FROM_PROJECT SUBSCRIBE TRANSFER_IN TRANSFER_OUT TRANSLATION UNMANAGE WRITE
     * WRITE_ICOS
     * 
     * @param object
     * @param privilegeName 权限名称
     * @return
     */
    boolean hasPrivilege(ModelObject object, String privilegeName);

    /**
     * 对于当前登入用户，取权限最大的组、角色，判断对当前对象是否有写权限, <br>
     * 如有DBA角色、项目管理角色，项目管理角色 没有写权限，但DBA有写权限，无论当前选择的组是哪个组，均有写权限 <br>
     * 
     * 对于当前 登入用户如果是DBA用户，且开启了旁路，对于判断是否有权限与开旁路无关，执行上述权限规则 ，<br>
     * 如开启了旁路权限，对于没有写权限的对象（已发布）, 则依然没有写权限 <br>
     * 
     * @param uid
     * @return
     */
    boolean hasWritePrivilege(String uid);

    /**
     * 对于当前登入用户，取权限最大的组、角色，判断对当前对象是否有写权限, <br>
     * 如有DBA角色、项目管理角色，项目管理角色 没有写权限，但DBA有写权限，无论当前选择的组是哪个组，均有写权限 <br>
     * 
     * 对于当前 登入用户如果是DBA用户，且开启了旁路，对于判断是否有权限与开旁路无关，执行上述权限规则 ，<br>
     * 如开启了旁路权限，对于没有写权限的对象（已发布）, 则依然没有写权限 <br>
     * 
     * @param object
     * @return
     */
    boolean hasWritePrivilege(ModelObject object);
}

package com.eingsoft.emop.tc.service;

import java.util.List;
import java.util.Map;

import com.eingsoft.emop.tc.model.ModelObject;
import com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship;
import com.teamcenter.services.strong.core._2007_09.DataManagement.ExpandGRMRelationsPref2;
import com.teamcenter.soa.client.model.strong.ItemRevision;

public interface TcRelationshipService extends TcService {

    /**
     * 对象与对象之间绑定关联关系, 与RCP API类似于：primary.add(relationType, secondary);
     * 
     * @param primary
     * @param secondary
     * @param relationType
     */
    void buildRelation(com.teamcenter.soa.client.model.ModelObject primary,
        com.teamcenter.soa.client.model.ModelObject secondary, String relationType);

    /**
     * 批量对象与对象之间建立关系
     * 
     * @param relations
     */
    void batchBuildRelation(List<Relationship> relations);

    /**
     * 在零组件版本下返回渲染关系相关的所有对象
     * 
     * @param itemRev
     * @return
     */
    List<ModelObject> getRenderingRelatedObjs(ItemRevision itemRev);

    /**
     * 找到零组件版本下渲染关系的 直接模型对象
     * 
     * @param itemRev
     * @return
     */
    ModelObject getRenderingRelatedDirectModel(ItemRevision itemRev);

    /**
     * 获取对象下面所有关系的所有对象类型
     * 
     * @param itemUid
     * @return
     */
    List<ModelObject> findAllRelatedModelObjs(String uid);

    /**
     * 获取对象下面所有关系的所有对象类型
     * 
     * @param itemUid
     * @return
     */
    List<ModelObject> findAllRelatedModelObjs(com.teamcenter.soa.client.model.ModelObject obj);

    /**
     * 根据关系获取对象下面的子对象, 如果relationType 为空， 则返回所有关系对象， 否则返回指定关系对象。
     * 
     * @param uid 对象的UID的值
     * @param relationType 主对象与次对象业务的关系
     * @return
     */
    List<ModelObject> findAllRelatedModelObjsByRelation(String uid, String relationType);

    /**
     * 根据关系获取对象下面的子对象, 如果relationType 为空， 则返回所有关系对象， 否则返回指定关系对象。
     * 
     * @param uids 对象的UID列表的值，根据UID LOAD Model Object再去寻找相关对象
     * @param relationType 主对象与次对象业务的关系
     * @return
     */
    Map<ModelObject, List<ModelObject>> findAllRelatedModelObjsByUidsAndRelName(List<String> uids, String relationType);

    /**
     * 根据关系获取对象下面的子对象, 如果relationType 为空， 则返回所有关系对象， 否则返回指定关系对象。
     * 
     * @param uid 对象的UID的值
     * @param relationType 主对象与次对象业务的关系
     * @return
     */
    List<ModelObject> findAllRelatedModelObjsByRelation(com.teamcenter.soa.client.model.ModelObject modelObj,
        String relationType);

    /**
     * 根据关系获取对象下面的子对象, 如果relationType 为空， 则返回所有关系对象， 否则返回指定关系对象。<br>
     * 
     * @param uid 对象的UID的值
     * @param relationType 主对象与次对象业务的关系
     * @return 返回所有子输入对象相关关系的集合Map
     */
    Map<ModelObject, List<ModelObject>> findAllRelatedModelObjsByObjsAndRelName(
        List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjs, String relationType);

    /**
     * 根据对象列表查找指定关系和指定对象类型的对象。 <br>
     * 如果关系为空，则查找对象下的所有关系; <br>
     * 如果指定对象类型为空， 则返回所关系下的对象； <br>
     * 如果关系和返回的对象类型都为空， 则返回对象下的所有对象；<br>
     * 
     * @param modelObjs
     * @param relationType
     * @param relatedObjType 可以是空，也可以是逗号分隔的多个类型
     * @return
     */
    Map<ModelObject, List<ModelObject>> findAllRelatedModelObjsByRelationAndType(
        List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjs, String relationType,
        String relatedObjType);

    /**
     * 根据对象列表查找指定关系和指定对象类型的对象。 <br>
     * 如果关系为空，则查找对象下的所有关系; <br>
     * 如果指定对象类型为空， 则返回所关系下的对象； <br>
     * 如果关系和返回的对象类型都为空， 则返回对象下的所有对象；<br>
     * 
     * @param modelObjs
     * @param relationType
     * @param relatedObjType
     * @param pref2
     * @return
     */
    Map<ModelObject, List<ModelObject>> findAllRelatedModelObjsByRelationAndType(
        List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjs, String relationType,
        String relatedObjType, ExpandGRMRelationsPref2 pref2);

    /**
     * Build 关系查找对象， 如果RelationType为空，使用此条件，则返回所有关系对象。
     * 
     * @param relationType
     * @return
     */
    ExpandGRMRelationsPref2 buildRelationPref2(String relationType);

    /**
     * 查找引用对象, 包括子类型， 东重使用此方法时会将并行的也搜索出来，可能BUG
     * 
     * @param objs
     * @param relType
     * @param objTypes
     * @param level
     * @return
     */
    @Deprecated
    Map<ModelObject, List<ModelObject>> findWhereReferenced(
        List<? extends com.teamcenter.soa.client.model.ModelObject> objs, String relType, List<String> objTypes,
        int level);

    /**
     * 查找引用对象, 匹配精确类型，使用equals type查找，不包含子类型
     * 
     * @param objs
     * @param relType
     * @param objTypes
     * @param level
     * @param isPreciseType
     * @return
     */
    Map<ModelObject, List<ModelObject>> findWhereReferenced(
        List<? extends com.teamcenter.soa.client.model.ModelObject> objs, String relType, List<String> objTypes,
        int level, boolean isPreciseType);

    void deleteRelation(com.teamcenter.soa.client.model.ModelObject primary,
        com.teamcenter.soa.client.model.ModelObject secondary, String relationType);

    void batchDeleteRelation(List<Relationship> relations);

    Relationship initRelationShip(com.teamcenter.soa.client.model.ModelObject primary,
        com.teamcenter.soa.client.model.ModelObject secondary, String relationType);
}

package com.eingsoft.emop.tc.service.impl;

import static com.eingsoft.emop.tc.BMIDE.REL_RENDERING;
import static com.eingsoft.emop.tc.util.ProxyUtil.proxy;
import static com.eingsoft.emop.tc.util.ProxyUtil.spy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;
import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcRelationshipService;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core._2006_03.DataManagement.CreateRelationsResponse;
import com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship;
import com.teamcenter.services.strong.core._2007_01.DataManagement.WhereReferencedInfo;
import com.teamcenter.services.strong.core._2007_01.DataManagement.WhereReferencedOutput;
import com.teamcenter.services.strong.core._2007_01.DataManagement.WhereReferencedResponse;
import com.teamcenter.services.strong.core._2007_06.DataManagement.RelationAndTypesFilter;
import com.teamcenter.services.strong.core._2007_09.DataManagement.ExpandGRMRelationsData2;
import com.teamcenter.services.strong.core._2007_09.DataManagement.ExpandGRMRelationsOutput2;
import com.teamcenter.services.strong.core._2007_09.DataManagement.ExpandGRMRelationsPref2;
import com.teamcenter.services.strong.core._2007_09.DataManagement.ExpandGRMRelationsResponse2;
import com.teamcenter.services.strong.core._2007_09.DataManagement.ExpandGRMRelationship;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.Type;
import com.teamcenter.soa.client.model.strong.DirectModel;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.client.model.strong.WorkspaceObject;
import com.teamcenter.soa.exceptions.NotLoadedException;

@ScopeDesc(Scope.TcContextHolder)
@Log4j2
public class TcRelationshipServiceImpl implements TcRelationshipService {
    @Getter
    private TcContextHolder tcContextHolder;

    public TcRelationshipServiceImpl(TcContextHolder tcContextHolder) {
        this.tcContextHolder = tcContextHolder;
    }

    /**
     * 对象与对象之间绑定关联关系, 与RCP API类似于：primary.add(relationType, secondary);
     * 
     * @param primary
     * @param secondary
     * @param relationType
     */
    @Override
    public void buildRelation(com.teamcenter.soa.client.model.ModelObject primary,
        com.teamcenter.soa.client.model.ModelObject secondary, String relationType) {
        Relationship relation = initRelationShip(primary, secondary, relationType);
        batchBuildRelation(Arrays.asList(relation));
    }

    /**
     * 批量对象与对象之间建立关系
     * 
     * @param relations
     */
    @Override
    public void batchBuildRelation(List<Relationship> relations) {
        CreateRelationsResponse response = this.tcContextHolder.getDataManagementService()
            .createRelations(relations.toArray(new Relationship[relations.size()]));
        tcContextHolder.printAndLogMessageFromServiceData(response.serviceData);
    }

    @Override
    public void deleteRelation(com.teamcenter.soa.client.model.ModelObject primary,
        com.teamcenter.soa.client.model.ModelObject secondary, String relationType) {
        Relationship relation = initRelationShip(primary, secondary, relationType);
        batchDeleteRelation(Arrays.asList(relation));
    }

    @Override
    public Relationship initRelationShip(com.teamcenter.soa.client.model.ModelObject primary,
        com.teamcenter.soa.client.model.ModelObject secondary, String relationType) {
        Relationship relation = new Relationship();
        relation.primaryObject = primary;
        relation.secondaryObject = secondary;
        relation.relationType = relationType;
        return relation;
    }

    @Override
    public void batchDeleteRelation(List<Relationship> relations) {
        ServiceData serviceData = this.tcContextHolder.getDataManagementService()
            .deleteRelations(relations.toArray(new Relationship[relations.size()]));
        tcContextHolder.printAndLogMessageFromServiceData(serviceData);
    }

    /**
     * 在零组件版本下返回渲染关系相关的所有对象
     * 
     * @param itemRev
     * @return
     */
    @Override
    public List<ModelObject> getRenderingRelatedObjs(ItemRevision itemRev) {
        itemRev = (ItemRevision)tcContextHolder.getTcLoadService().loadProperty(itemRev, REL_RENDERING);
        com.teamcenter.soa.client.model.ModelObject[] modelObjects = null;
        try {
            modelObjects = itemRev.get_IMAN_Rendering();
        } catch (NotLoadedException e) {
            e.printStackTrace();
        }
        return spy(Arrays.asList(modelObjects), tcContextHolder);
    }

    /**
     * 找到零组件版本下渲染关系的 直接模型对象
     * 
     * @param itemRev
     * @return
     */
    @Override
    public ModelObject getRenderingRelatedDirectModel(ItemRevision itemRev) {
        ModelObject directModel = null;
        for (ModelObject modelObject : getRenderingRelatedObjs(itemRev)) {
            if (modelObject instanceof DirectModel) {
                directModel = spy(modelObject, tcContextHolder);
                break;
            }
        }
        return directModel;
    }

    /**
     * 获取对象下面所有关系的所有对象类型
     * 
     * @param itemUid
     * @return
     */
    @Override
    public List<ModelObject> findAllRelatedModelObjs(String uid) {
        return findAllRelatedModelObjsByRelation(uid, null);
    }

    /**
     * 获取对象下面所有关系的所有对象类型
     * 
     * @param itemUid
     * @return
     */
    @Override
    public List<ModelObject> findAllRelatedModelObjs(com.teamcenter.soa.client.model.ModelObject modelObj) {
        return findAllRelatedModelObjsByRelation(modelObj, null);
    }

    /**
     * 根据关系获取对象下面的子对象, 如果relationType 为空， 则返回所有关系对象， 否则返回指定关系对象。
     * 
     * @param uid 对象的UID的值
     * @param relationType 主对象与次对象业务的关系
     * @return
     */
    @Override
    public List<ModelObject> findAllRelatedModelObjsByRelation(String uid, String relationType) {
        ModelObject modelObj = tcContextHolder.getTcLoadService().loadObjectWithProperties(uid);
        return findAllRelatedModelObjsByRelation(modelObj, relationType);
    }

    /**
     * 根据关系获取对象下面的子对象, 如果relationType 为空， 则返回所有关系对象， 否则返回指定关系对象。
     * 
     * @param uids 对象的UID列表的值，根据UID LOAD Model Object再去寻找相关对象
     * @param relationType 主对象与次对象业务的关系
     * @return
     */
    @Override
    public Map<ModelObject, List<ModelObject>> findAllRelatedModelObjsByUidsAndRelName(List<String> uids,
        String relationType) {
        List<ModelObject> modelObjs = tcContextHolder.getTcLoadService().loadObjects(uids);
        return findAllRelatedModelObjsByObjsAndRelName(modelObjs, relationType);
    }

    /**
     * 根据关系获取对象下面的子对象, 如果relationType 为空， 则返回所有关系对象， 否则返回指定关系对象。
     * 
     * @param uid 对象的UID的值
     * @param relationType 主对象与次对象业务的关系
     * @return
     */
    @Override
    public List<ModelObject> findAllRelatedModelObjsByRelation(com.teamcenter.soa.client.model.ModelObject modelObj,
        String relationType) {
        Map<ModelObject, List<ModelObject>> map =
            findAllRelatedModelObjsByObjsAndRelName(Arrays.asList(modelObj), relationType);
        return map.get(modelObj);
    }

    /**
     * 根据关系获取对象下面的子对象, 如果relationType 为空， 则返回所有关系对象， 否则返回指定关系对象。<br>
     * 
     * @param uid 对象的UID的值
     * @param relationType 主对象与次对象业务的关系
     * @return 返回所有子输入对象相关关系的集合Map
     */
    @Override
    public Map<ModelObject, List<ModelObject>> findAllRelatedModelObjsByObjsAndRelName(
        List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjs, String relationType) {
        return findAllRelatedModelObjsByRelationAndType(modelObjs, relationType, null);
    }

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
    @Override
    public Map<ModelObject, List<ModelObject>> findAllRelatedModelObjsByRelationAndType(
        List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjs, String relationType,
        String relatedObjType) {
        ExpandGRMRelationsPref2 pref2 = buildRelationPref2(relationType);
        return findAllRelatedModelObjsByRelationAndType(modelObjs, relationType, relatedObjType, pref2);
    }

    /**
     * 根据对象列表查找指定关系和指定对象类型的对象。 <br>
     * 如果关系为空，则查找对象下的所有关系; <br>
     * 如果指定对象类型为空， 则返回所关系下的对象； <br>
     * 如果关系和返回的对象类型都为空， 则返回对象下的所有对象；<br>
     * 
     * @param modelObjs
     * @param relationType
     * @param relatedObjType 如： Part_0_Revision_alt(Part Revision类型的存储类) or ItemRevision
     * @param pref2
     * @return
     */
    @Override
    public Map<ModelObject, List<ModelObject>> findAllRelatedModelObjsByRelationAndType(
        List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjs, String relationType,
        String relatedObjType, ExpandGRMRelationsPref2 pref2) {
        ExpandGRMRelationsResponse2 response = tcContextHolder.getDataManagementService().expandGRMRelationsForPrimary(
            modelObjs.toArray(new com.teamcenter.soa.client.model.ModelObject[modelObjs.size()]), pref2);
        getTcContextHolder().printAndLogMessageFromServiceData(response.serviceData);
        if ((response.output == null) || (response.output.length == 0)) {
            return Collections.emptyMap();
        }

        List<String> objTypeList = new ArrayList<String>();
        if (relatedObjType != null) {
            objTypeList = Arrays.stream(relatedObjType.split(",")).map(o -> o.trim()).filter(o -> !o.isEmpty())
                .collect(Collectors.toList());
        }

        Map<ModelObject, List<ModelObject>> secondModelObjMap = new HashMap<>();
        ExpandGRMRelationsOutput2[] output2s = response.output;
        for (int i = 0; i < output2s.length; i++) {
            ExpandGRMRelationsData2[] relData = output2s[i].relationshipData;
            if (relData.length <= 0) {
                continue;
            }

            List<ModelObject> secondsModelObjs = new ArrayList<>();
            if ((relationType == null || relationType.trim().isEmpty())
                || relData[0].relationName.equals(relationType)) {
                ExpandGRMRelationship[] objects = relData[0].relationshipObjects;
                for (ExpandGRMRelationship obj : objects) {
                    if (obj.otherSideObject == null) {
                        log.warn("{} is referencing to a deleted object?", obj.relation);
                        continue;
                    }
                    ModelObject modelObject = spy(obj.otherSideObject, tcContextHolder);

                    if (objTypeList.isEmpty()) {
                        secondsModelObjs.add(modelObject);
                        continue;
                    }

                    Type type = modelObject.getTypeObject();
                    for (String objType : objTypeList) {
                        if (type.isInstanceOf(objType)) {
                            secondsModelObjs.add(modelObject);
                            break;
                        }
                    }
                }
            }
            if (secondsModelObjs.size() > 0) {
                secondModelObjMap.put(spy(output2s[i].inputObject, tcContextHolder),
                    proxy(secondsModelObjs, tcContextHolder));
            }
        }

        return secondModelObjMap;
    }

    /**
     * Build 关系查找对象， 如果RelationType为空，使用此条件，则返回所有关系对象。
     * 
     * @param relationType
     * @return
     */
    @Override
    public ExpandGRMRelationsPref2 buildRelationPref2(String relationType) {
        RelationAndTypesFilter filter = new RelationAndTypesFilter();
        if (relationType == null || relationType.trim().isEmpty()) {
            filter.relationTypeName = "";
        } else {
            filter.relationTypeName = relationType;
        }
        ExpandGRMRelationsPref2 pref2 = new ExpandGRMRelationsPref2();
        pref2.expItemRev = true;
        pref2.returnRelations = true;
        pref2.info = new RelationAndTypesFilter[] {filter};
        return pref2;
    }

    @Override
    public Map<ModelObject, List<ModelObject>> findWhereReferenced(
        List<? extends com.teamcenter.soa.client.model.ModelObject> objs, String relType, List<String> objTypes,
        int level) {
        Map<ModelObject, List<ModelObject>> objMap = new HashMap<>();
        DataManagementService dmService = tcContextHolder.getDataManagementService();
        WhereReferencedResponse response =
            dmService.whereReferenced(objs.stream().toArray(WorkspaceObject[]::new), level);
        for (WhereReferencedOutput output : response.output) {
            List<ModelObject> refObjs = new ArrayList<ModelObject>();
            for (WhereReferencedInfo info : output.info) {
                // int curLvl = info.level;
                String curRel = info.relation;
                WorkspaceObject curObj = info.referencer;

                if (relType != null && !relType.isEmpty() && !relType.equals(curRel)) {
                    continue;
                }

                if (objTypes.size() > 0) {
                    boolean matchType = false;
                    Type type = curObj.getTypeObject();
                    for (String objType : objTypes) {
                        if (type.isInstanceOf(objType)) {
                            matchType = true;
                            break;
                        }
                    }
                    if (!matchType) {
                        continue;
                    }
                }

                refObjs.add(spy(curObj, tcContextHolder));
            }
            objMap.put(spy(output.inputObject, tcContextHolder), refObjs);
        }
        return objMap;
    }

    @Override
    public Map<ModelObject, List<ModelObject>> findWhereReferenced(
        List<? extends com.teamcenter.soa.client.model.ModelObject> objs, String relType, List<String> objTypes,
        int level, boolean isPreciseType) {
        Map<ModelObject, List<ModelObject>> objMap = new HashMap<>();
        DataManagementService dmService = tcContextHolder.getDataManagementService();
        WhereReferencedResponse response =
            dmService.whereReferenced(objs.stream().toArray(WorkspaceObject[]::new), level);
        for (WhereReferencedOutput output : response.output) {
            List<ModelObject> refObjs = new ArrayList<ModelObject>();
            for (WhereReferencedInfo info : output.info) {
                // int curLvl = info.level;
                String curRel = info.relation;
                WorkspaceObject curObj = info.referencer;

                if (relType != null && !relType.isEmpty() && !relType.equals(curRel)) {
                    continue;
                }

                if (objTypes.size() > 0) {
                    boolean matchType = false;
                    Type type = curObj.getTypeObject();
                    for (String objType : objTypes) {
                        if (isPreciseType) {
                            if (type.getName().equals(objType)) {
                                matchType = true;
                                break;
                            }
                        } else {
                            if (type.isInstanceOf(objType)) {
                                matchType = true;
                                break;
                            }
                        }

                    }
                    if (!matchType) {
                        continue;
                    }
                }

                refObjs.add(spy(curObj, tcContextHolder));
            }
            objMap.put(spy(output.inputObject, tcContextHolder), refObjs);
        }
        return objMap;
    }
}

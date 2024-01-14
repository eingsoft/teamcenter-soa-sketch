package com.eingsoft.emop.tc.service.impl;

import com.eingsoft.emop.tc.BMIDE;
import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcDataManagementService;
import com.eingsoft.emop.tc.util.ItemRevIdUtil;
import com.eingsoft.emop.tc.util.ProxyUtil;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.teamcenter.services.strong.core._2006_03.DataManagement.ObjectOwner;
import com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship;
import com.teamcenter.services.strong.core._2007_01.DataManagement.GetItemFromIdPref;
import com.teamcenter.services.strong.core._2008_06.DataManagement.*;
import com.teamcenter.services.strong.core._2009_10.DataManagement.GetItemFromAttributeInfo;
import com.teamcenter.services.strong.core._2009_10.DataManagement.GetItemFromAttributeItemRevOutput;
import com.teamcenter.services.strong.core._2009_10.DataManagement.GetItemFromAttributeResponse;
import com.teamcenter.services.strong.core._2013_05.DataManagement.*;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.*;
import com.teamcenter.soa.exceptions.NotLoadedException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static com.eingsoft.emop.tc.util.ProxyUtil.spy;

@Log4j2
@ScopeDesc(Scope.TcContextHolder)
public class TcDataManagementServiceImpl extends TcLoadServiceImpl implements TcDataManagementService {

    @Getter
    private TcContextHolder tcContextHolder;

    public TcDataManagementServiceImpl(TcContextHolder tcContextHolder) {
        super(tcContextHolder);
        this.tcContextHolder = tcContextHolder;
    }

    @Override
    public List<Item> findItems(String itemId) {
        List<Item> items = Lists.newArrayList();
        Map<String, ItemRevInfo> itemRevInfoMap = findItemRevInfoMapByItemId(itemId);
        for (ItemRevInfo itemRevInfo : itemRevInfoMap.values()) {
            items.add(itemRevInfo.getItem());
        }
        return items;
    }

    @Override
    public Item findItem(String itemId) {
        Map<String, ItemRevInfo> itemRevInfoMap = findItemRevInfoMapByItemId(itemId);
        ItemRevInfo info = itemRevInfoMap.get(itemId);
        return info != null ? info.item : null;
    }

    @Override
    public ItemRevision findLatestItemRevision(String itemId) {
        Map<String, ItemRevInfo> itemRevInfoMap = findItemRevInfoMapByItemId(itemId);
        ItemRevInfo info = itemRevInfoMap.get(itemId);
        return info != null ? info.latestItemRevOutputInfo.revision : null;
    }

    @Override
    public com.eingsoft.emop.tc.model.ModelObject findLatestApprovedItemRevision(String itemId) {
        return findLatestSpecificStatusItemRev(itemId, "Approved");
    }

    @Override
    public com.eingsoft.emop.tc.model.ModelObject findLatestAnyStatusItemRev(String itemId) {
        return findLatestSpecificStatusItemRev(itemId, null);
    }

    @Override
    public com.eingsoft.emop.tc.model.ModelObject findLatestSpecificStatusItemRev(String itemId, String statusName) {
        ItemRevision rev = findLatestItemRevision(itemId);
        if (Objects.isNull(rev)) {
            log.warn("Can not find item revision with param {}", itemId);
            return null;
        }
        // 检验当前对象的状态
        com.eingsoft.emop.tc.model.ModelObject obj = ProxyUtil.spy(rev, getTcContextHolder());
        if (isSpecificStatus(obj, statusName)) {
            return obj;
        }

        // 校验上一个版本对象的状态
        com.eingsoft.emop.tc.model.ModelObject previousRev = ProxyUtil.spy(findPreviousItemRevision(obj), getTcContextHolder());
        if (isSpecificStatus(previousRev, statusName)) {
            return previousRev;
        }
        log.warn("Can not find {} status revision obj with {} for the latest and previou version.", Strings.isNullOrEmpty(statusName) ? "any" : statusName, itemId);
        return null;
    }

    @Override
    public boolean isAnyStatus(ModelObject tcObj) {
        return isSpecificStatus(tcObj, null);
    }

    @Override
    public boolean isApprovedStatus(ModelObject tcObj) {
        return isSpecificStatus(tcObj, "Approved");
    }

    @Override
    public boolean isSpecificStatus(ModelObject tcObj, String statusName) {
        com.eingsoft.emop.tc.model.ModelObject obj = ProxyUtil.spy(tcObj, getTcContextHolder());
        com.eingsoft.emop.tc.model.ModelObject status = obj.getModelObject(BMIDE.PROP_LAST_RELEASE_STATUS);
        if (Objects.isNull(status)) {
            log.debug("{} no any status", obj.getDisplayVal(BMIDE.PROP_OBJECT_STRING));
            return false;
        }
        // 为空，则代表任何状态；
        if (Strings.isNullOrEmpty(statusName)) {
            return true;
        }
        // 如果多个状态， 则以英文逗号隔开
        List<String> multipStatusNames = Lists.newArrayList(statusName.split(BMIDE.COMMA));
        // 不为空，必须匹配状态
        if (status.getObjectName().equals(statusName) || multipStatusNames.contains(status.getObjectName())) {
            log.debug("find {} status for tc obj {} ", Strings.isNullOrEmpty(statusName) ? "any" : statusName,
                    obj.getDisplayVal(BMIDE.PROP_OBJECT_STRING));
            return true;
        }
        log.warn("{} last release status {} don't matched {}", obj.getDisplayVal(BMIDE.PROP_OBJECT_STRING), status.getObjectName(), statusName);
        return false;
    }

    @Override
    public ItemRevision findSpecificItemRevision(String itemId, String revId) {
        Map<String, ItemRevInfo> itemRevInfoMap = findItemRevInfoMapByItemId(itemId);
        ItemRevInfo info = itemRevInfoMap.get(itemId);
        return info != null ? info.revId2OutputInfos.get(revId).getRevision() : null;
    }

    @Override
    public ItemRevision findPreviousItemRevision(ModelObject itemRev) {
        com.eingsoft.emop.tc.model.ModelObject rev = spy(itemRev, tcContextHolder);
        String revId = rev.get(BMIDE.PROP_ITEM_REV_ID).toString();
        if (ItemRevIdUtil.isFirstRevId(revId)) {
            return (ItemRevision) rev;
        }
        String itemId = rev.get(BMIDE.PROP_ITEM_ID).toString();
        return findSpecificItemRevision(itemId, ItemRevIdUtil.getPreviousRevId(revId));
    }

    @Override
    public ItemRevision findNextItemRevision(ModelObject itemRev) {
        com.eingsoft.emop.tc.model.ModelObject rev = spy(itemRev, tcContextHolder);
        String revId = rev.get(BMIDE.PROP_ITEM_REV_ID).toString();
        String itemId = rev.get(BMIDE.PROP_ITEM_ID).toString();
        return findSpecificItemRevision(itemId, ItemRevIdUtil.getNextRevId(revId));
    }

    @Override
    public ItemRevInfo findItemRevInfoByUid(String uid) {
        Map<String, ItemRevInfo> resultMap = findItemRevInfoMapByUids(Lists.newArrayList(uid));
        return resultMap.get(uid);
    }

    @Override
    public Map<String, ItemRevInfo> findItemRevInfoMapByUids(List<String> uids) {
        List<com.eingsoft.emop.tc.model.ModelObject> tcObjs = getTcContextHolder().getTcLoadService().loadObjects(uids);
        Map<String, String> uid2ItemIdMap = tcObjs.stream().collect(Collectors.toMap(com.eingsoft.emop.tc.model.ModelObject::getUid, o -> o.getDisplayVal(BMIDE.PROP_ITEM_ID)));
        List<Map<String, String>> prop2valConditionMaps = Lists.newArrayList();
        for (String itemId : uid2ItemIdMap.values()) {
            Map<String, String> prop2valConditionMap = new HashMap<>();
            prop2valConditionMap.put(BMIDE.PROP_ITEM_ID, itemId);
            prop2valConditionMaps.add(prop2valConditionMap);
        }
        Map<String, ItemRevInfo> itemId2ItemRevInfoMap = findItemRevInfoMapByCondition(prop2valConditionMaps);
        Map<String, ItemRevInfo> resultMap = Maps.newHashMap();
        for (Entry<String, String> entry : uid2ItemIdMap.entrySet()) {
            ItemRevInfo itemRevInfo = itemId2ItemRevInfoMap.get(entry.getValue());
            if (itemRevInfo == null) {
                continue;
            }
            resultMap.put(entry.getKey(), itemRevInfo);
        }
        return resultMap;
    }

    @Override
    public ItemRevInfo findItemRevInfoByItemId(String itemId) {
        Map<String, ItemRevInfo> resultMap = findItemRevInfoMapByItemId(itemId);
        return resultMap.get(itemId);
    }

    @Override
    public Map<String, ItemRevInfo> findItemRevInfoMapByItemId(String itemId) {
        Map<String, String> prop2valConditionMap = new HashMap<>();
        prop2valConditionMap.put(BMIDE.PROP_ITEM_ID, itemId);
        return findItemRevInfoMapByCondition(prop2valConditionMap);
    }

    @Override
    public Map<String, ItemRevInfo> findItemRevInfoMapByItemIds(List<String> itemIds) {
        List<Map<String, String>> maps = Lists.newArrayList();
        for (String itemId : itemIds) {
            Map<String, String> prop2valConditionMap = new HashMap<>();
            prop2valConditionMap.put(BMIDE.PROP_ITEM_ID, itemId);
            maps.add(prop2valConditionMap);
        }
        return findItemRevInfoMapByCondition(maps);
    }

    @Override
    public Map<String, ItemRevInfo> findItemRevInfoMapByItemIdAndObjectType(String itemId, String itemType) {
        Map<String, String> prop2valConditionMap = new HashMap<>();
        prop2valConditionMap.put(BMIDE.PROP_ITEM_ID, itemId);
        if (!Strings.isNullOrEmpty(itemType)) {
            prop2valConditionMap.put(BMIDE.PROP_OBJECT_TYPE, itemType);
        }

        return findItemRevInfoMapByCondition(prop2valConditionMap);
    }

    @Override
    public Map<String, ItemRevInfo> findItemRevInfoMapByItemIdAndObjectTypes(String itemId, List<String> itemTypes) {
        Map<String, ItemRevInfo> map = new LinkedHashMap<>();
        if (itemTypes == null || itemTypes.isEmpty()) {
            return findItemRevInfoMapByItemId(itemId);
        }
        for (String itemType : itemTypes) {
            map.putAll(findItemRevInfoMapByItemIdAndObjectType(itemId, itemType));
        }
        return map;
    }

    @Override
    public Map<String, ItemRevInfo> findItemRevInfoMapByCondition(Map<String, String> prop2valConditionMap) {
        if (prop2valConditionMap.isEmpty()) {
            return Collections.emptyMap();
        }
        return findItemRevInfoMapByCondition(Lists.newArrayList(prop2valConditionMap));
    }

    @Override
    public Map<String, ItemRevInfo> findItemRevInfoMapByCondition(List<Map<String, String>> prop2valConditionMaps) {
        Map<String, ItemRevInfo> itemRevInfoMap = Maps.newHashMap();
        prop2valConditionMaps = prop2valConditionMaps.stream()
                .filter(m -> Objects.nonNull(m) && !Strings.isNullOrEmpty(m.get(BMIDE.PROP_ITEM_ID))).collect(Collectors.toList());
        if (prop2valConditionMaps.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            GetItemFromAttributeInfo[] input = new GetItemFromAttributeInfo[prop2valConditionMaps.size()];
            for (int i = 0; i < prop2valConditionMaps.size(); i++) {
                input[i] = new GetItemFromAttributeInfo();
                for (Entry<String, String> entry : prop2valConditionMaps.get(i).entrySet()) {
                    if (entry.getValue() != null) {
                        input[i].itemAttributes.put(entry.getKey(), entry.getValue().trim());
                    }
                }
            }

            GetItemFromIdPref pref = new GetItemFromIdPref();

            GetItemFromAttributeResponse resp = tcContextHolder.getDataManagementService().getItemFromAttribute(input, -1, pref);

            if (resp.serviceData.sizeOfPartialErrors() > 0) {
                log.error("it's exist errors so return empty map when find itemRevInfo by item id.");
                return Collections.emptyMap();
            }
            for (int i = 0; i < resp.output.length; i++) {
                Map<String, RevOutputInfo> revId2OutputInfoMap = new LinkedHashMap<>();
                Item item = (Item) spy(resp.output[i].item, tcContextHolder);
                GetItemFromAttributeItemRevOutput[] itemRevOutputs = resp.output[i].itemRevOutput;
                List<RevOutputInfo> revOutputInfos = Lists.newArrayList();
                for (GetItemFromAttributeItemRevOutput revOutput : itemRevOutputs) {
                    ItemRevision revision = (ItemRevision) spy(revOutput.itemRevision, tcContextHolder);
                    String revId = revision.get_item_revision_id();
                    String itemId = revision.get_item_id();
                    RevOutputInfo revOutputInfo = new RevOutputInfo(itemId, revId, revision, Arrays.asList(revOutput.datasets));
                    revId2OutputInfoMap.put(revId, revOutputInfo);
                    revOutputInfos.add(revOutputInfo);
                }
                RevOutputInfo latestRevOutputInfo = revOutputInfos.get(itemRevOutputs.length - 1);
                itemRevInfoMap.put(item.get_item_id(), new ItemRevInfo(item, latestRevOutputInfo, revId2OutputInfoMap));
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getLocalizedMessage(), e);
        }
        return itemRevInfoMap;
    }

    @Override
    public List<com.eingsoft.emop.tc.model.ModelObject> findChildren(ModelObject modelObject) {
        Map<com.eingsoft.emop.tc.model.ModelObject, List<com.eingsoft.emop.tc.model.ModelObject>> map =
                findChildren(Lists.newArrayList(modelObject));
        return map.get(modelObject) == null ? Collections.emptyList() : map.get(modelObject);
    }

    @Override
    public List<com.eingsoft.emop.tc.model.ModelObject> findChildren(ModelObject modelObject, String objectType) {
        List<com.eingsoft.emop.tc.model.ModelObject> children = findChildren(modelObject);
        return children.stream().filter(o -> o.getTypeObject().isInstanceOf(objectType) || o.getTypeObject().getName().equals(objectType))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<com.eingsoft.emop.tc.model.ModelObject, List<com.eingsoft.emop.tc.model.ModelObject>> findChildren(
            List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjects) {
        return findChildren(modelObjects, null);
    }

    @Override
    public Map<com.eingsoft.emop.tc.model.ModelObject, List<com.eingsoft.emop.tc.model.ModelObject>> findChildren(
            List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjects, String filterObjectTypes) {
        modelObjects = modelObjects.parallelStream().filter(Objects::nonNull).collect(Collectors.toList());
        if (modelObjects == null || modelObjects.isEmpty()) {
            return Collections.emptyMap();
        }

        // 如果过滤类型参数为 空，则类型为空，否则需要支持指定的对象类型， 转换后的类型为空list 则说明不过滤
        List<String> objectTypes =
                Strings.isNullOrEmpty(filterObjectTypes) ? Collections.emptyList() : Lists.newArrayList(filterObjectTypes.split(BMIDE.COMMA));

        List<GetChildrenInputData> inputs = Lists.newArrayList();
        for (ModelObject tcObj : modelObjects) {
            GetChildrenInputData input = new GetChildrenInputData();
            input.clientId = tcObj.getUid();
            input.obj = tcObj;
            inputs.add(input);
        }
        GetChildrenResponse response =
                tcContextHolder.getDataManagementService().getChildren(inputs.toArray(new GetChildrenInputData[inputs.size()]));

        Map<ModelObject, GetChildrenOutput[]> map = response.objectWithChildren;

        Map<com.eingsoft.emop.tc.model.ModelObject, List<com.eingsoft.emop.tc.model.ModelObject>> resultMap = new HashMap<>();
        for (Map.Entry<ModelObject, GetChildrenOutput[]> entry : map.entrySet()) {
            List<com.eingsoft.emop.tc.model.ModelObject> childrenObj = Lists.newArrayList();
            com.eingsoft.emop.tc.model.ModelObject parent = ProxyUtil.spy(entry.getKey(), tcContextHolder);
            resultMap.put(parent, childrenObj);
            GetChildrenOutput[] outputs = map.get(entry.getKey());
            for (GetChildrenOutput output : outputs) {
                // 伪文件，则忽略
                if ("pseudo_folder".equals(output.propertyName)) {
                    continue;
                }
                ModelObject[] children = output.children;
                if (children == null || children.length == 0) {
                    // log.debug("it's empty children with the parent {}",
                    // parent.getDisplayVal(BMIDE.PROP_OBJECT_STRING));
                    continue;
                }
                for (ModelObject child : output.children) {
                    com.eingsoft.emop.tc.model.ModelObject childObj = ProxyUtil.spy(child, tcContextHolder);
                    // log.debug("the child tc object {} with parent {} and property {}",
                    // childObj.getDisplayVal(BMIDE.PROP_OBJECT_STRING),
                    // parent.getDisplayVal(BMIDE.PROP_OBJECT_STRING), output.propertyName);

                    if (objectTypes.isEmpty()) {// 无需过滤
                        childrenObj.add(childObj);
                    } else {
                        // 有任何一个满足要求，则返回结果
                        if (objectTypes.stream().anyMatch(
                                filterType -> childObj.getTypeObject().isInstanceOf(filterType) || childObj.getTypeObject().getName().equals(filterType))) {
                            childrenObj.add(childObj);
                        }
                    }
                }
            }
        }
        return resultMap;
    }

    @Override
    public List<com.eingsoft.emop.tc.model.ModelObject> findChildrenWithRel(ModelObject modelObject, String relationName) {
        return findChildrenWithRel(modelObject, relationName, null);
    }

    @Override
    public List<com.eingsoft.emop.tc.model.ModelObject> findChildrenWithRel(ModelObject modelObject, String relationName, String objectType) {
        Map<String, List<com.eingsoft.emop.tc.model.ModelObject>> childrenWithRel = findChildrenWithRel(modelObject);
        List<com.eingsoft.emop.tc.model.ModelObject> children = childrenWithRel.get(relationName);
        if (children == null) {
            return Collections.emptyList();
        }
        if (Strings.isNullOrEmpty(objectType)) {
            return children;
        }
        return children.stream().filter(o -> o.getTypeObject().isInstanceOf(objectType) || o.getTypeObject().getName().equals(objectType))
                .collect(Collectors.toList());
    }

    @Override
    public Map<com.eingsoft.emop.tc.model.ModelObject, List<com.eingsoft.emop.tc.model.ModelObject>> findChildrenWithRel(
            List<ModelObject> modelObjs, String relationName, String filterObjectTypes) {

        if (modelObjs == null || modelObjs.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ParentRelationInfo> parentRelationInfos = findParentAndChildrenWithRel(modelObjs, relationName, filterObjectTypes);
        if (parentRelationInfos.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<com.eingsoft.emop.tc.model.ModelObject, List<com.eingsoft.emop.tc.model.ModelObject>> childrenWithRel = new HashMap<>();
        for (ParentRelationInfo parentRelInfo : parentRelationInfos) {
            List<RelationInfo> relationInfos = parentRelInfo.getRelations();
            RelationInfo relationInfo = relationInfos.stream().filter(o -> o.getRelationName().equals(relationName)).findFirst().orElse(null);
            if (relationInfo != null) {
                childrenWithRel.put(parentRelInfo.getParent(), relationInfo.getChildren());
            } else {
                childrenWithRel.put(parentRelInfo.getParent(), Collections.emptyList());
            }
        }
        return childrenWithRel;
    }

    /**
     * @param modelObjs
     * @param refRelation      可为空 root_reference_attachments, root_target_attachments
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<ParentRelationInfo> findParentAndChildrenWithRel(List<ModelObject> modelObjs, String refRelation, String filterObjectTypes) {
        if (modelObjs == null || modelObjs.isEmpty()) {
            return Collections.emptyList();
        }
        List<GetChildrenInputData> inputs = Lists.newArrayList();
        for (ModelObject modelObj : modelObjs) {
            GetChildrenInputData input = new GetChildrenInputData();
            input.clientId = modelObj.getUid();
            input.obj = modelObj;
            inputs.add(input);
        }
        GetChildrenResponse response =
                tcContextHolder.getDataManagementService().getChildren(inputs.toArray(new GetChildrenInputData[inputs.size()]));

        Map<ModelObject, GetChildrenOutput[]> map = response.objectWithChildren;

        List<ParentRelationInfo> parentRelationInfos = Lists.newArrayList();
        for (Map.Entry<ModelObject, GetChildrenOutput[]> entry : map.entrySet()) {
            GetChildrenOutput[] outputs = map.get(entry.getKey());
            ParentRelationInfo parentRelationInfo = new ParentRelationInfo();
            parentRelationInfo.setParent(spy(entry.getKey(), getTcContextHolder()));
            for (GetChildrenOutput output : outputs) {
                // 非关系类型的伪文件，则忽略
                if (!"pseudo_folder".equals(output.propertyName)) {
                    continue;
                }
                List<RelationInfo> relationInfos = Lists.newArrayList();
                for (ModelObject realtion : output.children) {
                    // UID格式 SR::N::PseudoFolder..14:T4W1WRGrJMlDLB26:root_reference_attachments
                    int rel = realtion.getUid().lastIndexOf(":");
                    String relationName = realtion.getUid().substring(rel + 1);
                    // 关系名相同 或者为空（不过滤）
                    if (Strings.isNullOrEmpty(refRelation) || refRelation.equals(relationName)) {
                        RelationInfo relationInfo = new RelationInfo();
                        relationInfo.setRelation(spy(realtion, getTcContextHolder()));
                        relationInfo.setRelationName(relationName);
                        relationInfos.add(relationInfo);
                    }
                }
                parentRelationInfo.setRelations(relationInfos);
            }
            parentRelationInfos.add(parentRelationInfo);
        }

        // 批量加载所有关系下的所有对象
        Map<com.eingsoft.emop.tc.model.ModelObject, RelationInfo> relaton2RelationInfoMap =
                Maps.uniqueIndex(parentRelationInfos.stream().flatMap(o -> o.getRelations().stream()).collect(Collectors.toList()),
                        new Function<RelationInfo, com.eingsoft.emop.tc.model.ModelObject>() {

                            @Override
                            public com.eingsoft.emop.tc.model.ModelObject apply(RelationInfo input) {
                                return input.getRelation();
                            }
                        });
        Map<com.eingsoft.emop.tc.model.ModelObject, List<com.eingsoft.emop.tc.model.ModelObject>> relation2ChildrenMap =
                findChildren(Lists.newArrayList(relaton2RelationInfoMap.keySet()), filterObjectTypes);
        // 将以关系对象找到的所有子对象列表，填充至目标list中
        for (Entry<com.eingsoft.emop.tc.model.ModelObject, List<com.eingsoft.emop.tc.model.ModelObject>> entry : relation2ChildrenMap
                .entrySet()) {
            com.eingsoft.emop.tc.model.ModelObject relation = entry.getKey();
            RelationInfo relationInfo = relaton2RelationInfoMap.get(relation);
            relationInfo.setChildren(entry.getValue());
        }

        return parentRelationInfos;
    }

    @Override
    public Map<String, List<com.eingsoft.emop.tc.model.ModelObject>> findChildrenWithRel(ModelObject modelObj) {
        if (modelObj == null) {
            return Collections.emptyMap();
        }
        List<ParentRelationInfo> parentRelationInfos = findParentAndChildrenWithRel(Lists.newArrayList(modelObj), null, null);
        if (parentRelationInfos.isEmpty()) {
            return Collections.emptyMap();
        }
        List<RelationInfo> relationInfos = parentRelationInfos.get(0).getRelations();
        Map<String, List<com.eingsoft.emop.tc.model.ModelObject>> resultMap = new HashMap<>();
        for (RelationInfo relationInfo : relationInfos) {
            resultMap.put(relationInfo.getRelationName(), relationInfo.getChildren());
        }
        return resultMap;
    }

    @Override
    public com.eingsoft.emop.tc.model.ModelObject findLatestActivePSParent(ModelObject itemRev) {
        Map<String, com.eingsoft.emop.tc.model.ModelObject> parentItemId2ModelObjMap = findLatestActivePSParentMap(itemRev);
        if (parentItemId2ModelObjMap.isEmpty()) {
            return null;
        }
        // 默认取第一个
        return Lists.newArrayList(parentItemId2ModelObjMap.values()).get(0);
    }

    @Override
    public List<com.eingsoft.emop.tc.model.ModelObject> findLatestActivePSParents(ModelObject itemRev) {
        Map<String, com.eingsoft.emop.tc.model.ModelObject> parentItemId2ModelObjMap = findLatestActivePSParentMap(itemRev);
        if (parentItemId2ModelObjMap.isEmpty()) {
            return Collections.emptyList();
        }
        return Lists.newArrayList(parentItemId2ModelObjMap.values());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, com.eingsoft.emop.tc.model.ModelObject> findLatestActivePSParentMap(ModelObject itemRev) {
        com.eingsoft.emop.tc.model.ModelObject rev = ProxyUtil.spy(itemRev, getTcContextHolder());
        Object psParent = rev.get(BMIDE.PROP_PS_PARENTS);
        if (psParent == null) {
            return Collections.emptyMap();
        }
        // parents 有可能 包含 AB不同的版本，有可能包含同一个版本下 不同的小版本
        List<com.eingsoft.emop.tc.model.ModelObject> parents = (List<com.eingsoft.emop.tc.model.ModelObject>) psParent;
        if (parents.isEmpty()) {
            return Collections.emptyMap();
        }
        // 过滤掉小版本
        parents = parents.stream().filter(o -> 0 != (Integer) o.get("active_seq")).collect(Collectors.toList());
        Map<String, com.eingsoft.emop.tc.model.ModelObject> parentItemId2ModelObjMap = new HashMap<>();
        // AB大版本中，只取最新版本
        for (com.eingsoft.emop.tc.model.ModelObject parent : parents) {
            String itemid = parent.getDisplayVal(BMIDE.PROP_ITEM_ID);
            if (parentItemId2ModelObjMap.containsKey(itemid)) {
                String revId = parent.getDisplayVal(BMIDE.PROP_ITEM_REV_ID);
                com.eingsoft.emop.tc.model.ModelObject oldModelObject = parentItemId2ModelObjMap.get(itemid);
                String oldRevId = oldModelObject.getDisplayVal(BMIDE.PROP_ITEM_REV_ID);
                if (ItemRevIdUtil.compareRevId(revId, oldRevId) > 0) {
                    parentItemId2ModelObjMap.put(itemid, parent);
                }
            } else {
                parentItemId2ModelObjMap.put(itemid, parent);
            }
        }
        if (parentItemId2ModelObjMap.keySet().size() > 1) {
            List<String> parentStr =
                    parentItemId2ModelObjMap.values().stream().map(o -> o.getDisplayVal(BMIDE.PROP_OBJECT_STRING)).collect(Collectors.toList());
            log.warn("the item  {} of parent {} don't unique, please attention.", rev.getDisplayVal(BMIDE.PROP_OBJECT_STRING),
                    parentStr.toString());
        }
        return parentItemId2ModelObjMap;
    }

    /**
     * @param itemRev
     * @param newRevId
     * @param clientId 很重要的参数， 返回response的map中，此参数作为key值
     * @return
     */
    private ReviseInfo buildReviseInfo(ItemRevision itemRev, String newRevId, String clientId) {
        ReviseInfo info = new ReviseInfo();
        info.baseItemRevision = itemRev;
        info.newRevId = newRevId;
        info.clientId = clientId;
        return info;
    }

    @Override
    public ItemRevision revise(ItemRevision itemRev, String newRevId) {
        if (itemRev == null || Strings.isNullOrEmpty(newRevId)) {
            log.warn("the base item revision or newRevId is null, skip revise action.");
            return null;
        }
        ReviseInfo info = buildReviseInfo(itemRev, newRevId, itemRev.getUid());
        ReviseResponse2 response = tcContextHolder.getDataManagementService().revise2(new ReviseInfo[]{info});

        @SuppressWarnings("unchecked")
        Map<String, ReviseOutput> respMap = response.reviseOutputMap;
        if (respMap == null || respMap.isEmpty()) {
            log.warn("failed to revise to {} with {}, so return null.", newRevId,
                    spy(itemRev, getTcContextHolder()).getDisplayVal(BMIDE.PROP_OBJECT_STRING));
            return null;
        }
        return (ItemRevision) spy(Lists.newArrayList(respMap.values()).get(0).newItemRev, tcContextHolder);
    }

    @Override
    public ItemRevision revise(ItemRevision itemRev) {
        com.eingsoft.emop.tc.model.ModelObject rev = spy(itemRev, getTcContextHolder());
        String revId = rev.getDisplayVal(BMIDE.PROP_ITEM_REV_ID);
        String newRevId = ItemRevIdUtil.getNextRevId(revId);
        return revise(itemRev, newRevId);
    }

    @Override
    public Map<ItemRevision, ItemRevision> revises(List<ItemRevision> itemRevs) {
        if (itemRevs == null || itemRevs.isEmpty()) {
            log.warn("the base item revisions are null, skip revise action.");
            return Collections.emptyMap();
        }
        Map<String, ItemRevision> uid2RevMap = itemRevs.stream().collect(Collectors.toMap(ItemRevision::getUid, o -> o));

        ReviseInfo[] reviseInfos = itemRevs.stream().map(o -> {
            com.eingsoft.emop.tc.model.ModelObject rev = spy(o, getTcContextHolder());
            String revId = rev.getDisplayVal(BMIDE.PROP_ITEM_REV_ID);
            String newRevId = ItemRevIdUtil.getNextRevId(revId);
            return buildReviseInfo(o, newRevId, o.getUid());
        }).toArray(ReviseInfo[]::new);

        ReviseResponse2 response = tcContextHolder.getDataManagementService().revise2(reviseInfos);
        Map<ItemRevision, ItemRevision> rev2NewRevMap = new HashMap<>();

        @SuppressWarnings("unchecked")
        Map<String, ReviseOutput> respMap = response.reviseOutputMap;
        for (String clientId : respMap.keySet()) {
            rev2NewRevMap.put(uid2RevMap.get(clientId), (ItemRevision) spy(respMap.get(clientId).newItemRev, tcContextHolder));
        }

        return rev2NewRevMap;
    }

    @Override
    public Map<String, ItemRevision> reviseWithUids(List<String> revUids) {
        Map<ItemRevision, ItemRevision> map = reviseWithRevs(revUids);
        Map<String, ItemRevision> uid2NewRevMap = new HashMap<>();
        for (Entry<ItemRevision, ItemRevision> entry : map.entrySet()) {
            uid2NewRevMap.put(entry.getKey().getUid(), entry.getValue());
        }
        return uid2NewRevMap;
    }

    @Override
    public Map<ItemRevision, ItemRevision> reviseWithRevs(List<String> revUids) {
        if (revUids == null || revUids.isEmpty()) {
            log.warn("the base rev UIDs are null, skip revise action.");
            return Collections.emptyMap();
        }
        List<ItemRevision> itemRevs = getTcContextHolder().getTcLoadService().loadObjects(revUids).parallelStream().filter(Objects::nonNull)
                .filter(o -> o instanceof ItemRevision).map(o -> (ItemRevision) o).collect(Collectors.toList());

        return revises(itemRevs);
    }

    @Override
    public ItemRevision saveAs(ItemRevision itemRev, String newItemId, String newName) {
        if (itemRev == null) {
            log.warn("the base item revision is null, so skip execute save as command.");
            return null;
        }
        SaveAsNewItemInfo info = new SaveAsNewItemInfo();
        info.baseItemRevision = itemRev;
        if (!Strings.isNullOrEmpty(newItemId)) {
            info.newItemId = newItemId;
        }
        if (!Strings.isNullOrEmpty(newName)) {
            info.name = newName;
        }
        SaveAsNewItemResponse2 response = tcContextHolder.getDataManagementService().saveAsNewItem2(new SaveAsNewItemInfo[]{info});
        ServiceData serviceData = response.serviceData;
        tcContextHolder.printAndLogMessageFromServiceData(serviceData);

        for (int i = 0; i < serviceData.sizeOfCreatedObjects(); i++) {
            ModelObject newRev = response.serviceData.getCreatedObject(i);
            if (newRev instanceof ItemRevision) {
                ItemRevision newItemRev = (ItemRevision) spy(newRev, tcContextHolder);
                try {
                    log.info("the new item revision info is {} when execute save as command.", newItemRev.get_object_string());
                } catch (NotLoadedException e) {
                    e.printStackTrace();
                }
                return newItemRev;
            }
        }
        return null;
    }

    @Override
    public void refreshObjects(List<? extends ModelObject> objs) {
        Set<String> refreshedObjectUids = SOAExecutionContext.current().getRefreshedObjectUids();
        List<ModelObject> toBeRefreshedObjects =
                objs.stream().filter(o -> !refreshedObjectUids.contains(o.getUid())).collect(Collectors.toList());
        if (toBeRefreshedObjects.size() > 0) {
            tcContextHolder.getDataManagementService().refreshObjects(objs.toArray(new ModelObject[toBeRefreshedObjects.size()]));
            refreshedObjectUids.addAll(toBeRefreshedObjects.stream().map(o -> o.getUid()).collect(Collectors.toList()));
        }
    }

    @Override
    public void deleteModelObjects(Collection<? extends ModelObject> objs) {
        if (!objs.isEmpty()) {
            List<? extends ModelObject> list = new ArrayList<ModelObject>(objs);
            ServiceData response = tcContextHolder.getDataManagementService().deleteObjects(list.toArray(new ModelObject[objs.size()]));
            tcContextHolder.printAndLogMessageFromServiceData(response);
        }
    }

    /**
     * delete the objects with deleting relation first
     *
     * @param relationType
     * @Param primaryObj
     */
    @Override
    public void deleteModelObjects(String relationType, ModelObject primaryObj, Collection<? extends ModelObject> secondaryObjs) {
        if (!secondaryObjs.isEmpty()) {
            List<Relationship> relationList = new ArrayList<Relationship>();
            for (ModelObject obj : secondaryObjs) {
                Relationship relation = new Relationship();
                relation.relationType = relationType;
                relation.primaryObject = primaryObj;
                relation.secondaryObject = obj;
                relationList.add(relation);
            }

            ServiceData response =
                    getTcContextHolder().getDataManagementService().deleteRelations(relationList.toArray(new Relationship[relationList.size()]));
            tcContextHolder.printAndLogMessageFromServiceData(response);

            response = tcContextHolder.getDataManagementService().deleteObjects(secondaryObjs.toArray(new ModelObject[secondaryObjs.size()]));
            tcContextHolder.printAndLogMessageFromServiceData(response);
        }
    }

    /**
     * delete the objects with deleting relation first
     */
    @Override
    public void deleteModelObjects(String relationType, List<? extends ModelObject> primaryObjs,
                                   Collection<? extends ModelObject> secondaryObjs) {
        if (primaryObjs.isEmpty() || primaryObjs.size() != secondaryObjs.size()) {
            log.warn("primaryObjs size (" + primaryObjs.size() + ") should be the same as secondaryObjs size (" + secondaryObjs.size()
                    + ") and both should be greater than 0.");
            return;
        }
        List<Relationship> relationList = new ArrayList<Relationship>();
        int i = 0;
        for (ModelObject obj : secondaryObjs) {
            Relationship relation = new Relationship();
            relation.relationType = relationType;
            relation.primaryObject = primaryObjs.get(i++);
            relation.secondaryObject = obj;
            relationList.add(relation);
        }

        ServiceData response =
                getTcContextHolder().getDataManagementService().deleteRelations(relationList.toArray(new Relationship[relationList.size()]));
        tcContextHolder.printAndLogMessageFromServiceData(response);

        response = tcContextHolder.getDataManagementService().deleteObjects(secondaryObjs.toArray(new ModelObject[secondaryObjs.size()]));
        tcContextHolder.printAndLogMessageFromServiceData(response);
    }

    @Override
    public void changeOwnership(List<ModelObject> modelObjects, ModelObject user, ModelObject group) {
        ObjectOwner[] ownerData = new ObjectOwner[modelObjects.size()];
        for (int i = 0; i < modelObjects.size(); i++) {
            ObjectOwner ownrObj = new ObjectOwner();
            ownrObj.object = modelObjects.get(i);
            ownrObj.owner = (User) user;
            ownrObj.group = (Group) group;
            ownerData[i] = ownrObj;
        }
        ServiceData returnData = tcContextHolder.getDataManagementService().changeOwnership(ownerData);
        tcContextHolder.printAndLogMessageFromServiceData(returnData);
    }

    @Override
    public void changeOwnership(List<ModelObject> modelObjects, ModelObject user) {
        ModelObject group;
        try {
            group = ((User) user).get_default_group();
            changeOwnership(modelObjects, user, group);
        } catch (NotLoadedException e) {
            log.error("load user dafault group failure - " + e.getMessage());
        }
    }

    @Override
    public String generateItemId(String itemType) {
        GenerateNextValuesIn in = new GenerateNextValuesIn();
        in.clientId = "AutoAssignRAC";
        in.operationType = 1;
        in.businessObjectName = itemType;
        Map<String, String> map = new HashMap<>();
        map.put(BMIDE.PROP_ITEM_ID, "");
        in.propertyNameWithSelectedPattern = map;

        GenerateNextValuesResponse resp = getTcContextHolder().getDataManagementService().generateNextValues(new GenerateNextValuesIn[]{in});
        GeneratedValuesOutput[] outs = resp.generatedValues;
        GeneratedValue val = (GeneratedValue) outs[0].generatedValues.get(BMIDE.PROP_ITEM_ID);
        return val.nextValue;
    }

    // -----------Deprecated start
    @Override
    @Deprecated
    public List<Item> getItemsByItemIds(List<String> itemIds, String revId) {
        List<Item> result = Lists.newArrayList();
        AttrInfo[] attrInfos = new AttrInfo[itemIds.size()];
        for (int i = 0; i < itemIds.size(); i++) {
            attrInfos[i] = new AttrInfo();
            attrInfos[i].name = "item_id";
            attrInfos[i].value = itemIds.get(i);

        }
        ItemInfo itemInfo = new ItemInfo();
        itemInfo.ids = attrInfos;
        itemInfo.clientId = "itemInfo1";
        itemInfo.useIdFirst = true;
        RevInfo revInfo = new RevInfo();
        revInfo.id = revId;
        revInfo.nRevs = 1;
        revInfo.clientId = "revInfo1";
        revInfo.useIdFirst = true;
        revInfo.processing = "Ids";
        DatasetInfo dsInfo = new DatasetInfo();
        dsInfo.clientId = "dsInfo1";
        dsInfo.filter = new DatasetFilter();
        dsInfo.filter.processing = "None";
        GetItemAndRelatedObjectsInfo[] itemAndRelatedObjectsInfo = new GetItemAndRelatedObjectsInfo[1];
        itemAndRelatedObjectsInfo[0] = new GetItemAndRelatedObjectsInfo();
        itemAndRelatedObjectsInfo[0].itemInfo = itemInfo;
        itemAndRelatedObjectsInfo[0].revInfo = revInfo;
        itemAndRelatedObjectsInfo[0].datasetInfo = dsInfo;
        itemAndRelatedObjectsInfo[0].clientId = "itemAndRelObj1";
        GetItemAndRelatedObjectsResponse response =
                tcContextHolder.getDataManagementService().getItemAndRelatedObjects(itemAndRelatedObjectsInfo);
        tcContextHolder.printAndLogMessageFromServiceData(response.serviceData);
        if (response.serviceData.sizeOfPartialErrors() <= 0) {
            for (GetItemAndRelatedObjectsItemOutput out : response.output) {
                if (out != null) {
                    result.add(out.item);
                }
            }
        }
        return result;
    }

    @Override
    @Deprecated
    public Item getItemByItemId(String itemId, String revId) {
        List<Item> items = this.getItemsByItemIds(Lists.newArrayList(itemId), revId);
        return items.isEmpty() ? null : items.get(0);
    }
    // ------------- Deprecated end

    @AllArgsConstructor
    public static class ItemRevInfo {
        @Getter
        private Item item;

        @Getter
        private RevOutputInfo latestItemRevOutputInfo;

        @Getter
        private Map<String, RevOutputInfo> revId2OutputInfos = Maps.newLinkedHashMap();
    }

    @AllArgsConstructor
    public static class RevOutputInfo {
        @Getter
        private String itemId;
        @Getter
        private String revId;
        @Getter
        private ItemRevision revision;
        @Getter
        private List<Dataset> datasets = Lists.newArrayList();
    }

    @Data
    public static class ParentRelationInfo {
        private com.eingsoft.emop.tc.model.ModelObject parent;

        private List<RelationInfo> relations = Collections.emptyList();
    }

    @Data
    public static class RelationInfo {
        private String relationName;

        private com.eingsoft.emop.tc.model.ModelObject relation;

        private List<com.eingsoft.emop.tc.model.ModelObject> children = Collections.emptyList();
    }

}

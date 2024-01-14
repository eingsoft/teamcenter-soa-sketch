package com.eingsoft.emop.tc.service.impl;

import com.eingsoft.emop.tc.BMIDE;
import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;
import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcSOAService;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.internal.strong.core.ICTService;
import com.teamcenter.services.internal.strong.core._2011_06.ICT;
import com.teamcenter.services.internal.strong.core._2011_06.ICT.InvokeICTMethodResponse;
import com.teamcenter.services.strong.core.SessionService;
import com.teamcenter.services.strong.core._2006_03.DataManagement.*;
import com.teamcenter.services.strong.core._2006_03.Session.GetSessionGroupMemberResponse;
import com.teamcenter.services.strong.core._2007_01.DataManagement.VecStruct;
import com.teamcenter.services.strong.core._2007_01.Session;
import com.teamcenter.services.strong.core._2007_12.Session.StateNameValue;
import com.teamcenter.services.strong.core._2008_06.DataManagement;
import com.teamcenter.services.strong.core._2008_06.DataManagement.CreateIn;
import com.teamcenter.services.strong.core._2008_06.DataManagement.CreateInput;
import com.teamcenter.services.strong.core._2008_06.DataManagement.CreateOut;
import com.teamcenter.services.strong.core._2008_06.DataManagement.CreateResponse;
import com.teamcenter.services.strong.core._2010_09.DataManagement.NameValueStruct1;
import com.teamcenter.services.strong.core._2010_09.DataManagement.PropInfo;
import com.teamcenter.services.strong.core._2010_09.DataManagement.SetPropertyResponse;
import com.teamcenter.services.strong.core._2015_07.DataManagement.CreateIn2;
import com.teamcenter.services.strong.core._2015_07.DataManagement.CreateInput2;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.*;
import com.teamcenter.soa.exceptions.NotLoadedException;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static com.eingsoft.emop.tc.BMIDE.*;
import static com.eingsoft.emop.tc.util.ProxyUtil.proxy;
import static com.eingsoft.emop.tc.util.ProxyUtil.spy;
import static java.util.Collections.emptyList;

/**
 * 获取各种SOA Service API, 创建、删除、修改、加载属性及对象的工具类。
 *
 * @author king
 */
@Log4j2
@ScopeDesc(Scope.TcContextHolder)
public class TcSOAServiceImpl implements TcSOAService {
    @Getter
    private TcContextHolder tcContextHolder;

    public TcSOAServiceImpl(TcContextHolder tcContextHolder) {
        this.tcContextHolder = tcContextHolder;
    }

    /**
     * 获得当前登入用户信息
     *
     * @return
     */
    @Override
    public User getUser() {
        SessionService sessionService = tcContextHolder.getSessionService();
        Session.GetTCSessionInfoResponse tcSessionInfoResponse;
        try {
            tcSessionInfoResponse = sessionService.getTCSessionInfo();
        } catch (ServiceException e) {
            e.printStackTrace();
            return null;
        }
        return proxy(tcSessionInfoResponse.user, tcContextHolder);
    }

    /**
     * 获取当前登入成员
     *
     * @return
     */
    @Override
    public GroupMember getGroupMember() {
        SessionService sessionService = tcContextHolder.getSessionService();
        GetSessionGroupMemberResponse tcSessionGroupMemberResponse;
        try {
            tcSessionGroupMemberResponse = sessionService.getSessionGroupMember();
        } catch (ServiceException e) {
            e.printStackTrace();
            return null;
        }
        return proxy(tcSessionGroupMemberResponse.groupMember, tcContextHolder);
    }

    /**
     * 当前用户的Home文件夹
     *
     * @return
     */
    @Override
    public Folder getHomeFolder() {
        Folder homeFolder = null;
        try {
            homeFolder = getUser().get_home_folder();
        } catch (NotLoadedException e) {
            e.printStackTrace();
        }
        return proxy(homeFolder, tcContextHolder);
    }

    /**
     * 当前用户的Home文件夹
     *
     * @return
     */
    @Override
    public Folder getNewstuffFolder() {
        Folder newstuffFolder = null;
        try {
            newstuffFolder = getUser().get_newstuff_folder();
        } catch (NotLoadedException e) {
            e.printStackTrace();
        }

        return proxy(newstuffFolder, tcContextHolder);
    }

    /**
     * 创建单个Item对象
     *
     * @param itemProperty
     * @param container
     * @param relationProp
     */
    @Override
    public Item createItem(ItemProperties itemProperty, com.teamcenter.soa.client.model.ModelObject container, String relationProp) {
        List<Item> items = createItems(Arrays.asList(itemProperty), container, relationProp);
        return items.size() != 0 ? items.get(0) : null;
    }

    /**
     * 创建多个Item对象
     *
     * @param itemProperties
     * @param container
     * @param relationProp
     */
    @Override
    public List<Item> createItems(List<ItemProperties> itemProperties, com.teamcenter.soa.client.model.ModelObject container,
                                  String relationProp) {
        CreateItemsResponse response = this.tcContextHolder.getDataManagementService()
                .createItems(itemProperties.toArray(new ItemProperties[itemProperties.size()]), container, relationProp);
        CreateItemsOutput[] ouputs = response.output;
        Item[] createdItems = new Item[ouputs.length];
        for (int i = 0; i < ouputs.length; i++) {
            createdItems[i] = ouputs[i].item;
            SOAExecutionContext.current().createdItem(ouputs[i]);
        }
        return proxy(Arrays.asList(createdItems), tcContextHolder);
    }

    /**
     * 创建一个Item对象，带revision属性、container、relationType
     */
    @Override
    public Item createItems(String itemType, String itemId, String itemName, String clientId, Map<String, Object> itemPropMap,
                            Map<String, Object> itemRevPropMap, com.teamcenter.soa.client.model.ModelObject container, String relationType) {
        CreateIn createIn = buildItemInput(itemType, itemId, itemName, clientId, itemPropMap, itemRevPropMap);
        List<CreateOut> output = batchCreateObjects(Arrays.asList(createIn), container, relationType);
        Item item = output.size() > 0 ? proxy((Item) output.get(0).objects[0], tcContextHolder) : null;
        if (item != null) {
            SOAExecutionContext.current().createdModelObject(item);
        }
        return item;
    }

    /**
     * 批量创建Items, 并以contents关系放到Newstuff 文件夹下; 如需要连带赋值属性创建，请调用batchCreateItems(CreateIn[] createIns)
     *
     * @param itemProperties
     * @return
     */
    @Override
    public List<CreateItemsOutput> batchCreateItemsAndPutToNewstuff(List<ItemProperties> itemProperties) {
        return batchCreateItems(itemProperties, getNewstuffFolder(), REL_CONTENTS);
    }

    /**
     * 批量创建Items; 如需要连带赋值属性创建，请调用batchCreateItems(CreateIn[] createIns)
     *
     * @param itemProperties
     * @param container
     * @param relationProp
     * @return
     */
    @Override
    public List<CreateItemsOutput> batchCreateItems(List<ItemProperties> itemProperties,
                                                    com.teamcenter.soa.client.model.ModelObject container, String relationProp) {
        CreateItemsResponse response = this.tcContextHolder.getDataManagementService()
                .createItems(itemProperties.toArray(new ItemProperties[itemProperties.size()]), container, relationProp);
        tcContextHolder.printAndLogMessageFromServiceData(response.serviceData);
        List<CreateItemsOutput> result = Arrays.asList(response.output);
        result.stream().forEach(o -> SOAExecutionContext.current().createdItem(o));
        return result;
    }

    /**
     * 根据指定的Item Type创建Item对象， 若itemId为空， 则自动Assign； 对象以contents关系绑定在Newstuff文件夹下。
     *
     * @param itemId
     * @param itemName
     * @param itemType
     */
    @Override
    public Item createItem(String itemId, String itemName, String itemType) {
        ItemProperties itemProperty = new ItemProperties();
        itemProperty.clientId = "";
        itemProperty.type = itemType;
        itemProperty.name = itemName;
        itemProperty.revId = "A";
        itemProperty.itemId = itemId;
        // 此属性只对Item属性有效， ItemRevision无效，TC API可能有BUG
        itemProperty.extendedAttributes = new ExtendedAttributes[]{new ExtendedAttributes()};
        List<Item> items = createItems(Arrays.asList(itemProperty), getNewstuffFolder(), REL_CONTENTS);
        return items.size() > 0 ? items.get(0) : null;
    }

    /**
     * 创建的文件夹放在Newstuff下
     *
     * @param folderName
     * @return
     */
    @Override
    public Folder createFolder(String folderName) {
        return createFolder(folderName, getNewstuffFolder(), REL_CONTENTS);
    }

    /**
     * 创建的文件夹以relationType关系放在container中
     *
     * @param folderName
     * @param container
     * @param relationType
     * @return
     */
    @Override
    public Folder createFolder(String folderName, com.teamcenter.soa.client.model.ModelObject container, String relationType) {
        if (Strings.isNullOrEmpty(folderName)) {
            return null;
        }
        CreateFolderInput folderInput = new CreateFolderInput();
        folderInput.name = folderName;
        CreateFoldersResponse response =
                tcContextHolder.getDataManagementService().createFolders(new CreateFolderInput[]{folderInput}, container, relationType);
        tcContextHolder.printAndLogMessageFromServiceData(response.serviceData);
        return proxy(response.output[0].folder, tcContextHolder);
    }

    /**
     * 更新普通属性的值，BMIDE中定义的属性类型为字符串
     *
     * @param modelObject
     * @param prop
     * @param value
     */
    @Override
    public void setProperties(com.teamcenter.soa.client.model.ModelObject modelObject, String prop, String value) {
        Map<String, Object> map = new HashMap<>();
        map.put(prop, value);
        setProperties(modelObject, map);
    }

    /**
     * 更新数组属性的值， BMIDE中定义的属性类型为字符串数组
     *
     * @param modelObject
     * @param prop
     * @param values
     */
    @Override
    public void setProperties(com.teamcenter.soa.client.model.ModelObject modelObject, String prop, List<String> values) {
        Map<String, Object> map = new HashMap<>();
        map.put(prop, values);
        setProperties(modelObject, map);
    }

    /**
     * 修改对象属性值， value对应值的类型为: String, String[]. 变更空字符串和空数组可以进行保存
     *
     * @param modelObject
     * @param map
     */
    @Override
    public void setProperties(com.teamcenter.soa.client.model.ModelObject modelObject, Map<String, Object> map) {
        Map<String, VecStruct> vecStructMap = new HashMap<>();
        for (String key : map.keySet()) {
            VecStruct vecStruct = new VecStruct();
            Object value = map.get(key);
            if (BMIDE.PROP_UOM_TAG.equals(key)) {
                value = getUnitOfMeasureUid(modelObject.getTypeObject().getName(), String.valueOf(value));
            }
            if (value instanceof String[]) {
                // String[] tempValue = (String[])value;
                // if (null != value) {
                vecStruct.stringVec = (String[]) value;
                vecStructMap.put(key, vecStruct);
                // }
            } else if (value instanceof String) {
                // String tempValue = (String)value;
                // if (null != value) {
                vecStruct.stringVec = new String[]{(String) value};
                vecStructMap.put(key, vecStruct);
                // }
            }
        }
        if (!vecStructMap.isEmpty()) {
            setProperties(Arrays.asList(modelObject), vecStructMap);
        }
    }

    /**
     * 设置可修改属性的值
     *
     * @param modelObjects
     * @param map
     */
    @Override
    public void setProperties(List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjects, Map<String, VecStruct> map) {
        if (modelObjects.isEmpty()) {
            return;
        }
        ServiceData serviceData = this.tcContextHolder.getDataManagementService()
                .setProperties(modelObjects.toArray(new com.teamcenter.soa.client.model.ModelObject[modelObjects.size()]), map);
        tcContextHolder.printAndLogMessageFromServiceData(serviceData, true);
    }

    @Override
    public void setRefProperty(com.teamcenter.soa.client.model.ModelObject primary, String secondaryUid, String property) {
        PropInfo[] propInfos = new PropInfo[1];
        propInfos[0] = new PropInfo();
        propInfos[0].object = primary;
        propInfos[0].vecNameVal = new NameValueStruct1[1];
        propInfos[0].vecNameVal[0] = new NameValueStruct1();
        propInfos[0].vecNameVal[0].name = property;
        propInfos[0].vecNameVal[0].values = new String[]{secondaryUid};
        // Not allowed for NULL
        String[] options = new String[]{""};
        tcContextHolder.getDataManagementService().setProperties(propInfos, options);
    }

    /**
     * 构造对象多个属性的值的信息， 用于批量设置多个属性
     *
     * @param object        business object
     * @param nameValuesMap prop-values map
     * @return
     */
    @Override
    public PropInfo buildPropInfo(com.teamcenter.soa.client.model.ModelObject object, Map<String, List<String>> nameValuesMap) {
        if (object == null || nameValuesMap == null || nameValuesMap.isEmpty()) {
            return null;
        }
        String objectType = object.getTypeObject().getName();
        PropInfo info = new PropInfo();
        info.object = object;
        info.timestamp = new GregorianCalendar();
        List<NameValueStruct1> nameValueStruct1s = Lists.newArrayList();
        for (String prop : nameValuesMap.keySet()) {
            NameValueStruct1 nameValueStruct1 = new NameValueStruct1();
            nameValueStruct1.name = prop;
            if (BMIDE.PROP_UOM_TAG.equals(prop)) {
                //  单位更新仅支持单个值
                String unitKey = nameValuesMap.get(prop).isEmpty() ? "" : nameValuesMap.get(prop).get(0);
                String unitUid = "";
                if (StringUtils.isEmpty(unitKey)) {
                    log.warn("the input uom_tag prop value is empty, it should be set default value({} or {}) for unit measure instance ", "PCS", "每个");
                } else {
                    unitUid = getUnitOfMeasureUid(objectType, unitKey);
                }
                nameValueStruct1.values = new String[]{unitUid};
            } else {
                nameValueStruct1.values = nameValuesMap.get(prop).stream().toArray(String[]::new);
            }
            nameValueStruct1s.add(nameValueStruct1);
        }
        info.vecNameVal = nameValueStruct1s.toArray(new NameValueStruct1[nameValueStruct1s.size()]);
        return info;
    }

    private String getUnitOfMeasureUid(String objectType, String unitKey) {
        ModelObject unitOfMeasure = tcContextHolder.getTcLOVService().findUnitOfMeasureWithSymbol(objectType, BMIDE.PROP_UOM_TAG, unitKey);
        return unitOfMeasure.getUid();
    }

    /**
     * 支持同时更新多个对象，多个属性值的API
     *
     * @param propInfos
     */
    @Override
    public void setProperties(PropInfo[] propInfos) {
        if (propInfos == null || propInfos.length == 0) {
            log.warn("the set properties method don't handle null or empty list");
            return;
        }
        setProperties(Arrays.asList(propInfos), emptyList());
    }

    /**
     * 支持同时更新多个对象，多个属性值的API
     *
     * @param propInfos
     */
    @Override
    public void setProperties(List<PropInfo> propInfos) {
        if (propInfos == null || propInfos.isEmpty()) {
            log.warn("the set properties method don't handle null or empty list");
            return;
        }
        setProperties(propInfos, emptyList());
    }

    /**
     * 支持同时更新多个对象，多个属性值的API
     *
     * @param propInfo
     * @param options  可为空
     */
    @Override
    public void setProperties(List<PropInfo> propInfo, List<String> options) {
        if (propInfo.isEmpty()) {
            return;
        }
        SetPropertyResponse response = this.tcContextHolder.getDataManagementService()
                .setProperties(propInfo.toArray(new PropInfo[propInfo.size()]), options.toArray(new String[options.size()]));
        tcContextHolder.printAndLogMessageFromServiceData(response.data, true);
    }

    @Override
    public void addRowToTable(com.teamcenter.soa.client.model.ModelObject targetObj, String tableType, String tableProp,
                              Map<String, String[]> columnName2ValueMap) {
        addRowsToTable(targetObj, tableType, tableProp, Lists.newArrayList(columnName2ValueMap));
    }

    @Override
    public void addRowsToTable(com.teamcenter.soa.client.model.ModelObject targetObj, String tableType, String tableProp,
                               List<Map<String, String[]>> columnName2ValueMaps) {
        List<CreateIn2> createIn2s = Lists.newArrayList();
        for (Map<String, String[]> columnName2ValueMap : columnName2ValueMaps) {
            CreateInput2 createData = new CreateInput2();
            createData.boName = tableType;
            createData.propertyNameValues = columnName2ValueMap;

            CreateIn2 input = new CreateIn2();
            input.clientId = "RAC_Client";
            input.pasteProp = tableProp;
            input.targetObject = targetObj;
            input.createData = createData;
            input.dataToBeRelated = new HashMap<>();
            input.workflowData = new HashMap<>();
            createIn2s.add(input);
        }
    }

    /**
     * 批量创建对象，以relationType保存至container下; 如果Container or relation type任意为空，创建的对象不以任何关系绑定。 <br>
     *
     * @param createIns
     * @return
     */
    @Override
    public List<CreateOut> batchCreateObjects(List<CreateIn> createIns, com.teamcenter.soa.client.model.ModelObject container,
                                              String relationType) {
        CreateResponse createresponse = null;
        try {
            createresponse = tcContextHolder.getDataManagementService().createObjects(createIns.toArray(new CreateIn[createIns.size()]));
        } catch (ServiceException e) {
            log.error(e.getLocalizedMessage(), e);
        }
        ServiceData serviceData = createresponse.serviceData;
        if (serviceData.sizeOfPartialErrors() > 0) {
            tcContextHolder.printAndLogMessageFromServiceData(serviceData);
        } else {
            // 创建的对象以relationType 保存至 container中, 必需是Workspace Object在子类
            if (container != null && !Strings.isNullOrEmpty(relationType)) {
                addCreatedObjectsToContainer(createresponse.output, container, relationType);
            }
        }

        return Arrays.asList(createresponse.output);
    }

    /**
     * 批量创建Items, 并以contents关系放到Newstuff 文件夹下; 如需要连带赋值属性创建，请调用batchCreateItems(CreateIn[] createIns)
     *
     * @param itemProperties
     * @return
     */
    @Override
    public List<CreateItemsOutput> batchCreateItems(List<ItemProperties> itemProperties) {
        return batchCreateItems(itemProperties, getNewstuffFolder(), REL_CONTENTS);
    }

    /**
     * 创建必需是Workspace Object对象， 以某种关系保存至container下
     *
     * @param outputs
     * @param container
     * @param relationType
     */
    private void addCreatedObjectsToContainer(CreateOut[] outputs, com.teamcenter.soa.client.model.ModelObject container,
                                              String relationType) {
        if (container == null || relationType == null || relationType.trim().isEmpty()) {
            log.error("the container and relation type must not be null or empty when build relations");
            return;
        }
        List<Relationship> relationships = Lists.newArrayList();
        for (CreateOut output : outputs) {
            Relationship relationship = new Relationship();
            for (com.teamcenter.soa.client.model.ModelObject obj : output.objects) {
                // ItemRevison and Item_Master don't need build relation when
                // associate with item.
                if (obj instanceof WorkspaceObject && !(obj instanceof ItemRevision || obj instanceof Item_Master)) {
                    relationship.primaryObject = container;
                    relationship.secondaryObject = obj;
                    relationship.relationType = relationType;
                    break;
                }
            }
            relationships.add(relationship);
        }
        if (!relationships.isEmpty()) {
            tcContextHolder.getTcRelationshipService().batchBuildRelation(relationships);
        }
    }

    /**
     * 创建可对itemRev属性赋值的Item， 并保存至Newstuff文件夹下，返回值为Item, ItemRevision, Master对象数组
     *
     * @param itemType
     * @param itemId
     * @param itemName
     * @param itemRevPropMap
     * @return
     */
    @Override
    public List<ModelObject> createItem(String itemType, String itemId, String itemName, Map<String, Object> itemRevPropMap) {
        CreateIn createItemIn = buildItemInput(itemType, itemId, itemName, null, null, itemRevPropMap);
        List<CreateOut> output = batchCreateObjects(Arrays.asList(createItemIn), getNewstuffFolder(), REL_CONTENTS);
        return output.size() > 0 ? spy(Arrays.asList(output.get(0).objects), tcContextHolder) : emptyList();
    }

    @Override
    public CreateIn buildItemInput(String itemType, String itemId, String itemName, String clientId, Map<String, Object> itemPropMap,
                                   Map<String, Object> itemRevPropMap) {
        return buildItemInput(itemType, itemType + "Revision", itemId, itemName, clientId, itemPropMap, itemRevPropMap);
    }

    /**
     * 建立创建Item所需要的输入信息， itemPropMap用于保存非item_id, object_name属性, itemRevPropMap可选
     *
     * @param itemType       为空则默认使用Item类型
     * @param itemId         为空，则自动AssignID
     * @param itemName       不建议为空
     * @param clientId       批量创建Item时有用， 用以区分返回的结果, 可选
     * @param itemPropMap    item的属性与属性值对（为null则默认带出rev，不允许有必填属性使用null）
     * @param itemRevPropMap 不应包含item_id等非Revision属性和由item创建时附加赋值的属性
     *                       itemRev的属性与属性值对（为null则默认带出rev，不允许有必填属性使用null）
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public CreateIn buildItemInput(String itemType, String revisionType, String itemId, String itemName, String clientId,
                                   Map<String, Object> itemPropMap, Map<String, Object> itemRevPropMap) {
        CreateInput itemInput = new CreateInput();
        if (!Strings.isNullOrEmpty(itemId)) {
            itemInput.stringProps.put(PROP_ITEM_ID, itemId);
        }
        if (!Strings.isNullOrEmpty(itemName)) {
            itemInput.stringProps.put(PROP_OBJECT_NAME, itemName);
        }

        if (itemPropMap != null && !itemPropMap.isEmpty()) {
            itemPropMap.forEach((k, v) -> {
                // uom_tag类型属性是特殊类型
                if (BMIDE.PROP_UOM_TAG.equals(k) && v != null && StringUtils.isNotBlank(String.valueOf(v))) {
                    ModelObject unitOfMeasure = tcContextHolder.getTcLOVService().findUnitOfMeasureWithSymbol(itemType, k, (String) v);
                    itemInput.tagProps.put(k, unitOfMeasure);
                } else {
                    fillItemInput(itemInput, k, v);
                }
            });
        }

        // 存在版本的属性则对其进行赋值
        if (itemRevPropMap != null && !itemRevPropMap.isEmpty()) {
            CreateInput itemRevInput = new CreateInput();
            itemRevInput.boName = revisionType;
            itemRevPropMap.forEach((k, v) -> {
                // itemRevProp 在此处不应包含 item_id, items_tag属性， 否则有可能报错,所有特殊处理
                if (PROP_ITEM_ID.equals(k) || PROP_ITEMS_TAG.equals(k)) {
                    return;
                }
                fillItemInput(itemRevInput, k, v);
            });
            itemInput.compoundCreateInput.put("revision", new CreateInput[]{itemRevInput});
        }

        CreateIn createItemIn = new CreateIn();
        createItemIn.data = itemInput;
        if (Strings.isNullOrEmpty(itemType)) {
            itemInput.boName = "Item";
        } else {
            itemInput.boName = itemType;
        }

        if (Strings.isNullOrEmpty(clientId)) {
            createItemIn.clientId = "";
        } else {
            createItemIn.clientId = clientId;
        }
        return createItemIn;
    }

    private void fillItemInput(DataManagement.CreateInput itemInput, String k, Object v) {
        switch (v.getClass().getTypeName()) {
            case "java.lang.String":
                itemInput.stringProps.put(k, (String) v);
                break;
            case "java.lang.String[]":
                itemInput.stringArrayProps.put(k, (String[]) v);
                break;
            case "java.lang.Boolean":
                itemInput.boolProps.put(k, (Boolean) v);
                break;
            case "java.lang.Boolean[]":
                itemInput.boolArrayProps.put(k, (boolean[]) v);
                break;
            case "java.lang.Integer":
                itemInput.intProps.put(k, (BigInteger) v);
                break;
            case "int[]":
                itemInput.intArrayProps.put(k, (int[]) v);
                break;
            case "java.lang.Float":
                itemInput.floatProps.put(k, (Float) v);
                break;
            case "float[]":
                itemInput.floatArrayProps.put(k, (float[]) v);
                break;
            case "java.lang.Double":
                itemInput.doubleProps.put(k, (Double) v);
                break;
            case "double[]":
                itemInput.doubleArrayProps.put(k, (double[]) v);
                break;
            case "java.util.Date":
                itemInput.dateProps.put(k, (Calendar) v);
                break;
            case "java.util.Date[]":
                itemInput.dateArrayProps.put(k, (Calendar[]) v);
        }
    }

    /**
     * 创建Model Object对象并以relationType保存至Container中
     *
     * @param objectType   为空则默认创建Item对象
     * @param clientId
     * @param propMap
     * @param container
     * @param relationType
     * @return
     */
    @Override
    public List<ModelObject> createObject(String objectType, String clientId, Map<String, String> propMap,
                                          com.teamcenter.soa.client.model.ModelObject container, String relationType) {
        CreateIn createIn = buildCreateIn(objectType, clientId, propMap);
        List<CreateOut> output = batchCreateObjects(Arrays.asList(createIn), container, relationType);
        return output.size() > 0 ? spy(Arrays.asList(output.get(0).objects), tcContextHolder) : emptyList();
    }

    /**
     * 根据对象类型构建创建对象的输入条件， 必填属性不能为null or empty, <br>
     * 此方法仅限于创建 item 上属性的对象，并不包含rev属性的值，如有需要，请使用buildItemInput()方法
     *
     * @param objectType
     * @param clientId
     * @param propMap    属性名， 创建的对象不允许为空的属性必填在此处
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public CreateIn buildCreateIn(String objectType, String clientId, Map<String, String> propMap) {
        CreateInput input = new CreateInput();

        if (propMap != null && !propMap.isEmpty()) {
            for (String prop : propMap.keySet()) {
                // uom_tag类型属性是特殊类型
                if (BMIDE.PROP_UOM_TAG.equals(prop)) {
                    ModelObject unitOfMeasure = tcContextHolder.getTcLOVService().findUnitOfMeasureWithSymbol(objectType, prop, propMap.get(prop));
                    input.tagProps.put(prop, unitOfMeasure);
                } else {
                    input.stringProps.put(prop, propMap.get(prop));
                }
            }
        }

        CreateIn createIn = new CreateIn();
        createIn.data = input;
        if (Strings.isNullOrEmpty(objectType)) {
            input.boName = TYPE_ITEM;
        } else {
            input.boName = objectType;
        }

        if (Strings.isNullOrEmpty(clientId)) {
            createIn.clientId = "";
        } else {
            createIn.clientId = clientId;
        }
        return createIn;
    }

    /**
     * 查找指定ItemId的对象
     *
     * @param modelObjects
     * @param itemId
     * @return
     */
    @Override
    public ModelObject filterObjsByItemId(List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjects, String itemId) {
        for (com.teamcenter.soa.client.model.ModelObject modelObject : modelObjects) {
            if (spy(modelObject, tcContextHolder).get(PROP_ITEM_ID).equals(itemId)) {
                return spy(modelObject, tcContextHolder);
            }
        }
        return null;
    }

    /**
     * 查找引用某个对象的对象们
     *
     * @param obj
     * @param relType
     * @param objType 对象类型，可以用英文逗号分割
     * @return
     */
    @SuppressWarnings("deprecation")
    @Override
    public List<ModelObject> findWhereReferenced(com.teamcenter.soa.client.model.ModelObject obj, String relType, String objType, int level) {
        List<String> objTypeArr = null;
        if (objType != null) {
            objTypeArr = Arrays.stream(objType.split(",")).map(o -> o.trim()).filter(o -> !o.isEmpty()).collect(Collectors.toList());
        }
        return tcContextHolder.getTcRelationshipService().findWhereReferenced(Arrays.asList(obj), relType, objTypeArr, level).getOrDefault(obj,
                emptyList());
    }

    /**
     * 刷新对象
     */
    @Override
    public void refreshObj(List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjs) {
        tcContextHolder.getDataManagementService()
                .refreshObjects(modelObjs.toArray(new com.teamcenter.soa.client.model.ModelObject[modelObjs.size()]));
    }

    /**
     * 刷新对象
     */
    @Override
    public void refreshObjs(com.teamcenter.soa.client.model.ModelObject modelObj) {
        tcContextHolder.getDataManagementService().refreshObjects(new com.teamcenter.soa.client.model.ModelObject[]{modelObj});
    }

    /**
     * 开启旁路权限
     */
    @Override
    public void openByPass() {
        tcContextHolder.getSessionService().setUserSessionState(setBypass(true));
        log.info("Open byPass");
    }

    /**
     * 是否已开启旁路权限 仅DBA权限才会允许开启旁路权限
     */
    @Override
    public boolean hasByPass() {
        boolean hasBypass = false;
        try {
            hasBypass = tcContextHolder.getSessionService().getTCSessionInfo().bypass;
            log.info("Has byPass " + hasBypass);
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        return hasBypass;
    }

    /**
     * 关闭旁路权限
     */
    @Override
    public void closeByPass() {
        tcContextHolder.getSessionService().setUserSessionState(setBypass(false));
        log.info("Close byPass");
    }

    /**
     * 开启旁路权限
     *
     * @param flag
     * @return
     */
    private StateNameValue[] setBypass(boolean flag) {
        StateNameValue[] properties = new StateNameValue[1];
        properties[0] = new StateNameValue();
        properties[0].name = "bypassFlag";
        properties[0].value = Boolean.toString(flag);
        return properties;
    }

    /**
     * 执行ICT函数，主要用以执行没有暴露SOA Java接口的调用
     *
     * @param className
     * @param methodName
     * @param argList
     * @return
     */
    @Override
    public InvokeICTMethodResponse invokeICTMethod(String className, String methodName, List<String> argList) {
        ICTService ictService = getTcContextHolder().getICTService();
        ICT.Arg[] args = argList.stream().map(o -> {
            ICT.Arg arg = new ICT.Arg();
            arg.val = o;
            return arg;
        }).toArray(ICT.Arg[]::new);
        try {
            InvokeICTMethodResponse response = ictService.invokeICTMethod(className, methodName, args);
            getTcContextHolder().printAndLogMessageFromServiceData(response.serviceData);
            return response;
        } catch (ServiceException e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public InvokeICTMethodResponse invokeICTMethod(String className, String methodName, ICT.Arg[] args) {
        ICTService ictService = getTcContextHolder().getICTService();
        try {
            InvokeICTMethodResponse response = ictService.invokeICTMethod(className, methodName, args);
            getTcContextHolder().printAndLogMessageFromServiceData(response.serviceData);
            return response;
        } catch (ServiceException e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ModelObject> getUserGroups(User user) {
        return tcContextHolder.getTcOrgnizationService().getUserGroups(user);
    }

    @Override
    public User findUserByUserId(String userId) {
        return tcContextHolder.getTcOrgnizationService().findUserByUserId(userId);
    }
}

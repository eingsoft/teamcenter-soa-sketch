package com.eingsoft.emop.tc.service.impl;

import com.eingsoft.emop.tc.BMIDE;
import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;
import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.service.TcClassificationService;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.util.ProxyUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.internal.strong.core._2011_06.ICT;
import com.teamcenter.services.internal.strong.core._2011_06.ICT.InvokeICTMethodResponse;
import com.teamcenter.services.strong.classification._2007_01.Classification;
import com.teamcenter.services.strong.classification._2007_01.Classification.*;
import com.teamcenter.services.strong.classification._2011_12.Classification.*;
import com.teamcenter.soa.client.model.strong.WorkspaceObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.math.BigInteger;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static com.eingsoft.emop.tc.util.ProxyUtil.proxy;
import static com.eingsoft.emop.tc.util.ProxyUtil.spy;

@Log4j2
@ScopeDesc(Scope.TcContextHolder)
public class TcClassificationServiceImpl implements TcClassificationService {
    @Getter
    private TcContextHolder tcContextHolder;

    public TcClassificationServiceImpl(TcContextHolder tcContextHolder) {
        this.tcContextHolder = tcContextHolder;
    }

    @Override
    public Map<String, List<ClassificationProperty>> getClassificationPropsByRevUids(String... revUids) {
        Map<ModelObject, List<ModelObject>> classificationObjs = tcContextHolder.getTcRelationshipService().findAllRelatedModelObjsByUidsAndRelName(Arrays.asList(revUids), BMIDE.REL_CLASSIFICATION);
        Map<String, List<ClassificationProperty>> result = new HashMap<String, List<ClassificationProperty>>();
        batchLoadClassifications(classificationObjs).forEach((k, v) -> result.put(k.getUid(), v));
        return result;
    }

    private Map<ModelObject, List<ClassificationProperty>> batchLoadClassifications(Map<ModelObject, List<ModelObject>> classificationObjs) {
        Map<ModelObject, List<ClassificationProperty>> result = new HashMap<ModelObject, List<ClassificationProperty>>();
        Set<String> allIcoUids = new HashSet<String>();
        // get all ico uids first, for batch load purpose
        for (List<ModelObject> rels : classificationObjs.values()) {
            allIcoUids.addAll(rels.stream().map(rel -> rel.getUid()).collect(Collectors.toSet()));
        }
        // batch loaded classification properties
        Map<String, List<ClassificationProperty>> allClassificationProperties = getClassificationPropsByIcoUids(allIcoUids.toArray(new String[allIcoUids.size()]));
        // populate result from batch loaded classification properties
        for (Entry<ModelObject, List<ModelObject>> entry : classificationObjs.entrySet()) {
            List<ClassificationProperty> props = new ArrayList<ClassificationProperty>(entry.getValue().size());
            for (ModelObject rel : entry.getValue()) {
                List<ClassificationProperty> theClassificationProperties = allClassificationProperties.get(rel.getUid());
                if (theClassificationProperties != null) {
                    props.addAll(theClassificationProperties);
                }
            }
            result.put(proxy(entry.getKey(), tcContextHolder), props);
        }
        return result;
    }

    /**
     * 此方法比getClassificationPropsByRevUids根据UID批量加载性能要高， 无需先根据UID加载ModelObject
     *
     * @param objs
     * @return
     */
    @Override
    public Map<ModelObject, List<ClassificationProperty>> getClassificationPropsByRevisions(com.teamcenter.soa.client.model.ModelObject... objs) {
        Map<ModelObject, List<ModelObject>> classificationObjs = tcContextHolder.getTcRelationshipService().findAllRelatedModelObjsByObjsAndRelName(Arrays.asList(objs), BMIDE.REL_CLASSIFICATION);
        return batchLoadClassifications(classificationObjs);
    }

    /**
     * 根据分类UID获取分类属性
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, List<ClassificationProperty>> getClassificationPropsByIcoUids(String... icoUids) {
        Map<String, List<ClassificationProperty>> result = new HashMap<String, List<ClassificationProperty>>();
        try {
            ClassificationInfoResponse response = tcContextHolder.getClassificationService().getClassificationObjectInfo(icoUids, new int[0], true, true, "zh_CN");
            tcContextHolder.printAndLogMessageFromServiceData(response.svcData);
            Map<String, ClassAttrInfo> objInfos = response.classificationObjectInfo;

            for (Entry<String, ClassAttrInfo> objInfoEntry : objInfos.entrySet()) {
                List<ClassificationProperty> classificationProps = new ArrayList<ClassificationProperty>();
                Map<BigInteger, AttributeValues> attrValues = objInfoEntry.getValue().attrValuesMap;
                for (Entry<BigInteger, AttributeValues> attrValEntry : attrValues.entrySet()) {
                    BigInteger attrId = attrValEntry.getKey();
                    Value[] vals = attrValEntry.getValue().values;
                    if (vals != null) {
                        Arrays.stream(vals).forEach(val -> classificationProps.add(new ClassificationProperty(attrId, getPropertyDisplayName(objInfoEntry.getValue().cid, attrId, response.classAttributeDesc), val.attrValue, getUnit(val.unit))));

                    }
                }
                result.put(objInfoEntry.getKey(), classificationProps);
            }
        } catch (Exception e) {
            throw new RuntimeException("cannot get classification detail " + Arrays.toString(icoUids), e);
        }
        return result;
    }

    /**
     * 获取分类树单层子节点集
     *
     * @param cids
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, List<ClassificationNode>> getChildren(String[] cids) {
        Map<String, List<ClassificationNode>> resultMap = new HashMap<String, List<ClassificationNode>>();
        if (null == cids || cids.length < 1) {
            cids = new String[]{"ICM"};
        }
        Map<String, ChildDef[]> childrenMap;
        ChildDef[] children;
        try {
            GetChildrenResponse response = tcContextHolder.getClassificationService().getChildren(cids);
            childrenMap = response.children;
            if (null == childrenMap) {
                return resultMap;
            }
            for (String cid : childrenMap.keySet()) {
                children = childrenMap.get(cid);
                List<ClassificationNode> childrenList = new ArrayList<>();
                for (ChildDef childDef : children) {
                    childrenList.add(new ClassificationNode(childDef.id, childDef.name + (BMIDE.STORAGE_CLASS.equals(childDef.type) ? " [" + childDef.instanceCount + "]" : " [" + childDef.childCount + "]"), BMIDE.STORAGE_CLASS.equals(childDef.type)));
                }
                resultMap.put(cid, childrenList);
            }
        } catch (ServiceException e) {
            log.error("cannot obtain classification nodes ", e);
            e.printStackTrace();
        }
        return resultMap;
    }

    /**
     * 获取分类属性
     *
     * @param cids
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, ClassAttribute[]> getClassificationAttributes(String... cids) {
        Map<String, ClassAttribute[]> resultMap = new HashMap<>();
        try {
            GetAttributesForClassesResponse response = tcContextHolder.getClassificationService().getAttributesForClasses(cids);
            if (null == response.attributes) {
                return resultMap;
            }
            resultMap = response.attributes;
        } catch (ServiceException e) {
            log.error("cannot obtain classification attributes ", e);
            e.printStackTrace();
        }
        return resultMap;
    }

    /**
     * 删除分类属性
     *
     * @param classificationObjects
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean deleteClassificationObjects(com.teamcenter.soa.client.model.ModelObject[] classificationObjects) {
        try {
            tcContextHolder.getClassificationService().deleteClassificationObjects(classificationObjects);
            return true;
        } catch (ServiceException e) {
            log.error("cannot obtain delete classification objects ", e);
            e.printStackTrace();
        }
        return false;
    }

    /**
     * delete classAttr by itemRevision and cid
     *
     * @param itemRevision
     * @return
     */
    @Override
    public boolean deleteClassificationObjects(com.teamcenter.soa.client.model.ModelObject itemRevision) {
        if (!isExistClassification(itemRevision)) {
            return true;
        }
        try {
            FindClassificationObjectsResponse findResponse = getTcContextHolder().getClassificationService().findClassificationObjects(Lists.newArrayList(itemRevision).stream().toArray(WorkspaceObject[]::new));

            if (findResponse.icos.values().size() > 0) {

                Map<WorkspaceObject, com.teamcenter.soa.client.model.ModelObject[]> responseMap = findResponse.icos;

                com.teamcenter.soa.client.model.ModelObject[] classificationObjects = responseMap.values().stream().flatMap(o -> Arrays.stream(o)).toArray(com.teamcenter.soa.client.model.ModelObject[]::new);

                boolean deleteResult = deleteClassificationObjects(classificationObjects);

                if (!deleteResult) {
                    log.error("delete old classAttr fail uid:{}.", itemRevision.getUid());
                } else {
                    log.info("delete old classAttr for uid:{}.", itemRevision.getUid());
                }
            }
        } catch (ServiceException e) {
            log.error("cannot delete old classAttr", e);
            e.printStackTrace();
        }
        return false;
    }


    /**
     * update class attr
     * 1. delete old class attr
     * 2. save new class attr
     *
     * @param revUid
     * @param cid
     * @param classAttributes
     * @return
     */
    @Override
    public boolean updateClassificationAttributes(String revUid, String cid, List<ClassAttribute> classAttributes) {

        if (Strings.isNullOrEmpty(revUid)) {
            log.error("uid cannot be empty");
            return false;
        }
        if (Strings.isNullOrEmpty(cid)) {
            log.error("cid cannot be empty");
            return false;
        }
        if (null == classAttributes || classAttributes.isEmpty()) {
            log.error("classAttributes cannot be empty");
            return false;
        }

        ModelObject itemRevision = getTcContextHolder().getTcLoadService().loadObject(revUid);

        if (isExistClassification(itemRevision)) {
            deleteClassificationObjects(itemRevision);
        } else {
            log.info("ItemRevision({}) has not any old classAttr", revUid);
        }

        return this.saveClassificationAttributes(revUid, cid, classAttributes);
    }

    /**
     * 获取分类属性LOV
     *
     * @param lovKeys
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, KeyLOVDefinition> getClassificationAttributeLOV(String[] lovKeys) {
        Map<String, KeyLOVDefinition> resultMap = new HashMap<String, KeyLOVDefinition>();
        try {
            GetKeyLOVsResponse response = tcContextHolder.getClassificationService().getKeyLOVs(lovKeys);
            if (null == response.keyLOVs) {
                return resultMap;
            }
            resultMap = response.keyLOVs;
        } catch (ServiceException e) {
            log.error("cannot obtain classification attribute LOV ", e);
            e.printStackTrace();
        }
        return resultMap;
    }

    /**
     * 根据ico获取wso
     *
     * @param uids
     */
    @Override
    public List<WorkspaceObject> getClassifiedObjects(String... uids) {
        List<WorkspaceObject> resultArr = new ArrayList<WorkspaceObject>();
        try {
            List<ModelObject> modelObjects = tcContextHolder.getTcLoadService().loadObjects(Arrays.asList(uids));
            FindClassifiedObjectsResponse response = tcContextHolder.getClassificationService().findClassifiedObjects(modelObjects.toArray(new ModelObject[modelObjects.size()]));
            resultArr = Arrays.asList(response.wsos);
        } catch (ServiceException e) {
            log.error("cannot obtain classified objects ", e);
            e.printStackTrace();
        }
        return proxy(resultArr, tcContextHolder);
    }

    /**
     * 传入ItemRevision，得到ItemRevision和其对应分类对象的Map
     *
     * @param wsos
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<ModelObject, List<ClassificationObject>> findClassificationObjects(List<? extends com.teamcenter.soa.client.model.ModelObject> wsos) {
        Map<ModelObject, List<ClassificationObject>> resultMap = new HashMap<ModelObject, List<ClassificationObject>>();
        try {
            FindClassificationObjectsResponse response = tcContextHolder.getClassificationService().findClassificationObjects(wsos.stream().toArray(WorkspaceObject[]::new));
            Map<WorkspaceObject, com.teamcenter.soa.client.model.ModelObject[]> respMap = response.icos;

            for (Entry<WorkspaceObject, com.teamcenter.soa.client.model.ModelObject[]> entry : respMap.entrySet()) {
                ModelObject objProxy = spy(entry.getKey(), tcContextHolder);
                GetClassificationObjectsResponse response2 = tcContextHolder.getClassificationService().getClassificationObjects(entry.getValue());
                Map<com.teamcenter.soa.client.model.ModelObject, ClassificationObject> resp2Map = response2.clsObjs;
                List<ClassificationObject> clsObjList = resp2Map.values().stream().collect(Collectors.toList());
                resultMap.put(objProxy, clsObjList);
            }
        } catch (ServiceException e) {
            log.error("cannot obtain classification objects ", e);
            e.printStackTrace();
        }
        return resultMap;
    }

    /**
     * 保存单个item的分类属性
     *
     * @param revUid
     * @param cid
     * @param attributes
     */
    @Override
    public Boolean saveClassificationAttributes(String revUid, String cid, List<ClassAttribute> attributes) {
        ClassificationObject clsObj = new ClassificationObject();
        try {
            clsObj.classId = cid;
            clsObj.properties = attibutesToProperties(attributes);
            clsObj.unitBase = getClassDescriptions(new String[]{cid}).get(cid).unitBase;
            clsObj.wsoId = (WorkspaceObject) tcContextHolder.getTcLoadService().loadObject(revUid);
            CreateClassificationObjectsResponse response = tcContextHolder.getClassificationService().createClassificationObjects(new ClassificationObject[]{clsObj});
            tcContextHolder.printAndLogMessageFromServiceData(response.data);
            ModelObject itemRev = tcContextHolder.getTcLoadService().loadObject(revUid);
            return isExistClassification(itemRev);
        } catch (ServiceException e) {
            log.error("create classification object failure", e);
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 根据分类cid查询分类的详细定义信息
     *
     * @param cids
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, ClassDef> getClassDescriptions(String[] cids) {
        Map<String, ClassDef> resultMap = new HashMap<String, ClassDef>();
        try {
            GetClassDescriptionsResponse response = tcContextHolder.getClassificationService().getClassDescriptions(cids);
            if (null == response.descriptions) {
                return resultMap;
            }
            resultMap = response.descriptions;
        } catch (ServiceException e) {
            log.error("obtain classification class failure", e);
            e.printStackTrace();
        }
        return resultMap;
    }

    /**
     * classification attributes cast to properties
     *
     * @param attributes
     * @return
     */
    private Classification.ClassificationProperty[] attibutesToProperties(List<ClassAttribute> attributes) {
        List<Classification.ClassificationProperty> properties = new ArrayList<Classification.ClassificationProperty>();
        Classification.ClassificationProperty property = null;
        ClassificationPropertyValue propertyValue = null;
        for (ClassAttribute attribute : attributes) {
            property = new Classification.ClassificationProperty();
            property.attributeId = attribute.id;
            propertyValue = new ClassificationPropertyValue();
            propertyValue.dbValue = attribute.defaultValue;
            propertyValue.displayValue = attribute.defaultValue;
            property.values = new ClassificationPropertyValue[]{propertyValue};
            properties.add(property);
        }
        return properties.toArray(new Classification.ClassificationProperty[properties.size()]);
    }

    /**
     * 根据分类的叶子节点的CID获取下属的物料的对象
     *
     * @param cids
     * @return
     */
    @Override
    public Map<String, List<ModelObject>> findClassifiedObjectsByCids(List<String> cids) {
        Map<String, List<ModelObject>> result = new HashMap<String, List<ModelObject>>();

        Classification.SearchClassAttributes[] scAttr = new Classification.SearchClassAttributes[1];
        scAttr[0] = new Classification.SearchClassAttributes();
        scAttr[0].classIds = cids.toArray(new String[cids.size()]);

        SearchResponse response;
        try {
            response = tcContextHolder.getClassificationService().search(scAttr);
            getTcContextHolder().printAndLogMessageFromServiceData(response.data);
        } catch (ServiceException e) {
            log.error(e);
            return result;
        }

        @SuppressWarnings("unchecked") Map<String, com.teamcenter.soa.client.model.ModelObject[]> icmMap = response.clsObjTags;
        for (String cid : cids) {
            List<ModelObject> list = new ArrayList<ModelObject>();
            result.put(cid, list);

            com.teamcenter.soa.client.model.ModelObject[] icmList = icmMap.get(cid);

            Classification.FindClassifiedObjectsResponse response2;
            try {
                response2 = tcContextHolder.getClassificationService().findClassifiedObjects(icmList);
                getTcContextHolder().printAndLogMessageFromServiceData(response.data);
            } catch (ServiceException e) {
                log.error(e);
                continue;
            }

            com.teamcenter.soa.client.model.ModelObject[] itemRevs = response2.wsos;
            for (com.teamcenter.soa.client.model.ModelObject itemRev : itemRevs) {
                ModelObject itemRevProxy = ProxyUtil.spy(itemRev, getTcContextHolder());
                list.add(itemRevProxy);
            }
        }

        return result;
    }

    // Lenght_mm -> mm
    // TODO: maybe the logic here is incorrect, the data query is necessary?o
    private String getUnit(String str) {
        if (str == null) {
            return null;
        }
        int pos = str.lastIndexOf("_");
        if (pos >= 0) {
            return str.substring(pos + 1);
        }
        return str;
    }

    /**
     * 减少数字类型多余格式， 如 00008.03000， 期望返回 8.03
     * TODO 需要判断分类属性类型，否则会将字符串类型的数据转换成数字  01 -> 1
     *
     * @param attrVal
     * @return
     */
    private String getAttrValue(String attrVal) {
        if (isIntegerStr(attrVal)) {
            attrVal = String.valueOf(Integer.parseInt(attrVal));
        } else if (isDoubleStr(attrVal)) {
            attrVal = String.valueOf(Double.parseDouble(attrVal));
        }
        return attrVal;
    }

    private boolean isIntegerStr(String val) {
        try {
            Integer.parseInt(val);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private boolean isDoubleStr(String val) {
        try {
            Double.parseDouble(val);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private String getPropertyDisplayName(String clzId, BigInteger attrId, Map<String, AttrDescriptor> clzAttrDescriptors) {
        Map<BigInteger, ClassAttrDescriptor> attrDesc = clzAttrDescriptors.get(clzId).attrDescMap;
        return attrDesc.get(attrId).name;
    }

    @AllArgsConstructor
    public static class ClassificationProperty {
        @Getter
        private BigInteger attrId;
        @Getter
        private String attrName;
        @Getter
        private String attrVal;
        @Getter
        private String attrUnit;

        @Override
        public String toString() {
            return attrName + ":" + attrVal + "" + attrUnit;
        }

    }

    @AllArgsConstructor
    public static class ClassificationNode {
        @Getter
        private String id;
        @Getter
        private String name;
        @Getter
        private Boolean isLeaf;

        @Override
        public String toString() {
            return id + ":" + name;
        }

    }

    @AllArgsConstructor
    public static class CID {
        @Getter
        private String cid;
        @Getter
        private String display;

        @Override
        public String toString() {
            return cid + ":" + display;
        }
    }

    @AllArgsConstructor
    @Data
    public static class ClassificationTree {
        private String status;
        private LinkedList<LinkedList<CID>> content;
    }

    @Override
    public List<String> searchHeaderObjectByName(String classificationName) {
        return serachHearderObject("-607 = " + classificationName);
    }

    private List<String> serachHearderObject(String searchParam) {
        List<String> args = Arrays.asList(searchParam, "2");
        InvokeICTMethodResponse resonse = getTcContextHolder().getTcSOAService().invokeICTMethod("ICCSService", "searchHeaderObject", args);
        List<String> result = Lists.newArrayList();
        ICT.Entry[] outputs = resonse.output[0].array[0].entries;
        for (ICT.Entry entry : outputs) {
            if (!Strings.isNullOrEmpty(entry.val)) {
                result.add(entry.val);
            }
        }
        return result;
    }

    @Override
    public List<String> searchHeaderObjectByCID(String cid) {
        return serachHearderObject("-600 = " + cid);
    }

    @Override
    public List<String> searchParentsByCID(String cid) {
        InvokeICTMethodResponse resonse = getTcContextHolder().getTcSOAService().invokeICTMethod("ICCSService", "askParent", Lists.newArrayList(cid));

        if (resonse.output.length == 0) {
            return Collections.emptyList();
        }
        String path = resonse.output[0].structure[0].args[1].val;
        List<String> list = Lists.newLinkedList();
        for (String str : path.split("::")) {
            if (!Strings.isNullOrEmpty(str)) {
                list.add(str);
            }
        }
        return list;
    }

    /**
     * 将查询出来的CID值转换为CID对象， 查询返回的数据格式为： classificationCID;classificationCIDName
     *
     * @param queryCidValue
     * @return
     */
    private static CID convertToCID(String queryCidValue) {
        if (Strings.isNullOrEmpty(queryCidValue)) {
            return null;
        }
        String[] cids = queryCidValue.split(";");
        return new CID(cids[0], cids[1]);
    }

    @Override
    public String searchHearderPath(String classificationNameOrCid, boolean isSearchByClassificationName) {
        LinkedList<LinkedList<CID>> content = new LinkedList<>();
        List<String> queryCIDValues = Lists.newArrayList();
        if (isSearchByClassificationName) {
            queryCIDValues = searchHeaderObjectByName(classificationNameOrCid);
        } else {
            queryCIDValues = searchHeaderObjectByCID(classificationNameOrCid);
        }
        if (queryCIDValues.isEmpty()) {
            ClassificationTree emptyTree = new ClassificationTree(Boolean.FALSE.toString(), null);
            log.warn("Search Classification Path is empty:\n" + emptyTree.toString());
            return emptyTree.toString();
        }
        // cidVal: cid;cidName
        for (String cidVal : queryCIDValues) {
            CID cid = convertToCID(cidVal);
            List<String> parentCids = searchParentsByCID(cid.cid);
            LinkedList<CID> path = new LinkedList<>();
            for (String parentCid : parentCids) {
                CID parent = convertToCID(searchHeaderObjectByCID(parentCid).get(0));
                path.add(parent);
            }
            path.add(cid);
            content.add(path);
        }
        ClassificationTree tree = new ClassificationTree(Boolean.TRUE.toString(), content);
        String serarchPath = tree.toString();
        log.info("Search Classification Path:\n" + serarchPath);
        return serarchPath;
    }

    /**
     * 判断是否存在于 分类库中，ics_classified 是已在分类库的标志
     *
     * @param itemRevision
     * @return
     */
    @Override
    public boolean isExistClassification(com.teamcenter.soa.client.model.ModelObject itemRevision) {
        return "YES".equalsIgnoreCase(ProxyUtil.proxy((ModelObject) itemRevision, tcContextHolder).getDisplayVal("ics_classified"));
    }


    /**
     * 是否为 必填属性
     *
     * @param classAttribute
     * @return
     */
    @Override
    public boolean isRequiredClassAttribute(ClassAttribute classAttribute) {
        return ((classAttribute.options & 0b100) >> 2) == 1;
    }

    /**
     * 是否为 引用属性
     *
     * @param classAttribute
     * @return
     */
    @Override
    public boolean isReferenceClassAttribute(ClassAttribute classAttribute) {
        return ((classAttribute.options & 0b10000000) >> 7) == 1;
    }

    /**
     * 是否为 引用属性
     *
     * @param classAttribute
     * @return
     */
    @Override
    public boolean isHiddenClassAttribute(ClassAttribute classAttribute) {
        return ((classAttribute.options & 0b100000000000000000000) >> 20) == 1;
    }
}

package com.eingsoft.emop.tc.service;

import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.service.impl.TcClassificationServiceImpl.ClassificationNode;
import com.eingsoft.emop.tc.service.impl.TcClassificationServiceImpl.ClassificationProperty;
import com.teamcenter.services.strong.classification._2007_01.Classification.ClassAttribute;
import com.teamcenter.services.strong.classification._2007_01.Classification.ClassDef;
import com.teamcenter.services.strong.classification._2007_01.Classification.ClassificationObject;
import com.teamcenter.services.strong.classification._2007_01.Classification.KeyLOVDefinition;
import com.teamcenter.soa.client.model.strong.WorkspaceObject;

import java.util.List;
import java.util.Map;

public interface TcClassificationService extends TcService {

    Map<String, List<ClassificationProperty>> getClassificationPropsByRevUids(String... revUids);

    /**
     * 根据分类UID获取分类属性
     *
     * @throws Exception
     */
    Map<String, List<ClassificationProperty>> getClassificationPropsByIcoUids(String... icoUids);

    /**
     * 此方法比getClassificationPropsByRevUids根据UID批量加载性能要高， 无需先根据UID加载ModelObject
     *
     * @param objs
     * @return
     */
    Map<ModelObject, List<ClassificationProperty>>
    getClassificationPropsByRevisions(com.teamcenter.soa.client.model.ModelObject... objs);

    /**
     * 获取分类树单层子节点集
     *
     * @param cids
     * @return
     */
    Map<String, List<ClassificationNode>> getChildren(String... cids);

    /**
     * 获取分类属性
     *
     * @param cids
     */
    Map<String, ClassAttribute[]> getClassificationAttributes(String... cids);

    /**
     * 获取分类属性LOV
     */
    Map<String, KeyLOVDefinition> getClassificationAttributeLOV(String... lovKeys);

    /**
     * 根据ico获取wso
     */
    List<WorkspaceObject> getClassifiedObjects(String... uids);

    /**
     * 传入Ision，得到ItemRevision和其对应分类对象的Map
     */
    Map<ModelObject, List<ClassificationObject>>
    findClassificationObjects(List<? extends com.teamcenter.soa.client.model.ModelObject> wsos);

    /**
     * 保存单个item的分类属性
     *
     * @param revUid
     * @param cid
     * @param attributes
     */
    Boolean saveClassificationAttributes(String revUid, String cid, List<ClassAttribute> attributes);

    /**
     * 根据分类cid查询分类的详细定义信息
     *
     * @param cids
     * @return
     */
    Map<String, ClassDef> getClassDescriptions(String... cids);

    /**
     * 根据分类的叶子节点的CID获取下属的物料的对象
     *
     * @param cids
     * @return
     */
    Map<String, List<ModelObject>> findClassifiedObjectsByCids(List<String> cids);

    /**
     * 根据分类名称搜索所有分类， 分类结果是分类ID+分号+名称
     *
     * @param classificationName 淋浴器*
     * @return 返回结果如： [-120002;淋浴器, -150070;淋浴器阀体]
     */
    List<String> searchHeaderObjectByName(String classificationName);

    /**
     * 根据分类ID搜索所有分类， 分类结果是分类ID+分号+名称
     *
     * @param cid D*04
     * @return 返回结果如： [D104;淋浴器, D204;淋浴器, D504;浴缸]
     */
    List<String> searchHeaderObjectByCID(String cid);

    /**
     * 根据CID 找到父节点CID 路径, 按顺序返回linked list<br>
     * SAM::ICM::MaterialLibrary::1Product::XiHe1::
     *
     * @param cid
     * @return LinkedList [SAM,ICM,MaterialLibrary,1Product,XiHe1]
     */
    List<String> searchParentsByCID(String cid);

    /**
     * 根据分类名称或者分类ID， 搜索并返回指定结构的搜索路径值 <br>
     * 搜索结果有值，则status 返回 true； 为空，则status 返回false <br>
     * {"status":"true","content":[[{"cid":"SAM","display":"TC Classification Root" },{"cid":"ICM","display":
     * "Classification Root"
     * },{"cid":"MaterialLibrary","display":"物料库"},{"cid":"1Product","display":"成品"},{"cid":"XiHe1","display":"西河"},{
     * "cid":"D104","display":"淋浴器"}],[{"cid":"SAM","display":"TC Classification Root"},{"cid":"ICM","display":
     * "Classification Root"
     * },{"cid":"MaterialLibrary","display":"物料库"},{"cid":"2SProduct","display":"半成品"},{"cid":"XiHe2","display":"西河"},{
     * "cid":"D204","display":"淋浴器"}],[{"cid":"SAM","display":"TC Classification Root"},{"cid":"ICM","display":
     * "Classification Root"
     * },{"cid":"MaterialLibrary","display":"物料库"},{"cid":"5Part","display":"零件"},{"cid":"XiHe5","display":"西河"},{"cid":
     * "D504","display":"浴缸"}]]}
     *
     * @param classificationNameOrCid
     * @param isSearchByClassificationName true 则根据名称搜索， false 则根据CID搜索
     * @return
     */
    String searchHearderPath(String classificationNameOrCid, boolean isSearchByClassificationName);

    /**
     * 删除分类属性
     *
     * @param classificationObjects
     */
    boolean deleteClassificationObjects(com.teamcenter.soa.client.model.ModelObject[] classificationObjects);

    /**
     * delete classAttr by itemRevision and cid
     *
     * @param itemRevision
     * @return
     */
    boolean deleteClassificationObjects(com.teamcenter.soa.client.model.ModelObject itemRevision);


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
    boolean updateClassificationAttributes(String revUid, String cid, List<ClassAttribute> classAttributes);


    /**
     * 判断是否存在于 分类库中，ics_classified 是已在分类库的标志
     *
     * @param itemRevision
     * @return
     */
    boolean isExistClassification(com.teamcenter.soa.client.model.ModelObject itemRevision);


    /**
     * 是否为 必填属性
     * @param classAttribute
     * @return
     */
    boolean isRequiredClassAttribute(ClassAttribute classAttribute);

    /**
     * 是否为 引用属性
     * @param classAttribute
     * @return
     */
    boolean isReferenceClassAttribute(ClassAttribute classAttribute);

    /**
     * 是否为 隐藏属性
     * @param classAttribute
     * @return
     */
    boolean isHiddenClassAttribute(ClassAttribute classAttribute);
}

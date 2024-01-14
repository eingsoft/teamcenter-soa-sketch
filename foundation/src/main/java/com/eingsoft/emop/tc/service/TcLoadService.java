package com.eingsoft.emop.tc.service;

import java.util.List;

import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.service.cache.SimpleModelObjectCache;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.exceptions.NotLoadedException;

public interface TcLoadService extends TcService {

    /**
     * 加载对象， 无初始化加载的属性
     * 
     * @param uid
     * @return
     */
    ModelObject loadObject(String uid);

    /**
     * 批量加载对象， 无初始化加载的属性
     * 
     * @param uids
     * @return
     */
    List<ModelObject> loadObjects(List<String> uids);

    /**
     * 根据UID， 加载Model Object， 并且批量加载 recommended 属性
     * 
     * @param uid
     * @return
     */
    ModelObject loadObjectWithProperties(String uid);

    /**
     * 根据UID数组，批量加载Model Object，并且批量加载属性
     * 
     * @param uids
     * @return
     */
    List<ModelObject> loadObjectsWithProperties(List<String> uids);

    /**
     * load recommended properties, pay attention, it will first retrieve the cached data from
     * {@link SimpleModelObjectCache}
     * 
     */
    List<ModelObject> loadProperties(List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjects);

    /**
     * load recommended properties, pay attention, it will first retrieve the cached data from
     * {@link SimpleModelObjectCache}
     * 
     */
    ModelObject loadProperties(com.teamcenter.soa.client.model.ModelObject modelObject);
    
    /**
     * load all properties
     * 
     */
    ModelObject loadAllProperties(com.teamcenter.soa.client.model.ModelObject modelObject);
    
    /**
     * load all properties
     * 
     */
    List<ModelObject> loadAllProperties(List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjects);

    /**
     * 加载某个对象的某个属性
     * 
     * @param modelObject
     * @param property
     */
    ModelObject loadProperty(com.teamcenter.soa.client.model.ModelObject modelObject, String property);

    /**
     * 批量加载对象的某个属性
     * 
     * @param modelObjects
     * @param property
     */
    List<ModelObject> loadProperty(List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjects,
        String property);

    /**
     * 加载对象的多个属性
     * 
     * @param modelObject
     * @param properties
     */
    ModelObject loadProperties(com.teamcenter.soa.client.model.ModelObject modelObject, List<String> properties);

    /**
     * 批量加载对象的多个属性
     * 
     * @param modelObjects
     * @param properties
     * @return
     */
    List<ModelObject> loadProperties(List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjects,
        List<String> properties);
    
    /**
     * 批量加载对象的多个属性以及推荐属性
     * 
     * @param modelObjects
     * @param properties
     * @return
     */
    List<ModelObject> loadPropertiesAndRecommendedProps(List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjects,
        List<String> properties);

    /**
     * 根据UID 获取ItemRevision对象, 并加载所有属性
     * 
     * @param uid
     * @return
     */
    ItemRevision getItemRevision(String uid);

    /**
     * 根据UID 获取Item对象， 并加载所有对象属性
     * 
     * @param uid
     * @return
     */
    Item getItem(String uid);

    /**
     * 根据零组件版本获取零组件对象
     * 
     * @param itemRev
     * @return
     */
    Item getItem(ItemRevision itemRev);

    /**
     * 根据Item对象， 获取对象下的版本对象列表
     * 
     * @param item
     * @return
     * @throws NotLoadedException
     */
    List<ItemRevision> getItemRevisions(Item item);

    /**
     * 根据Item对象，获取对下指定版本号的对象
     * 
     * @param item
     * @return
     */
    ItemRevision getItemRevision(Item item, String revId);

    /**
     * 根据Item对象，获取最新零组件版本。
     * 
     * @param item
     * @return
     * @throws NotLoadedException
     */
    ItemRevision getLatestItemRevision(Item item);

    List<String> getDeletedObjects(List<String> uids);

    /**
     * "另存为" 操作，revision下面的数据集会带过来
     */
    ItemRevision saveAs(String uid, String newItemId);
}

package com.eingsoft.emop.tc.service.impl;

import com.eingsoft.emop.tc.BMIDE;
import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;
import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcLoadService;
import com.eingsoft.emop.tc.service.TcObjectPropertyLoadStatService;
import com.eingsoft.emop.tc.service.cache.ModelObjectCache;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core._2008_06.DataManagement.SaveAsNewItemInfo;
import com.teamcenter.services.strong.core._2008_06.DataManagement.SaveAsNewItemOutput2;
import com.teamcenter.services.strong.core._2008_06.DataManagement.SaveAsNewItemResponse2;
import com.teamcenter.soa.client.model.ErrorStack;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.exceptions.NotLoadedException;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.stream.Collectors;

import static com.eingsoft.emop.tc.util.ProxyUtil.spy;
import static java.util.Collections.emptyList;

/**
 * To load model objects and properties
 *
 */
@Log4j2
@ScopeDesc(Scope.TcContextHolder)
public class TcLoadServiceImpl implements TcLoadService {
    @Getter
    private TcContextHolder tcContextHolder;

    public TcLoadServiceImpl(TcContextHolder tcContextHolder) {
        this.tcContextHolder = tcContextHolder;
    }

    /**
     * 加载对象， 无初始化加载的属性
     * 
     * @param uid
     * @return
     */
    @Override
    public ModelObject loadObject(String uid) {
        List<ModelObject> modelObjects = loadObjects(Arrays.asList(uid));
        return modelObjects.size() > 0 ? modelObjects.get(0) : null;
    }

    @Override
    public List<String> getDeletedObjects(List<String> uids) {
        DataManagementService dmService = this.tcContextHolder.getDataManagementService();
        ServiceData serviceData = dmService.loadObjects(uids.toArray(new String[uids.size()]));
        for (int i = 0; i < serviceData.sizeOfPartialErrors(); i++) {
            ErrorStack stack = serviceData.getPartialError(i);
            if (!Arrays.toString(stack.getMessages()).contains("在数据库中不存在")) {
                return retrieveDeletedObjects(serviceData);
            }
        }
        List<String> existingUids = new ArrayList<>();
        List<com.teamcenter.soa.client.model.ModelObject> existingModelObjects = new ArrayList<>();
        for (int i = 0; i < serviceData.sizeOfPlainObjects(); i++) {
            existingUids.add(serviceData.getPlainObject(i).getUid());
            existingModelObjects.add(serviceData.getPlainObject(i));
        }
        Set<String> result = new HashSet<>(uids);
        result.removeAll(existingUids);
        // try to refresh the existing objects, to avoid cache
        serviceData =
            tcContextHolder.getDataManagementService().refreshObjects(
                existingModelObjects.toArray(new com.teamcenter.soa.client.model.ModelObject[existingModelObjects
                    .size()]));
        result.addAll(retrieveDeletedObjects(serviceData));
        return new ArrayList<String>(result);
    }

    private List<String> retrieveDeletedObjects(ServiceData serviceData) {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < serviceData.sizeOfDeletedObjects(); i++) {
            result.add(serviceData.getDeletedObject(i));
        }
        return result;
    }

    /**
     * 批量加载对象， 无初始化加载的属性
     * 
     * @param uids
     * @return
     */
    @Override
    public List<ModelObject> loadObjects(List<String> uids) {
        if (uids.isEmpty()) {
            log.warn("the param of uids must neither empty nor null when load objects. ");
            return emptyList();
        }

        DataManagementService dmService = this.tcContextHolder.getDataManagementService();
        ServiceData serviceData = dmService.loadObjects(uids.toArray(new String[uids.size()]));

        tcContextHolder.printAndLogMessageFromServiceData(serviceData);

        List<ModelObject> modelObjects = new ArrayList<ModelObject>(serviceData.sizeOfPlainObjects());
        for (int i = 0; i < serviceData.sizeOfPlainObjects(); i++) {
            modelObjects.add(spy(serviceData.getPlainObject(i), tcContextHolder));
        }

        if (modelObjects.size() < uids.size()) {
            log.warn("Expected load " + uids.size() + " objects instead of " + modelObjects.size() + " objects.");
        }

        if (SOAExecutionContext.current().isDisableTCServerCache()) {
            log.debug("refreshing modle objects: " + uids.toString());
            tcContextHolder.getTcDataManagementService().refreshObjects(modelObjects);
        }

        return modelObjects;
    }

    /**
     * 根据UID， 加载Model Object， 并且批量加载属性
     * 
     * @param uid
     * @return
     */
    @Override
    public ModelObject loadObjectWithProperties(String uid) {
        List<ModelObject> modelObjects = loadObjectsWithProperties(Arrays.asList(uid));
        return modelObjects.size() > 0 ? modelObjects.get(0) : null;
    }

    /**
     * 根据UID数组，批量加载Model Object，并且批量加载属性
     * 
     * @param uids
     * @return
     */
    @Override
    public List<ModelObject> loadObjectsWithProperties(List<String> uids) {
        List<ModelObject> modelObjects = loadObjects(uids);
        // load all properties, if handle batch data, don't recommend load all
        // property
        if (modelObjects.size() > 0) {
            modelObjects = loadProperties(modelObjects);
        }

        return modelObjects;
    }

    /**
     * load properties
     * 
     */
    @Override
    public List<ModelObject> loadProperties(
        final List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjects) {
        if (modelObjects.isEmpty()) {
            return emptyList();
        }
        if (!SOAExecutionContext.current().getModelObjectCache().isDisabled()) {
            ModelObjectCache cache = SOAExecutionContext.current().getModelObjectCache();
            List<ModelObject> notCached =
                cache.getNotExistsInCache(Collections.unmodifiableList(modelObjects.stream()
                    .map(o -> spy(o, tcContextHolder)).collect(Collectors.toList())));
            Set<String> properties =
                notCached
                    .stream()
                    .map(
                        m -> Arrays.asList(TcObjectPropertyLoadStatService.getInstance()
                            .getRecommendedLoadPropertyNames(spy(m, tcContextHolder)))).flatMap(List::stream)
                    .collect(Collectors.toSet());
            if (!properties.isEmpty()) {
                List<ModelObject> newLoadedObjects = loadProperties(notCached, new ArrayList<String>(properties));
                newLoadedObjects.stream().forEach(m -> cache.put(m.getUid(), spy(m, tcContextHolder)));
            } else {
                // make sure they are represent in cache
                modelObjects.stream().forEach(m -> cache.put(m.getUid(), spy(m, tcContextHolder)));
            }
            return modelObjects.stream().map(m -> (ModelObject)cache.retrieve(m.getUid())).collect(Collectors.toList());
        } else {
            log.info(ModelObjectCache.class + " instance is expected here, but it is not represent, please check "
                + SOAExecutionContext.class + " is working well.");
            Set<String> properties =
                modelObjects
                    .stream()
                    .map(
                        m -> Arrays.asList(TcObjectPropertyLoadStatService.getInstance()
                            .getRecommendedLoadPropertyNames(spy(m, tcContextHolder)))).flatMap(List::stream)
                    .collect(Collectors.toSet());
            return loadProperties(modelObjects, new ArrayList<String>(properties));
        }
    }

    /**
     * load properties
     * 
     */
    @Override
    public ModelObject loadProperties(com.teamcenter.soa.client.model.ModelObject modelObject) {
        if (modelObject == null) {
            return null;
        }
        return loadProperties(Arrays.asList(modelObject)).get(0);
    }

    /**
     * 加载某个对象的某个属性
     * 
     * @param modelObject
     * @param property
     */
    @Override
    public ModelObject loadProperty(com.teamcenter.soa.client.model.ModelObject modelObject, String property) {
        List<ModelObject> updateObjs = loadProperties(Arrays.asList(modelObject), Arrays.asList(property));
        return updateObjs.size() > 0 ? updateObjs.get(0) : spy(modelObject, tcContextHolder);
    }

    /**
     * 批量加载对象的某个属性 F
     * 
     * @param modelObjects
     * @param property
     */
    @Override
    public List<ModelObject> loadProperty(List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjects,
        String property) {
        return loadProperties(modelObjects, Arrays.asList(property));
    }

    /**
     * 加载对象的多个属性
     * 
     * @param modelObject
     * @param properties
     */
    @Override
    public ModelObject loadProperties(com.teamcenter.soa.client.model.ModelObject modelObject, List<String> properties) {
        List<ModelObject> updateObjs = loadProperties(Arrays.asList(modelObject), properties);
        return updateObjs.size() > 0 ? updateObjs.get(0) : spy(modelObject, tcContextHolder);
    }

    /**
     * 批量加载对象的多个属性
     * 
     * @param modelObjects
     * @param properties
     * @return
     */
    @Override
    public List<ModelObject> loadProperties(
        final List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjects, final List<String> properties) {
        if (properties.isEmpty()) {
            log.warn("the param of properties must neither empty nor null when load properties. ");
            return spy(modelObjects, getTcContextHolder());
        }
        if (!SOAExecutionContext.current().getModelObjectCache().isDisabled()) {
            ModelObjectCache cache = SOAExecutionContext.current().getModelObjectCache();
            List<? extends com.teamcenter.soa.client.model.ModelObject> toBeUpdatedObjs =
                modelObjects
                    .stream()
                    .filter(
                        m -> {
                            ModelObject existingModelObject = cache.retrieve(m.getUid());
                            if (existingModelObject == null) {
                                return true;
                            }
                            Set<String> toBeCheckedProperties =
                                new HashSet<String>(existingModelObject.getTypeObject().getPropDescs().keySet());
                            toBeCheckedProperties.retainAll(properties);
                            Set<String> loaded = existingModelObject.getLoadedPropertyNames();
                            return toBeCheckedProperties.stream().anyMatch(p -> !loaded.contains(p));
                        }).collect(Collectors.toList());
            if (!toBeUpdatedObjs.isEmpty()) {
                List<ModelObject> loaded = loadPropertiesInternal(toBeUpdatedObjs, properties);
                loaded.stream().forEach(m -> cache.put(m.getUid(), spy(m, tcContextHolder)));
            }
            return modelObjects.stream().map(m -> cache.retrieve(m.getUid())).collect(Collectors.toList());
        }
        return loadPropertiesInternal(modelObjects, properties);
    }
    
    @Override
    public List<ModelObject> loadPropertiesAndRecommendedProps(
        final List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjects, final List<String> properties) {
      Set<String> recommendedProperties =
          modelObjects
              .stream()
              .map(
                  m -> Arrays.asList(TcObjectPropertyLoadStatService.getInstance()
                      .getRecommendedLoadPropertyNames(spy(m, tcContextHolder)))).flatMap(List::stream)
              .collect(Collectors.toSet());
      //combine the recommended properties and the given properties
      recommendedProperties.addAll(properties);
      return loadProperties(modelObjects, new ArrayList<String>(recommendedProperties));
    }

    protected List<ModelObject> loadPropertiesInternal(
        final List<? extends com.teamcenter.soa.client.model.ModelObject> toBeUpdateModelObjects,
        final List<String> properties) {
        if (SOAExecutionContext.current().isDisableTCServerCache()) {
            if (log.isDebugEnabled()) {
                log.debug("refreshing model objects {}",
                    toBeUpdateModelObjects.stream().map(o -> o.getUid() + "(" + o.getTypeObject().getName() + ")")
                        .collect(Collectors.toList()));
            }
            tcContextHolder.getTcDataManagementService().refreshObjects(toBeUpdateModelObjects);
            log.debug("refresh done");
        }
        // all existing properties
        Set<String> existingPropertyNames =
            toBeUpdateModelObjects.stream().map(m -> spy(m, tcContextHolder).getLoadedPropertyNames())
                .flatMap(Set::stream).collect(Collectors.toSet());
        // add the missing (new loading) properties
        existingPropertyNames.addAll(properties);
        if (log.isDebugEnabled()) {
            log.debug(
                "loading {} properties from {} model objects {} of properties {}",
                existingPropertyNames.size(),
                toBeUpdateModelObjects.size(),
                toBeUpdateModelObjects.stream().map(o -> o.getUid() + "(" + o.getTypeObject().getName() + ")")
                    .collect(Collectors.toList()), existingPropertyNames);
        }
        ServiceData serviceData =
            this.tcContextHolder.getDataManagementService().getProperties(
                toBeUpdateModelObjects.toArray(new com.teamcenter.soa.client.model.ModelObject[toBeUpdateModelObjects
                    .size()]), existingPropertyNames.toArray(new String[existingPropertyNames.size()]));
        log.debug("load properties done");
        tcContextHolder.printAndLogMessageFromServiceData(serviceData);
        List<ModelObject> updateObjs = new ArrayList<>();
        if (serviceData.sizeOfPlainObjects() > 0) {
            for (int i = 0; i < serviceData.sizeOfPlainObjects(); i++) {
                updateObjs.add(spy(serviceData.getPlainObject(i), tcContextHolder));
            }
        }
        return updateObjs;
    }

    /**
     * 根据UID 获取ItemRevision对象, 并加载所有属性
     * 
     * @param uid
     * @return
     */
    @Override
    public ItemRevision getItemRevision(String uid) {
        return (ItemRevision)loadObjectWithProperties(uid);
    }

    /**
     * 根据UID 获取Item对象， 并加载所有对象属性
     * 
     * @param uid
     * @return
     */
    @Override
    public Item getItem(String uid) {
        return (Item)loadObjectWithProperties(uid);
    }

    /**
     * 根据零组件版本获取零组件对象
     * 
     * @param itemRev
     * @return
     */
    @Override
    public Item getItem(ItemRevision itemRev) {
        return (Item)spy(itemRev, tcContextHolder).get(BMIDE.PROP_ITEMS_TAG);
    }

    /**
     * 根据Item对象， 获取对象下的版本对象列表
     * 
     * @param item
     * @return
     * @throws NotLoadedException
     */
    @Override
    public List<ItemRevision> getItemRevisions(Item item) {
        List wrappers = (List)spy(item, tcContextHolder).get(BMIDE.PROP_REVISION_LIST);
        List<ItemRevision> revisions = new ArrayList<>();
        for (Object wrapper : wrappers) {
            revisions.add((ItemRevision)wrapper);
        }
        return revisions;
    }

    /**
     * 根据Item对象，获取对下指定版本号的对象
     * 
     * @param item
     * @return
     */
    @Override
    public ItemRevision getItemRevision(Item item, String revId) {
        @SuppressWarnings("unchecked")
        List<ModelObject> wrappers = (List<ModelObject>)spy(item, tcContextHolder).get(BMIDE.PROP_REVISION_LIST);
        for (Object e : wrappers) {
            String curRevId = (String)((com.eingsoft.emop.tc.model.ModelObject)e).get(BMIDE.PROP_ITEM_REV_ID);
            if (revId.equals(curRevId)) {
                return (ItemRevision)e;
            }
        }
        return null;
    }

    /**
     * 根据Item对象，获取最新零组件版本。
     * 
     * @param item
     * @return
     * @throws NotLoadedException
     */
    @Override
    public ItemRevision getLatestItemRevision(Item item) {
        List<ItemRevision> revisions = getItemRevisions(item);
        return revisions.get(revisions.size() - 1);
    }

    @Override
    public ModelObject loadAllProperties(com.teamcenter.soa.client.model.ModelObject modelObject) {
        return loadProperties(modelObject, new ArrayList<String>(modelObject.getTypeObject().getPropDescs().keySet()));
    }

    @Override
    public List<ModelObject>
        loadAllProperties(List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjects) {
        return loadProperties(
            modelObjects,
            new ArrayList<String>(modelObjects.stream().map(m -> m.getTypeObject().getPropDescs().keySet())
                .flatMap(Set::stream).collect(Collectors.toSet())));
    }

    @Override
    public ItemRevision saveAs(String uid, String newItemId) {
        SaveAsNewItemInfo info = new SaveAsNewItemInfo();
        info.baseItemRevision = getTcContextHolder().getTcLoadService().getItemRevision(uid);
        info.newItemId = newItemId;
        SaveAsNewItemResponse2 resp = getTcContextHolder().getDataManagementService().saveAsNewItem2(new SaveAsNewItemInfo[]{info});
        Iterator<SaveAsNewItemOutput2> it = (Iterator<SaveAsNewItemOutput2>) resp.saveAsOutputMap.values().iterator();
        if (it.hasNext()) {
            return it.next().newItemRev;
        } else {
            throw new RuntimeException("cannot perform save as, please check log...");
        }
    }

}

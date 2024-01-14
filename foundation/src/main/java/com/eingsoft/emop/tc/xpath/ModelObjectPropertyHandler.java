package com.eingsoft.emop.tc.xpath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.jxpath.DynamicPropertyHandler;
import com.eingsoft.emop.tc.service.TcContextHolderAware;
import com.eingsoft.emop.tc.util.ProxyUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.teamcenter.soa.client.model.ModelObject;
import lombok.NonNull;

public class ModelObjectPropertyHandler implements DynamicPropertyHandler, TcContextHolderAware {

    private static Cache<String, Object> ephemeralCache = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();
    private ModelObjectBatchInvoker invoker = new ModelObjectBatchInvoker();
    
    @Override
    public String[] getPropertyNames(@NonNull Object object) {
        if (!(object instanceof ModelObject)) {
            return null;
        }
        ModelObject obj = (ModelObject)object;
        List<String> propertyNames = new ArrayList<String>(obj.getTypeObject().getPropDescs().keySet());
        return propertyNames.toArray(new String[propertyNames.size()]);
    }

    @Override
    public Object getProperty(@NonNull Object object, @NonNull String propertyName) {
      if (!(object instanceof ModelObject)) {
        return null;
      }
      ModelObject modelObject = ((ModelObject) object);
      if ("type".equals(propertyName) || "typeObject".equals(propertyName)) {
        return modelObject.getTypeObject();
      }
      Object cached = ephemeralCache.getIfPresent(getUniqueKey(modelObject, propertyName));
      if (cached != null) {
        return cached instanceof NullObject ? null : cached;
      }
      Object result = invoker.getPropertyValue((ModelObject) object, propertyName);
      ephemeralCache.put(getUniqueKey(modelObject, propertyName), result == null ? new NullObject() : result);
      return result;
    }

    private String getUniqueKey(ModelObject modelObject, String propertyName) {
      return modelObject.getUid() + "." + propertyName;
    }
    
    @Override
    public void setProperty(@NonNull Object object, @NonNull String propertyName, Object value) {
        throw new IllegalStateException("not implemented yet, tc xpath only supports readonly first.");
    }

    public static class ModelObjectBatchInvoker implements TcContextHolderAware {
      private static ThreadLocal<Map<String, Set<ModelObject>>> objectsByType = new ThreadLocal<>();
  
    public Object getPropertyValue(@NonNull ModelObject object, String propertyName) {
      com.eingsoft.emop.tc.model.ModelObject m = ProxyUtil.spy(object, getTcContextHolder());
      if (m.getLoadedPropertyNames().contains(propertyName)) {
        return m.get(propertyName);
      }
      addObject(object);
      getTcContextHolder().getTcLoadService().loadPropertiesAndRecommendedProps(
          new ArrayList<>(getObjectsCache().get(object.getTypeObject().getName())), Arrays.asList(propertyName));
      Object result = ProxyUtil.spy((ModelObject) object, getTcContextHolder()).get(propertyName);
      if (result instanceof ModelObject) {
        addObject(object);
      } else if (result instanceof Collection) {
        ((Collection<Object>) result).stream().parallel().filter(o -> o instanceof ModelObject).forEach(o -> addObject((ModelObject) o));
      }
      return result;
    }
  
      private static Map<String, Set<ModelObject>> getObjectsCache() {
        Map<String, Set<ModelObject>> cache = objectsByType.get();
        if (cache == null) {
          cache = new HashMap<>();
          objectsByType.set(cache);
        }
        return cache;
      }
  
      private static void addObject(ModelObject obj) {
        Set<ModelObject> objs = getObjectsCache().get(obj.getTypeObject().getName());
        if (objs == null) {
          objs = new HashSet<>();
          getObjectsCache().put(obj.getTypeObject().getName(), objs);
        }
        objs.add(obj);
      }
      
      public static void addModelObjects(@NonNull List<? extends Object> modelObjects) {
        modelObjects.forEach(m -> {
          if (m instanceof ModelObject) {
            addObject((ModelObject) m);
          }
        });
      }
      
      public static void cleanup() {
        objectsByType.remove();
      }
    }
    
    private static class NullObject {
    }
}
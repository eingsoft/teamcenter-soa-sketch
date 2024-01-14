package com.eingsoft.emop.tc.service;

import com.eingsoft.emop.tc.model.ModelObject;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.NonNull;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * @author Beam
 * <p>
 * Used to have the stats. of property referenced count by type. Invoke Model.getProperty("xxx") will trigger
 * one reference calculation.
 */
public interface TcObjectPropertyLoadStatService {

    /**
     * add usage statistics
     */
    default void addReferencedProperty(@NonNull String type, @NonNull String propName) {
        addReferencedProperties(type, new String[]{propName});
    }

    void addReferencedProperties(@NonNull String type, @NonNull String[] propNames);

    /**
     * referenced properties plus the default properties
     */
    String[] getRecommendedLoadPropertyNames(@NonNull ModelObject modelObject);

    static TcObjectPropertyLoadStatService getInstance() {
        ServiceLoader<TcObjectPropertyLoadStatService> loader = ServiceLoader.load(TcObjectPropertyLoadStatService.class);
        return loader.iterator().next();
    }

    /**
     * clean up statistics
     */
    void cleanUp();

    /**
     * unmodifiable property usage data
     */
    Map<String, List<String>> getCache();
}

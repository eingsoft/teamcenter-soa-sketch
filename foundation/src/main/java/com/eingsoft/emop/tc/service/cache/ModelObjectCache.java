package com.eingsoft.emop.tc.service.cache;

import java.util.List;
import java.util.Map;

import com.eingsoft.emop.tc.model.ModelObject;

public interface ModelObjectCache {

    void put(String uid, ModelObject obj);

    void remove(String uid);

    void cleanUp();

    ModelObject retrieve(String uid);

    Map<String, ModelObject> asMap();

    <T extends com.teamcenter.soa.client.model.ModelObject> List<T> getNotExistsInCache(List<T> objs);

    void disable();

    boolean isDisabled();
}

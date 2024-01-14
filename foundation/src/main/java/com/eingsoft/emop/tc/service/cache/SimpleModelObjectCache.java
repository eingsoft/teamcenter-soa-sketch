package com.eingsoft.emop.tc.service.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NonNull;

import com.eingsoft.emop.tc.model.ModelObject;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class SimpleModelObjectCache implements ModelObjectCache {

    @Getter
    private boolean disabled = false;
    private Cache<String, ModelObject> cache = CacheBuilder.newBuilder().build();

    @Override
    public void put(final @NonNull String uid, final @NonNull ModelObject obj) {
        if (!isDisabled()) {
            cache.put(uid, obj);
        }
    }

    @Override
    public void remove(final @NonNull String uid) {
        cache.invalidate(uid);
    }

    @Override
    public void cleanUp() {
        cache.invalidateAll();
    }

    @Override
    public ModelObject retrieve(String uid) {
        return cache.getIfPresent(uid);
    }

    @Override
    public <T extends com.teamcenter.soa.client.model.ModelObject> List<T> getNotExistsInCache(final List<T> objs) {
        List<T> result = new ArrayList<T>(objs);
        result.removeAll(cache.asMap().values());
        return result;
    }

    @Override
    public Map<String, ModelObject> asMap() {
        return Collections.unmodifiableMap(cache.asMap());
    }

    @Override
    public void disable() {
        disabled = true;
    }
}

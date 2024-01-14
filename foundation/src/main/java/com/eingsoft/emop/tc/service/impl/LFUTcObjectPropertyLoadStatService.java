package com.eingsoft.emop.tc.service.impl;

import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.service.TcObjectPropertyLoadStatService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * LFU（Least Frequently Used） implementation
 *
 * @author beam
 */
@Log4j2
public class LFUTcObjectPropertyLoadStatService implements TcObjectPropertyLoadStatService {

    private static final String CACHE_FILE_PATH = System.getProperty("user.home") + File.separator + "teamcenter-soa-sketch.typeToPropertyCache.service";
    private static final int SAVE_INTERVAL_MINUTES = 5;

    public static final String RECOMMENDED_PROPERTIES_ADD_DEFAULT_PROPERTIES = "RECOMMENDED_PROPERTIES_ADD_DEFAULT_PROPERTIES";

    public static final String RECOMMENDED_PROPERTIES_EXPIRE_MINUTE = "RECOMMENDED_PROPERTIES_EXPIRE_MINUTE";

    public static final String RECOMMENDED_PROPERTIES_MAX_PROP_COUNT_PER_TYPE = "RECOMMENDED_PROPERTIES_MAX_PROP_COUNT_PER_TYPE";

    private static Cache<String, Cache<String, String>> typeToPropertyCache = null;

    public LFUTcObjectPropertyLoadStatService() {
        if (typeToPropertyCache == null) {
            // load from disk
            typeToPropertyCache = loadCacheFromFile();
        }
        if (typeToPropertyCache == null) {
            typeToPropertyCache = Caffeine.newBuilder().maximumSize(500).expireAfterAccess(Integer.getInteger(RECOMMENDED_PROPERTIES_EXPIRE_MINUTE, 7 * 24 * 60), TimeUnit.MINUTES) // default expire for 7 days
                    .build();
        }

        // 定时执行保存缓存任务
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::saveCacheToFile, 0, SAVE_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public void addReferencedProperties(@NonNull String type, @NonNull String[] propNames) {
        // 获取或创建类型对应的属性LFU缓存
        Cache<String, String> propertyCache = typeToPropertyCache.get(type, k -> createPropertyCache());

        // 将属性放入LFU缓存
        for (String propName : propNames) {
            propertyCache.put(propName, propName);
        }
    }

    private Cache<String, String> createPropertyCache() {
        return Caffeine.newBuilder().maximumSize(Integer.getInteger(RECOMMENDED_PROPERTIES_MAX_PROP_COUNT_PER_TYPE, 30)).build();
    }

    private void saveCacheToFile() {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(CACHE_FILE_PATH))) {
            outputStream.writeObject(typeToPropertyCache);
        } catch (IOException e) {
            log.error("LFUTcObjectPropertyLoadStatService cannot save to disk.", e);
        }
    }

    private Cache<String, Cache<String, String>> loadCacheFromFile() {
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(CACHE_FILE_PATH))) {
            Object obj = inputStream.readObject();
            return (Cache<String, Cache<String, String>>) obj;
        } catch (IOException | ClassNotFoundException e) {
            log.error("LFUTcObjectPropertyLoadStatService cannot load from disk.", e);
        }
        return null;
    }

    @Override
    public String[] getRecommendedLoadPropertyNames(@NonNull ModelObject modelObject) {
        Cache<String, String> usedProperties = typeToPropertyCache.getIfPresent(modelObject.getTypeObject().getName());
        if (usedProperties != null) {
            Map<String, String> props = usedProperties.asMap();
            return props.keySet().toArray(new String[props.size()]);
        }
        if (Boolean.valueOf(System.getProperty(RECOMMENDED_PROPERTIES_ADD_DEFAULT_PROPERTIES, "true"))) {
            return modelObject.getPropertyNames();
        } else {
            return new String[0];
        }
    }

    @Override
    public void cleanUp() {
        if (typeToPropertyCache != null) {
            typeToPropertyCache.invalidateAll();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Teamcenter Object Property Load Stat").append("\n");
        sb.append(typeToPropertyCache.stats()).append("\n");
        sb.append(getCache());
        return sb.toString();
    }

    @Override
    public Map<String, List<String>> getCache() {
        return typeToPropertyCache.asMap().entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().asMap().values().stream().collect(Collectors.toList())));
    }
}

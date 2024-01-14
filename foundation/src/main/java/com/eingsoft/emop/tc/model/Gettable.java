package com.eingsoft.emop.tc.model;

import lombok.NonNull;

import java.util.List;

import com.eingsoft.emop.tc.util.ProxyUtil;

public interface Gettable {
    public static final String METHOD_GET = "get";
    public static final String METHOD_SIMPLE_GET = "simpleGet";
    public static final String METHOD_GETOPTIONAL = "getOptional";
    public static final String METHOD_GETDISPLAYVAL = "getDisplayVal";
    public static final String METHOD_GETDISPLAYVALS = "getDisplayVals";
    public static final String METHOD_REL = "rel";
    public static final String METHOD_REF = "ref";
    public static final String METHOD_GETMODELOBJECT = "getModelObject";

    /**
     * When invoke "get(xxx)" method, detect the result, when it is null or empty collection, return
     * {@link NullObjectGettable} instance
     */
    public static final ThreadLocal<Boolean> SKIP_NULL_OR_EMPTY = new ThreadLocal<Boolean>();

    /**
     * Get property value, please refere to {@link ProxyUtil} for the implementation, if the given propertyName is not a
     * valid Java variable naming standard, it will treat the propertyName as an xpath expression, and try to get the
     * value through xpath.
     */
    Object get(@NonNull String propertyName);
    
    /**
     * See <code>Object get(@NonNull String propertyName)</code> method, when the property is
     * {@link com.teamcenter.soa.client.model.ModelObject} or array of
     * {@link com.teamcenter.soa.client.model.ModelObject}, won't load the properties by default
     */
    Object simpleGet(@NonNull String propertyName);
    
    /**
     * Get property value, and cast the result to given type. refer to {@link #get(String)}
     */
    default <T> T get(@NonNull String propertyName, Class<T> clz) {
        return (T)get(propertyName);
    }
    
    /**
     * Get property value, and cast the result to {@link ModelObject}
     */
    default ModelObject getModelObject(@NonNull String propertyName) {
        return get(propertyName, ModelObject.class);
    }
    
    /**
     * Get property value, return {@link NullGettable} or , please refere to {@link ProxyUtil} for the implementation
     */
    Object getOptional(@NonNull String propertyName);
    
    /**
     * Get property display value
     */
    String getDisplayVal(@NonNull String propertyName);
    
    /**
     * Get property display value list
     */
    List<String> getDisplayVals(@NonNull String propertyName);

    /**
     * Get related objects by relationship name, and filter them by type names
     */
    Object rel(@NonNull String relationshipName, String... typeNames);
    
    /**
     * Get where referenced by relationship name, and filter them by type names
     * relationshipName can be empty
     * typeNames match precisely
     */
    Object ref(String relationshipName, String... typeNames);
}

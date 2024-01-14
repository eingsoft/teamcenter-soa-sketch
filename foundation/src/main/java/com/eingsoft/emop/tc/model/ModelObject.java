package com.eingsoft.emop.tc.model;

import java.util.List;
import java.util.Set;

import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.util.ProxyUtil;

public interface ModelObject extends com.teamcenter.soa.client.model.ModelObject, Gettable {

    public static final String METHOD_GETTCCONTEXTHOLDER = "getTcContextHolder";
    public static final String METHOD_GETPROPERTYOBJECT = "getPropertyObject";
    public static final String METHOD_GETTYPEOBJECT = "getTypeObject";
    public static final String METHOD_GETLOADEDPROPERTYNAMES = "getLoadedPropertyNames";
    public static final String METHOD_HASPROPERTY = "hasProperty";
    public static final String METHOD_GETOBJECTNAME = "getObjectName";
    public static final String METHOD_UNPROXY = "unproxy";
    public static final String METHOD_XPATH = "xpath";
    public static final String METHOD_XPATHVALUES = "xpathValues";

    /**
     * Get {@link TcContextHolder}, please refere to {@link ProxyUtil} for the implementation
     */
    TcContextHolder getTcContextHolder();

    /**
     * Whether the property in {@link ModelObject} is initialized
     */
    boolean hasProperty(String propName);

    /**
     * All the initialized property names
     */
    Set<String> getLoadedPropertyNames();

    /**
     * Get the original {@link com.teamcenter.soa.client.model.ModelObject} instance before proxy
     */
    com.teamcenter.soa.client.model.ModelObject unproxy();

    /**
     * Get the object_name property, usually, it is a lazy-load property
     */
    String getObjectName();
    
    /**
     * Get the property through xpath expression, it returns single value, if the expression evaluated to multiply
     * values, return the first one.
     */
    Object xpath(String expression);

    /**
     * Get the property through xpath expression, it returns all matched values and removed null elements
     */
    List<?> xpathValues(String expression);
}

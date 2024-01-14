package com.eingsoft.emop.tc.util;

import com.eingsoft.emop.tc.model.Gettable;
import com.eingsoft.emop.tc.model.NullObjectGettable;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcObjectPropertyLoadStatService;
import com.eingsoft.emop.tc.transformer.ModelObjectPropValueTransformerChain;
import com.eingsoft.emop.tc.xpath.XpathHelper;
import com.google.common.collect.Lists;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.Property;
import com.teamcenter.soa.client.model.Type;
import com.teamcenter.soa.exceptions.NotLoadedException;
import com.teamcenter.soa.internal.client.model.*;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.Strings;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.eingsoft.emop.tc.BMIDE.PROP_OBJECT_NAME;
import static com.eingsoft.emop.tc.model.Gettable.*;
import static com.eingsoft.emop.tc.model.ModelObject.*;
import static com.eingsoft.emop.tc.util.ReflectionUtil.getFieldValue;
import static com.eingsoft.emop.tc.util.ReflectionUtil.setFieldValue;

/**
 * proxy tc {@link com.teamcenter.soa.client.model.ModelObject} to {@link com.eingsoft.emop.tc.model.ModelObject}
 *
 * the proxied object provides object attribute preload and xpath functionality.
 *
 * @author beam
 *
 */
@Log4j2
public class ProxyUtil {

    // cache ProxyFactory instances
    private static Map<Class<? extends ModelObject>, Proxyer<? extends ModelObject>> proxiers =
        new HashMap<Class<? extends ModelObject>, ProxyUtil.Proxyer<? extends ModelObject>>();

    /**
     * proxy instance of {@link ModelObject}, at the same time will be the instance of
     * {@link com.eingsoft.emop.tc.model.ModelObject}
     * 
     * please use proxy(final T obj, final TcContextHolder tcContextHolder) instead, as the tcContextHolder is necessary
     */
    @Deprecated
    public static <T extends ModelObject> T proxy(final T obj) {
        return proxy(obj, null);
    }

    /**
     * unproxy the given instance if it is an instance of {@link com.eingsoft.emop.tc.model.ModelObject}
     */
    public static <T extends ModelObject> T unproxy(final T obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof com.eingsoft.emop.tc.model.ModelObject) {
            return (T)((com.eingsoft.emop.tc.model.ModelObject)obj).unproxy();
        }
        return obj;
    }

    /**
     * unproxy the given instances if they are instances of {@link com.eingsoft.emop.tc.model.ModelObject}
     */
    public static <T extends ModelObject> List<T> unproxy(final List<T> objs) {
        return objs.stream().map(o -> unproxy(o)).collect(Collectors.toList());
    }

    /**
     * proxy the given instance and return instance of {@link com.eingsoft.emop.tc.model.ModelObject}
     */
    public static com.eingsoft.emop.tc.model.ModelObject spy(final ModelObject obj,
        final TcContextHolder tcContextHolder) {
        return (com.eingsoft.emop.tc.model.ModelObject)proxy(obj, tcContextHolder);
    }

    /**
     * proxy the given instances and return instances of {@link com.eingsoft.emop.tc.model.ModelObject}
     */
    public static List<com.eingsoft.emop.tc.model.ModelObject> spy(final List<? extends ModelObject> objs,
        final TcContextHolder tcContextHolder) {
        return objs.stream().map(obj -> spy(obj, tcContextHolder)).collect(Collectors.toList());
    }

    /**
     * proxy instances of {@link ModelObject}, at the same time will be the instance of
     * {@link com.eingsoft.emop.tc.model.ModelObject}
     */
    public static <T extends ModelObject> List<T> proxy(final List<T> objs, final TcContextHolder tcContextHolder) {
        return objs.stream().map(obj -> proxy(obj, tcContextHolder)).collect(Collectors.toList());
    }

    /**
     * proxy instance of {@link ModelObject}, at the same time will be the instance of
     * {@link com.eingsoft.emop.tc.model.ModelObject}
     */
    public static <T extends ModelObject> T proxy(final T obj, final TcContextHolder tcContextHolder) {
        if (obj == null) {
            return null;
        }
        // don't create proxy again
        if (ProxyFactory.isProxyClass(obj.getClass())) {
            return obj;
        }
        try {
            // make sure the consturctor is present, NoSuchMethodException will be thrown if the constructor is absent
            obj.getClass().getConstructor(Type.class, String.class);
            Proxyer<T> proxyer = getProxier(obj, (Class<T>)obj.getClass());
            //the ProxyFactory is not thread-safe
            synchronized (proxyer.getProxyFactory()) {
              return (T) proxyer.getProxyFactory().create(new Class<?>[] {Type.class, String.class},
                  new Object[] {obj.getTypeObject(), obj.getUid()}, proxyer.getMethodHandler(obj.getTypeObject(), obj, tcContextHolder));
            }
        } catch (Exception e) {
            throw new RuntimeException("failed to create proxy of " + obj.getClass().getName() + " Object "
                + obj.getClass().getName() + " with Uid " + obj.getUid(), e);
        }
    }

    private static <T extends ModelObject> Proxyer<T> getProxier(final T obj, final Class<T> clz) {
        if (!proxiers.containsKey(obj.getClass())) {
            proxiers.put(obj.getClass(), new Proxyer<T>(clz));
        }
        return (Proxyer<T>)proxiers.get(clz);
    }

    static class Proxyer<T extends ModelObject> {

        public static final Pattern MODEL_OBJECT_GET_METHOD_PATTERN = Pattern.compile("^get_(?<prop>[a-zA-Z0-9_]+)$");
        private ProxyFactory factory;
        private Class<T> clz;

        public Proxyer(Class<T> clz) {
            this.clz = clz;
        }
        
        public synchronized ProxyFactory getProxyFactory() {
            if (factory == null) {
                factory = new ProxyFactory() {
                    @Override
                    protected ClassLoader getClassLoader() {
                        // set the correct classloader
                        return com.eingsoft.emop.tc.model.ModelObject.class.getClassLoader();
                    }
                };
                factory.setSuperclass(clz);
                // it will also be the instance of com.eingsoft.emop.tc.model.ModelObject
                factory.setInterfaces(new Class<?>[] {com.eingsoft.emop.tc.model.ModelObject.class});
            }
            return factory;
        }

        public MethodHandler getMethodHandler(final Type type, final T target, final TcContextHolder tcContextHolder) {
            return new MethodHandler() {
                @Override
                public Object invoke(Object self, Method method, Method proceed, Object[] args) throws Throwable {
                    if (isGetPropertyMethod(method, args)) {
                        return handleGetProperty(self, type, target, method, args);
                    } else if (isGetMethod(method, args)) {
                        Object returnVal = handleGet(self, target, args);
                        // null or empty list
                        if (isNullOrEmpty(returnVal) && Gettable.SKIP_NULL_OR_EMPTY.get() != null
                            && Gettable.SKIP_NULL_OR_EMPTY.get()) {
                            return new NullObjectGettable(target.getUid() + "->" + args[0]);
                        } else {
                            return returnVal;
                        }
                    } else if (isSimpleGetMethod(method, args)) {
                        Object returnVal = handleGet(self, target, args, true, false);
                        // null or empty list
                        if (isNullOrEmpty(returnVal) && Gettable.SKIP_NULL_OR_EMPTY.get() != null
                            && Gettable.SKIP_NULL_OR_EMPTY.get()) {
                            return new NullObjectGettable(target.getUid() + "->" + args[0]);
                        } else {
                            return returnVal;
                        }
                    } else if (isGetTcContextHolder(method, args)) {
                        return tcContextHolder;
                    } else if (isGetOptionalMethod(method, args)) {
                        Object returnVal = handleGet(self, target, args);
                        // null or empty list
                        if (isNullOrEmpty(returnVal)) {
                            return new NullObjectGettable(target.getUid() + "->" + args[0]);
                        } else {
                            return returnVal;
                        }
                    } else if (isGetDisplayValMethod(method, args)) {
                        String returnVal = handleGetDisplayVal(self, target, args);
                        if (isNullOrEmpty(returnVal) && Gettable.SKIP_NULL_OR_EMPTY.get() != null
                            && Gettable.SKIP_NULL_OR_EMPTY.get()) {
                            return Strings.EMPTY;
                        } else {
                            return returnVal;
                        }
                    } else if (isGetDisplayValsMethod(method, args)) {
                        List<String> returnVal = handleGetDisplayVals(self, target, args);
                        if (isNullOrEmpty(returnVal) && Gettable.SKIP_NULL_OR_EMPTY.get() != null
                            && Gettable.SKIP_NULL_OR_EMPTY.get()) {
                            return Lists.newArrayList();
                        } else {
                            return returnVal;
                        }
                    } else if (isRel(method, args)) {
                        return handleRelationship(self, target, args);
                    } else if (isRef(method, args)) {
                        return handleWhereReferenced(self, target, args);
                    } else if (isHasProperty(method, args)) {
                        return handleHasProperty(target, args);
                    } else if (isUnproxy(method, args)) {
                        return target;
                    } else if (isGetLoadedPropertyNames(method, args)) {
                        return handleGetLoadedPropertyNames(target, args);
                    } else if (isGetObjectName(method, args)) {
                        return handleGet(self, target, new String[] {PROP_OBJECT_NAME});
                    } else if (isGetWithTypeMethod(method, args)) {
                        return proceed.invoke(self, args);
                    } else if (isGetModelObjectMethod(method, args)) {
                        return proceed.invoke(self, args);
                    } else if (isXpath(method, args)) {
                        return XpathHelper.xpath(self, (String)args[0]);
                    } else if (isXpathValues(method, args)) {
                        return XpathHelper.xpathValues(self, (String)args[0]);
                    }
                    // execute the original method on target object
                    String propName = tryToGetPropertyNameByPattern(method, args);
                    boolean isGetPropertyPattern = propName != null;
                    try {
                        Object o = method.invoke(target, args);
                        // it is a get_xxxx() method
                        if (isGetPropertyPattern) {
                            return ModelObjectPropValueTransformerChain.getInstance().transform(
                                (com.eingsoft.emop.tc.model.ModelObject)self, propName, o);
                        }
                        return o;
                    } catch (Exception e) {
                        if ((e instanceof NotLoadedException || (e instanceof InvocationTargetException && e.getCause() instanceof NotLoadedException))
                            && isGetPropertyPattern) {
                            log.debug(propName + " is not loaded, try to reload it.");
                            return handleGet(self, target, new String[] {propName}, false, true);
                        }
                        throw e;
                    }
                }
                
                private Object handleHasProperty(final T target, final Object[] args) {
                    try {
                        Map<String, Property> props = (Map<String, Property>)getFieldValue(target, "m_props");
                        return props.containsKey(args[0]);
                    } catch (Exception e) {
                        log.warn(e.getMessage(), e);
                        return false;
                    }
                }

                private Object handleGetLoadedPropertyNames(final T target, final Object[] args) {
                    try {
                        return ((Map<String, Property>)getFieldValue(target, "m_props")).keySet();
                    } catch (Exception e) {
                        log.warn(e.getMessage(), e);
                        return Collections.emptySet();
                    }
                }

                private Object handleRelationship(final Object self, final T target, final Object[] args) {
                    Object returnVal = handleGet(self, target, args);
                    if (args.length > 1 && ((String[])args[1]).length >= 1) {
                        List<String> candidates = Arrays.asList((String[])args[1]);
                        List<ModelObject> objs = (List<ModelObject>)returnVal;
                        returnVal =
                            objs.stream().filter(o -> candidates.contains(((ModelObject)o).getTypeObject().getName()))
                                .map(o -> (ModelObject)o).collect(Collectors.toList());
                    }
                    // null or empty list
                    if (isNullOrEmpty(returnVal) && Gettable.SKIP_NULL_OR_EMPTY.get() != null
                        && Gettable.SKIP_NULL_OR_EMPTY.get()) {
                        return new NullObjectGettable(target.getUid() + "->" + args[0]);
                    } else {
                        return returnVal;
                    }
                }
                
                private Object handleWhereReferenced(final Object self, final T target, final Object[] args) {
                    TcContextHolder contextHolder = tryToGetTcContextHolder(self);
                    ModelObject obj = (ModelObject)self;
                    List<ModelObject> objs = Arrays.asList(obj);
                    String relType = (String)args[0];
                    List<String> objTypes = args.length > 1 && ((String[])args[1]).length >= 1
                        ? Arrays.asList((String[])args[1]) : new ArrayList<>();
                    Map<com.eingsoft.emop.tc.model.ModelObject, List<com.eingsoft.emop.tc.model.ModelObject>> objsMap =
                        contextHolder.getTcRelationshipService().findWhereReferenced(objs, relType, objTypes, 1, true);
                    return objsMap.get(obj);
                }

                private boolean isNullOrEmpty(final Object returnVal) {
                    return returnVal == null || (returnVal instanceof List && ((List)returnVal).isEmpty())
                        || (returnVal.getClass().isArray() && ((Object[])returnVal).length == 0);
                }

                private Object handleGet(final Object self, final T target, final Object[] args) {
                	return handleGet(self, target, args, true, true);
                }
                
                private Object handleGet(final Object self, final T target, final Object[] args, final boolean arrayToList, final boolean preloadProperty) {
                    /**
                     * get my property and if my property is a ModelObject or ModelObject array, use the targetStrategy
                     * to load the object of my property
                     */
                    String propName = (String)args[0];
                    if (!XpathHelper.isValidPropName(propName)) {
                        // evaluate it as xpath
                        return XpathHelper.xpath(self, propName);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("getting " + propName + " from " + target.getTypeObject().getName() + " of uid "
                            + target.getUid());
                    }
                    Property prop = getPropertyByName(self, target, propName);

                    if (prop == null) {
                        throw new NullPointerException("can't find property " + propName + " in " + target.getUid());
                    }
                    Object result = null;
                    if (prop instanceof PropertyModelObjectImpl) {
                        result = prop.getModelObjectValue();
                        TcContextHolder holder = tryToGetTcContextHolder(self);
                        if (result != null && holder != null) {
                            result = preloadProperty
                                ? proxy(holder.getTcLoadService().loadProperties(prop.getModelObjectValue()), holder)
                                : proxy(prop.getModelObjectValue(), holder);
                        }
                    } else if (prop instanceof PropertyStringImpl) {
                        result = prop.getStringValue();
                    } else if (prop instanceof PropertyCharImpl) {
                        result = prop.getCharValue();
                    } else if (prop instanceof PropertyDoubleImpl) {
                        result = prop.getDoubleValue();
                    } else if (prop instanceof PropertyFloatImpl) {
                        result = prop.getFloatValueAsDouble();
                    } else if (prop instanceof PropertyIntImpl) {
                        result = prop.getIntValue();
                    } else if (prop instanceof PropertyShortImpl) {
                        result = prop.getShortValue();
                    } else if (prop instanceof PropertyBoolImpl) {
                        result = prop.getBoolValue();
                    } else if (prop instanceof PropertyCalenderImpl) {
                        result = prop.getCalendarValue() == null ? null : prop.getCalendarValue();
                    } else if (prop instanceof PropertyModelObjectArrayImpl) {
                        ModelObjectUtil.removeNullElements(prop);
                        if(preloadProperty) {
                            List<? extends ModelObject> list = tcContextHolder.getTcLoadService()
                                .loadProperties(Arrays.asList(prop.getModelObjectArrayValue()));
                            result = arrayToList ? list : list.toArray(new ModelObject[list.size()]);
                        } else {
                            List<? extends ModelObject> list = Arrays.asList(prop.getModelObjectArrayValue()).stream()
                                .map(o -> proxy(o, tcContextHolder)).collect(Collectors.toList());
                            result = arrayToList ? list : list.toArray(new ModelObject[list.size()]);
                        }
                    } else if (prop instanceof PropertyStringArrayImpl) {
                        result = prop.getStringArrayValue();
                    } else if (prop instanceof PropertyCharArrayImpl) {
                        result = prop.getCharArrayValue();
                    } else if (prop instanceof PropertyDoubleArrayImpl) {
                        result = prop.getDoubleArrayValue();
                    } else if (prop instanceof PropertyFloatArrayImpl) {
                        result = prop.getFloatArrayValueAsDoubles();
                    } else if (prop instanceof PropertyIntArrayImpl) {
                        result = prop.getIntArrayValue();
                    } else if (prop instanceof PropertyShortArrayImpl) {
                        result = prop.getShortArrayValue();
                    } else if (prop instanceof PropertyBoolArrayImpl) {
                        result = prop.getBoolArrayValue();
                    } else if (prop instanceof PropertyCalenderArrayImpl) {
						List<Calendar> list = (prop.getCalendarArrayValue() == null ? Collections.emptyList() : Arrays
								.stream(prop.getCalendarArrayValue()).collect(Collectors.toList()));
						result = arrayToList ? list : list.toArray(new Calendar[list.size()]);
                    } else {
                        throw new RuntimeException("unknown property type " + prop.getClass());
                    }
                    return ModelObjectPropValueTransformerChain.getInstance().transform(
                        (com.eingsoft.emop.tc.model.ModelObject)self, propName, result);
                }

                private String handleGetDisplayVal(final Object self, final T target, final Object[] args) {
                    String propName = (String)args[0];
                    if (log.isDebugEnabled()) {
                        log.debug("getting " + propName + " from " + target.getTypeObject().getName() + " of uid "
                            + target.getUid());
                    }
                    Property prop = getPropertyByName(self, target, propName);

                    if (prop == null) {
                        return null;
                    }

                    return prop.getDisplayableValue();
                }

                private List<String> handleGetDisplayVals(final Object self, final T target, final Object[] args) {
                    String propName = (String)args[0];
                    if (log.isDebugEnabled()) {
                        log.debug("getting " + propName + " from " + target.getTypeObject().getName() + " of uid "
                            + target.getUid());
                    }
                    Property prop = getPropertyByName(self, target, propName);

                    if (prop == null) {
                        return null;
                    }

                    return prop.getDisplayableValues();
                }

                private Property getPropertyByName(Object proxy, ModelObject original, final String propertyName) {
                    Property prop = null;
                    int loadTimes = 0;
                    boolean hadLoadProperty = false;
                    // Make sure only load once property when throws NotLoadedExceptioin
                    do {
                        try {
                            prop = original.getPropertyObject(propertyName);
                            TcObjectPropertyLoadStatService.getInstance().addReferencedProperty(
                                original.getTypeObject().getName(), propertyName);
                        } catch (NotLoadedException e) {
                            // The object donesn't include property name if throws exception
                            // again after load property
                            if (loadTimes == 0) {
                                if (log.isDebugEnabled()) {
                                    log.debug("property " + propertyName + " hasn't been loaded in "
                                        + original.getTypeObject().getName() + " of uid " + original.getUid());
                                }
                                if (proxy instanceof com.eingsoft.emop.tc.model.ModelObject) {
                                    original =
                                        ((com.eingsoft.emop.tc.model.ModelObject)proxy).getTcContextHolder()
                                            .getTcLoadService().loadProperty(original, propertyName);
                                } else {
                                    throw new RuntimeException("cannot load " + propertyName + " from "
                                        + original.getUid());
                                }
                            }
                            loadTimes++;
                            hadLoadProperty = true;
                        }
                    } while (hadLoadProperty && loadTimes <= 1 && prop == null);

                    return prop;
                }

                private Object handleGetProperty(final Object self, final Type type, final T target,
                    final Method method, final Object[] args) throws Exception {
                    String propName = (String)args[0];
                    // add property reference stat.
                    TcObjectPropertyLoadStatService.getInstance().addReferencedProperty(type.getName(), propName);
                    Property prop = (Property)method.invoke(target, args);
                    if (prop instanceof PropertyModelObjectImpl && prop.getModelObjectValue() != null) {
                        setFieldValue(prop, "mValue", proxy(prop.getModelObjectValue(), tryToGetTcContextHolder(self)));
                    } else if (prop instanceof PropertyModelObjectArrayImpl) {
                        ModelObject[] objs =
                            Arrays
                                .stream(prop.getModelObjectArrayValue())
                                .filter(
                                    obj -> {
                                        boolean isNull = obj == null;
                                        if (isNull) {
                                            log.warn("skipped null value in model object array, attr " + propName
                                                + " in " + target.getUid());
                                        }
                                        return !isNull;
                                    }).map(obj -> proxy(obj, tryToGetTcContextHolder(self)))
                                .toArray(ModelObject[]::new);
                        setFieldValue(prop, "mValues", objs);
                    }
                    return method.invoke(target, args);
                }

                private boolean isGetMethod(Method method, Object[] args) {
                    return method.getName().equals(METHOD_GET) && args != null && args.length == 1;
                }
                
                private boolean isSimpleGetMethod(Method method, Object[] args) {
                    return method.getName().equals(METHOD_SIMPLE_GET) && args != null && args.length == 1;
                }
                
                private boolean isGetWithTypeMethod(Method method, Object[] args) {
                    return method.getName().equals(METHOD_GET) && args != null && args.length == 2;
                }
                
                private boolean isGetModelObjectMethod(Method method, Object[] args) {
                    return method.getName().equals(METHOD_GETMODELOBJECT) && args != null && args.length == 1;
                }

                private boolean isGetOptionalMethod(Method method, Object[] args) {
                    return method.getName().equals(METHOD_GETOPTIONAL) && args != null && args.length == 1;
                }

                private boolean isGetDisplayValMethod(Method method, Object[] args) {
                    return method.getName().equals(METHOD_GETDISPLAYVAL) && args != null && args.length == 1;
                }

                private boolean isGetDisplayValsMethod(Method method, Object[] args) {
                    return method.getName().equals(METHOD_GETDISPLAYVALS) && args != null && args.length == 1;
                }

                private boolean isGetPropertyMethod(Method method, Object[] args) {
                    return method.getName().equals(METHOD_GETPROPERTYOBJECT) && args != null && args.length == 1;
                }

                private boolean isGetTcContextHolder(Method method, Object[] args) {
                    return method.getName().equals(METHOD_GETTCCONTEXTHOLDER) && args != null && args.length == 0;
                }

                private boolean isHasProperty(Method method, Object[] args) {
                    return method.getName().equals(METHOD_HASPROPERTY) && args != null && args.length == 1;
                }

                private boolean isRel(Method method, Object[] args) {
                    return method.getName().equals(METHOD_REL) && args != null && args.length >= 1;
                }
                
                private boolean isRef(Method method, Object[] args) {
                    return method.getName().equals(METHOD_REF) && args != null && args.length >= 1;
                }

                private boolean isUnproxy(Method method, Object[] args) {
                    return method.getName().equals(METHOD_UNPROXY) && args != null && args.length == 0;
                }

                private boolean isGetObjectName(Method method, Object[] args) {
                    return method.getName().equals(METHOD_GETOBJECTNAME) && args != null && args.length == 0;
                }
                
                private boolean isXpath(Method method, Object[] args) {
                    return method.getName().equals(METHOD_XPATH) && args != null && args.length == 1;
                }

                private boolean isXpathValues(Method method, Object[] args) {
                    return method.getName().equals(METHOD_XPATHVALUES) && args != null && args.length == 1;
                }

                private boolean isGetLoadedPropertyNames(Method method, Object[] args) {
                    return method.getName().equals(METHOD_GETLOADEDPROPERTYNAMES) && args != null && args.length == 0;
                }

                /**
                 * if it is the pattern of "get_property_name()", trying to load the property_name of the ModelObject.
                 * otherwise, return null
                 */
                public String tryToGetPropertyNameByPattern(Method method, Object[] args) {
                    if (args == null || args.length != 0) {
                        return null;
                    }
                    Matcher matcher = MODEL_OBJECT_GET_METHOD_PATTERN.matcher(method.getName());
                    if (matcher.find()) {
                        return matcher.group("prop");
                    }
                    return null;
                }

                private TcContextHolder tryToGetTcContextHolder(final Object target) {
                    return (target instanceof com.eingsoft.emop.tc.model.ModelObject)
                        ? ((com.eingsoft.emop.tc.model.ModelObject)target).getTcContextHolder() : null;
                }

            };
        }
    }
}

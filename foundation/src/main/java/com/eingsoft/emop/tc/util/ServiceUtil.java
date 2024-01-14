package com.eingsoft.emop.tc.util;

import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.service.TcBOMPrintService;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcService;
import com.teamcenter.soa.client.model.ModelObject;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.eingsoft.emop.tc.util.ProxyUtil.spy;

public class ServiceUtil {

    private static List<String> SKIPPED_METHOD_SIGNATURE_CHECKING = Arrays.asList(TcBOMPrintService.class.getName() + ".pretty");

    public static <Service extends TcService> Service getService(Class<Service> clz, TcContextHolder tcContextHolder) {
        Object service = tcContextHolder.getServices().get(clz.getName());
        if (service == null) {
            service = createService(clz, tcContextHolder);
            tcContextHolder.getServices().put(clz.getName(), service);
        }
        return (Service) service;
    }

    private static ProxyFactory getProxyFactory(Class<?> impClz, Class<?> interfaceClz) {
        ProxyFactory factory = new ProxyFactory() {
            @Override
            protected ClassLoader getClassLoader() {
                // set the correct classloader
                return ServiceUtil.class.getClassLoader();
            }
        };
        factory.setSuperclass(impClz);
        factory.setInterfaces(new Class<?>[]{interfaceClz});
        return factory;
    }

    private synchronized static <Service extends TcService> Service createService(Class<Service> clz, TcContextHolder tcContextHolder) {
        MethodHandler methodHandler = new MethodHandler() {
            @Override
            public Object invoke(Object self, Method method, Method proceed, Object[] args) throws Throwable {
                // do nothing first
                Object result = proceed.invoke(self, args);
                if (result != null) {
                    // try to proxy the result
                    if (result instanceof ModelObject) {
                        return spy((ModelObject) result, tcContextHolder);
                    } else if (result.getClass().isArray() && result instanceof ModelObject[]) {
                        Object[] array = (Object[]) result;
                        for (int i = 0; i < array.length; i++) {
                            if (array[i] != null && array[i] instanceof ModelObject) {
                                array[i] = spy((ModelObject) array[i], tcContextHolder);
                            }
                        }
                        return array;
                    } else if (result instanceof List) {
                        return ((List) result).stream().map(o -> {
                            if (o != null && o instanceof ModelObject) {
                                return spy((ModelObject) o, tcContextHolder);
                            } else {
                                return o;
                            }
                        }).collect(Collectors.toList());
                    }
                }
                return result;
            }
        };
        try {
            Class<Service> implClz = (Class<Service>) Class.forName(getImpClzName(clz));
            if (!implClz.isAnnotationPresent(ScopeDesc.class)) {
                throw new RuntimeException(getImpClzName(clz) + " must be annotated with " + ScopeDesc.class);
            }
            return (Service) getProxyFactory(implClz, clz).create(new Class[]{TcContextHolder.class}, new Object[]{tcContextHolder}, methodHandler);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static <Service extends TcService> String getImpClzName(Class<Service> clz) {
        validateServiceSignature(clz);
        return "com.eingsoft.emop.tc.service.impl." + clz.getSimpleName() + "Impl";
    }

    /**
     * The service interface should follow the standards, to distinguish {@link ModelObject} and
     * {@link com.eingsoft.emop.tc.model.ModelObject} usage:
     *
     *
     * <li>return type should use emop {@link com.eingsoft.emop.tc.model.ModelObject} if it is ModelObject</li>
     *
     * <li>return type of list ModelObject should be List<ModelObject> if it is a list of ModelObject, the ModelObject
     * is the emop ModelObject</li>
     *
     * <li>input type should use TC ModelObject if it is ModelObject</li>
     *
     * <li>input type of list ModelObject should be List<? extend ModelObject> if it is a list of ModelObject, the
     * ModelObject is the TC ModelObject</li>
     *
     * @param clz
     */
    private static <Service extends TcService> void validateServiceSignature(Class<Service> clz) {
        for (Method method : clz.getDeclaredMethods()) {
            if (SKIPPED_METHOD_SIGNATURE_CHECKING.contains(clz.getName() + "." + method.getName())) {
                continue;
            }
            // validate the return type
            validateClz(method.getGenericReturnType(), false, method);
            // validate the input parameters
            for (Type parameterType : method.getGenericParameterTypes()) {
                validateClz(parameterType, true, method);
            }
        }
    }

    private static void validateClz(Type genericType, boolean isInuputParameter, Method method) {
        if (genericType instanceof ParameterizedType) {
            for (Type t : ((ParameterizedType) genericType).getActualTypeArguments()) {
                validateClz(t, isInuputParameter, method);
            }
            return;
        }
        if (!(genericType instanceof Class<?>)) {
            return;
        }
        Class<?> actualClz = (Class<?>) genericType;

        if (isInuputParameter) {
            if (actualClz.equals(com.eingsoft.emop.tc.model.ModelObject.class)) {
                throw new IllegalStateException(method.toString() + " input parameters should use " + ModelObject.class.getName() + ", but not " + com.eingsoft.emop.tc.model.ModelObject.class.getName() + ", please follow the standard.");
            }
        } else {
            if (actualClz.equals(ModelObject.class)) {
                throw new IllegalStateException(method.toString() + " return type should use " + com.eingsoft.emop.tc.model.ModelObject.class.getName() + ", but not " + ModelObject.class.getName() + ", please follow the standard.");
            }
        }
    }

}

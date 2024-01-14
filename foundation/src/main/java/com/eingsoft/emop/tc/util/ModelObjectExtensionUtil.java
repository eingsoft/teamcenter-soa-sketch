package com.eingsoft.emop.tc.util;

import static com.eingsoft.emop.tc.model.ModelObjectExt.METHOD_GETMODELOBJECT;

import java.lang.reflect.Method;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import lombok.NonNull;

import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.model.ModelObjectExt;

/**
 * Add additional behavior to existing {@link ModelObject} instance, it is useful when we are trying to add more
 * functionalities.
 * 
 * see {@link ModelObjectExt} for more detail.
 * 
 * @author beam
 *
 */
public class ModelObjectExtensionUtil {

    public static <T extends ModelObjectExt> T ext(@NonNull ModelObject obj, @NonNull Class<T> extension) {
        // don't create proxy again
        if (obj.getClass().isAssignableFrom(extension) && ProxyFactory.isProxyClass(obj.getClass())) {
            return (T)obj;
        }
        try {
            // make sure the consturctor is present, NoSuchMethodException will be thrown if the constructor is absent
            extension.getConstructor(ModelObject.class);
            return (T)getProxyFactory(extension).create(new Class<?>[] {ModelObject.class}, new Object[] {obj},
                new ExtensionMethodHandler());
        } catch (Exception e) {
            throw new RuntimeException("failed to create ModelObjectExt of " + obj.getClass().getName() + " Object "
                + obj.getClass().getName() + " with Uid " + obj.getUid(), e);
        }
    }

    private static <T extends ModelObjectExt> ProxyFactory getProxyFactory(@NonNull Class<T> extension) {
        ProxyFactory factory = new ProxyFactory() {
            @Override
            protected ClassLoader getClassLoader() {
                // set the correct classloader
                return com.eingsoft.emop.tc.model.ModelObjectExt.class.getClassLoader();
            }
        };
        factory.setSuperclass(extension);
        return factory;
    }

    private static final class ExtensionMethodHandler implements MethodHandler {

        @Override
        public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
            // from target ModelObject or from self/this object
            if (isRetrieveValueFromTargetModelObject(self, thisMethod, proceed, args)) {
                if (METHOD_GETMODELOBJECT.equals(thisMethod.getName()) && args.length == 0) {
                    throw new IllegalStateException(self.getClass().getName()
                        + " should provide the implementation of method " + METHOD_GETMODELOBJECT);
                }
                // retrieve ModelObject instance
                ModelObject modelObject = ((ModelObjectExt)self).getModelObject();
                if (modelObject == null) {
                    throw new NullPointerException("encountered null when invoking " + METHOD_GETMODELOBJECT + " from "
                        + self.getClass().getName());
                }
                return thisMethod.invoke(modelObject, args);
            } else {
                return proceed.invoke(self, args);
            }
        }

        private boolean isRetrieveValueFromTargetModelObject(Object self, Method thisMethod, Method proceed,
            Object[] args) throws NoSuchMethodException {
            /**
             * the method is not override in the abstract class <T extends ModelObjectExt>, just invoke from ModelObject
             */
            if (proceed == null) {
                return true;
            }
            /**
             * try to find the corresponding declared method, as self will always be a proxy, use getSuperclass() to
             * retrieve the real class
             */
            Method method =
                ReflectionUtil.getDeclaredMethod(self.getClass().getSuperclass(), thisMethod.getName(),
                    thisMethod.getParameterTypes());
            /**
             * if it is the declared method in teamcenter related package, it means we didn't override the method, just
             * get the value from target ModelObject
             * 
             */
            return method.getDeclaringClass().getPackage().getName().startsWith("com.teamcenter.soa");
        }
    }
}

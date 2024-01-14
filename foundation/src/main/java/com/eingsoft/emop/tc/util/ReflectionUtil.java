package com.eingsoft.emop.tc.util;

import static com.eingsoft.emop.tc.model.ModelObject.METHOD_GETTYPEOBJECT;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import lombok.NonNull;

import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.Type;

public class ReflectionUtil {

    public static Object getFieldValue(@NonNull Object target, @NonNull String fieldName) throws Exception {
        Field f = getDeclaredField(target.getClass(), fieldName); // NoSuchFieldException
        f.setAccessible(true);
        return f.get(target);
    }

    public static void setFieldValue(@NonNull Object target, @NonNull String fieldName, Object value) throws Exception {
        Field f = getDeclaredField(target.getClass(), fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    public static boolean isJUnitTestContext() {
        try {
            return Class.forName("org.junit.Assert") != null;
        } catch (ClassNotFoundException e) {
            // junit class is not in the context
            return false;
        }
    }

    public static Field getDeclaredField(Class<?> clz, String fieldName) throws NoSuchFieldException {
        try {
            return clz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            // ignore
            if (clz.getSuperclass() != null) {
                return getDeclaredField(clz.getSuperclass(), fieldName);
            } else {
                throw e;
            }
        }
    }

    public static Method getDeclaredMethod(Class<?> clz, String methodName, Class<?>... parameterTypes)
        throws NoSuchMethodException {
        try {
            return clz.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            // ignore
            if (clz.getSuperclass() != null) {
                return getDeclaredMethod(clz.getSuperclass(), methodName, parameterTypes);
            } else {
                throw e;
            }
        }
    }

    /**
     * different SOA version has different API.
     * 
     * 8.3 it is modelObject.getType()
     * 
     * 11.3 it is modelObject.getTypeObject();
     */
    public static Type getTypeFromModelObject(ModelObject obj) {
        try {
            return (Type)obj.getClass().getMethod(METHOD_GETTYPEOBJECT).invoke(obj);
        } catch (Exception e) {
            throw new RuntimeException("cannot get type from ModelObject", e);
        }
    }
}

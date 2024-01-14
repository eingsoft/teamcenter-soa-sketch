package com.eingsoft.emop.tc.util;

import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;

import com.teamcenter.soa.client.model.ModelObject;

public class ReflectionUtilTest {

    private static class A {
        private String a;

        public A(String a) {
            this.a = a;
        }

        public String getA() {
            return a;
        }
    }

    private static class B extends A {
        public B(String a) {
            super(a);
        }
    }

    private static class C extends B {
        public C(String a) {
            super(a);
        }

        public String getA() {
            return super.getA() + "'";
        }
    }

    @Test(expected = NoSuchMethodException.class)
    public void testGetDeclaredMethod() throws NoSuchMethodException {
        ReflectionUtil.getDeclaredMethod(Object.class, "getA", new Class<?>[] {});
    }
    
    @Test
    public void testGetDeclaredMethod2() throws NoSuchMethodException {
        Method m = ReflectionUtil.getDeclaredMethod(B.class, "getA", new Class<?>[] {});
        Assert.assertNotNull(m);
        Assert.assertEquals(A.class, m.getDeclaringClass());
        
        m = ReflectionUtil.getDeclaredMethod(C.class, "getA", new Class<?>[] {});
        Assert.assertNotNull(m);
        Assert.assertEquals(C.class, m.getDeclaringClass());
    }

    @Test
    public void testGetField() throws Exception {
        A a = new A("hello");
        Assert.assertEquals("hello", ReflectionUtil.getFieldValue(a, "a"));

        B b = new B("hello");
        Assert.assertEquals("hello", ReflectionUtil.getFieldValue(b, "a"));
        Assert.assertEquals("hello", b.getA());
    }

    @Test
    public void testSetField() throws Exception {
        A a = new A("hello");
        ReflectionUtil.setFieldValue(a, "a", "world");
        Assert.assertEquals("world", a.a);

        B b = new B("hello");
        ReflectionUtil.setFieldValue(b, "a", "world");
        Assert.assertEquals("world", ReflectionUtil.getFieldValue(b, "a"));
        Assert.assertEquals("world", b.getA());
    }

    @Test
    public void testIsJUnitTestContext() throws Exception {
        Assert.assertTrue(ReflectionUtil.isJUnitTestContext());
    }

    @Test
    public void testGetTypeFromModelObject() {
        ModelObject obj = MockDataUtil.createModelObject("ItemRevision", "uid");
        Assert.assertEquals("ItemRevision", ReflectionUtil.getTypeFromModelObject(obj).getName());
        Assert
            .assertEquals("ItemRevision", ReflectionUtil.getTypeFromModelObject(ProxyUtil.proxy(obj, null)).getName());
    }
}

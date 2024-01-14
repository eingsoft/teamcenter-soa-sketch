package com.eingsoft.emop.tc.model;

import org.junit.Assert;
import org.junit.Test;

public class NullObjectGettableTest {

    @Test
    public void testGetByString() {
        NullObjectGettable nog = new NullObjectGettable("info");
        Assert.assertNull(nog.get("prop"));

        NullObjectGettable.SKIP_NULL_OR_EMPTY.set(false);
        try {
            Assert.assertNull(nog.get("prop"));
        } finally {
            NullObjectGettable.SKIP_NULL_OR_EMPTY.remove();
        }

        NullObjectGettable.SKIP_NULL_OR_EMPTY.set(true);
        try {
            Object o = nog.get("prop");
            Assert.assertNotNull(o);
            Assert.assertTrue(o instanceof NullObjectGettable);
            Assert.assertEquals("", o.toString());
        } finally {
            NullObjectGettable.SKIP_NULL_OR_EMPTY.remove();
        }
    }

    @Test
    public void testGetByIndex() {
        NullObjectGettable nog = new NullObjectGettable("info");
        Object o = nog.get(0);
        Assert.assertNotNull(o);
        Assert.assertTrue(o instanceof NullObjectGettable);
        Assert.assertEquals("", o.toString());
    }

    @Test
    public void testGetOptional() {
        NullObjectGettable nog = new NullObjectGettable("info");
        Object o = nog.getOptional("prop");
        Assert.assertNotNull(o);
        Assert.assertTrue(o instanceof NullObjectGettable);
        Assert.assertEquals("", o.toString());
    }
}

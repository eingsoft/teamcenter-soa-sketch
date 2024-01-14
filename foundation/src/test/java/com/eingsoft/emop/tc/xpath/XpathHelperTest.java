package com.eingsoft.emop.tc.xpath;

import org.junit.Assert;
import org.junit.Test;

public class XpathHelperTest {

    @Test
    public void testIsValidPropName(){
       Assert.assertTrue(XpathHelper.isValidPropName("a"));
       Assert.assertTrue(XpathHelper.isValidPropName("a_"));
       Assert.assertTrue(XpathHelper.isValidPropName("_a"));
       Assert.assertTrue(XpathHelper.isValidPropName("a1"));
       Assert.assertTrue(XpathHelper.isValidPropName("a1_"));
       Assert.assertTrue(XpathHelper.isValidPropName("_1a"));
       Assert.assertTrue(XpathHelper.isValidPropName("A_1a"));
       
       Assert.assertFalse(XpathHelper.isValidPropName("1a"));
       Assert.assertFalse(XpathHelper.isValidPropName("//a"));
       Assert.assertFalse(XpathHelper.isValidPropName("a/"));
       Assert.assertFalse(XpathHelper.isValidPropName("a[]"));
       Assert.assertFalse(XpathHelper.isValidPropName("a/b"));
       Assert.assertFalse(XpathHelper.isValidPropName("/ab1/cd/e"));
    }
}

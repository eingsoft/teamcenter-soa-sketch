package com.eingsoft.emop.tc.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.eingsoft.emop.tc.util.MockDataUtil;

public class CredentialTcContextHolderTest {

    @Before
    public void setup() {
        MockDataUtil.initConnectionBuilderInfo();
    }

    @Test
    public void testEqualsAndHashcode() {
        CredentialTcContextHolder context1 = new CredentialTcContextHolder("username", "password");
        CredentialTcContextHolder context2 = new CredentialTcContextHolder("username", "password");
        CredentialTcContextHolder context3 = new CredentialTcContextHolder("username", "password1");

        Assert.assertEquals(context1, context2);
        Assert.assertNotEquals(context1, context3);
    }
}

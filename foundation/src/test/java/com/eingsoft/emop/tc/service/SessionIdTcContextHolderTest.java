package com.eingsoft.emop.tc.service;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.eingsoft.emop.tc.service.impl.SessionIdTcContextHolder;
import com.eingsoft.emop.tc.util.MockDataUtil;

public class SessionIdTcContextHolderTest {
    @Before
    public void setup() {
        MockDataUtil.initConnectionBuilderInfo();
    }

    @Test
    public void testEqualsAndHashcode() {
        SessionIdTcContextHolder context1 = new SessionIdTcContextHolder("sessionId");
        SessionIdTcContextHolder context2 = new SessionIdTcContextHolder("sessionId");
        SessionIdTcContextHolder context3 = new SessionIdTcContextHolder(UUID.randomUUID().toString());

        Assert.assertEquals(context1, context2);
        Assert.assertNotEquals(context1, context3);
    }
}

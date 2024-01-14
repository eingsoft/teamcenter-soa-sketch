package com.eingsoft.emop.tc.service.impl;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.eingsoft.emop.tc.util.MockDataUtil;
import com.teamcenter.soa.client.Connection;

public class SessionIdTcContextHolderTest {
    @Before
    public void setup() {
        MockDataUtil.initConnectionBuilderInfo();
    }

    @Test
    public void testConnectionCacheable() {
        String sessionId = UUID.randomUUID().toString();
        Connection conn1 = new SessionIdTcContextHolder(sessionId).getConnection();
        Connection conn2 = new SessionIdTcContextHolder(sessionId).getConnection();
        Assert.assertSame(conn1, conn2);

        conn2 = new SessionIdTcContextHolder(UUID.randomUUID().toString()).getConnection();
        Assert.assertNotEquals(conn1, conn2);
    }
}

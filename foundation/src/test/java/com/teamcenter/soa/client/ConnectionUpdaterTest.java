package com.teamcenter.soa.client;

import static com.eingsoft.emop.tc.util.MockDataUtil.initConnectionBuilderInfo;
import static com.eingsoft.emop.tc.util.ReflectionUtil.getFieldValue;

import java.util.UUID;

import javassist.util.proxy.ProxyFactory;

import org.junit.Assert;
import org.junit.Test;

import com.eingsoft.emop.tc.service.impl.SessionIdTcContextHolder;
import com.teamcenter.soa.internal.client.EmopXmlRestSender;
import com.teamcenter.soa.internal.client.SessionManager;
import com.teamcenter.soa.internal.client.XmlRestSender;

public class ConnectionUpdaterTest {

    @Test
    public void testUpdateCachePolicy() throws Exception {
        initConnectionBuilderInfo();
        Connection conn = new SessionIdTcContextHolder(UUID.randomUUID().toString()).getConnection();
        Assert.assertEquals(SessionManager.class, conn.sessionManager.getClass());
        Assert.assertEquals(EmopXmlRestSender.class, conn.m_senderInternal.getClass());
        Assert.assertEquals("Default",
            getFieldValue(getFieldValue(conn.sessionManager, "mPolicyManager"), "mCurrentPolicy"));
        new ConnectionUpdater(conn).update("cachPolicyName").updateSender();
        Assert.assertNotEquals(SessionManager.class, conn.sessionManager.getClass());
        Assert.assertTrue(ProxyFactory.isProxyClass(conn.sessionManager.getClass()));
        Assert.assertEquals(EmopXmlRestSender.class, conn.m_senderInternal.getClass());
    }
}

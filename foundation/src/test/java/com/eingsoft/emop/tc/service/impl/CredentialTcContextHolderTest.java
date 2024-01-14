package com.eingsoft.emop.tc.service.impl;

import com.eingsoft.emop.tc.service.CredentialTcContextHolderAware;
import com.eingsoft.emop.tc.util.MockDataUtil;
import com.teamcenter.soa.client.Connection;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static com.eingsoft.emop.tc.connection.ConnectionBuilder.SKIP_INITIAL_TC_CREDENTIAL_LOGIN;

public class CredentialTcContextHolderTest implements CredentialTcContextHolderAware {
    @Before
    public void setup() {
        MockDataUtil.initConnectionBuilderInfo();
        System.setProperty(SKIP_INITIAL_TC_CREDENTIAL_LOGIN, "true");
    }

    @After
    public void teardown() {
        System.clearProperty(SKIP_INITIAL_TC_CREDENTIAL_LOGIN);
    }

    @Test
    public void testEphemeralCredentialContextHolder() {
        Connection conn1 = createCredentialContextHolder("username", "password").getConnection();
        Connection conn2 = createCredentialContextHolder("username", "password").getConnection();
        Assert.assertNotEquals(conn1, conn2);

        conn2 = createCredentialContextHolder("username", "password").getConnection();
        Assert.assertNotEquals(conn1, conn2);
    }
}

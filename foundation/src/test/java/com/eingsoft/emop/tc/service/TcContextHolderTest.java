package com.eingsoft.emop.tc.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.service.impl.SessionIdTcContextHolder;
import com.eingsoft.emop.tc.util.MockDataUtil;

public class TcContextHolderTest {

    @Before
    public void setup() {
        MockDataUtil.initConnectionBuilderInfo();
    }

    @Test
    public void testCredentialContextHolderHasHigherPriority() {
        Assert
            .assertTrue(new CredentialTcContextHolder("username", "password").getPriority() > new SessionIdTcContextHolder(
                "sessionId").getPriority());
    }

    @Test
    public void testCanBeUpdated() {
        try {
            TcContextHolder sessionIdContextHolder = new SessionIdTcContextHolder("sessionId");
            SOAExecutionContext.current().init(sessionIdContextHolder);
            Assert.assertEquals(sessionIdContextHolder, SOAExecutionContext.current().getTcContextHolder());

            TcContextHolder credentialContextHolder = new CredentialTcContextHolder("username", "password");
            SOAExecutionContext.current().init(credentialContextHolder);
            Assert.assertEquals(credentialContextHolder, SOAExecutionContext.current().getTcContextHolder());
        } finally {
            SOAExecutionContext.current().cleanupSiliently();
        }
    }

    @Test
    public void testCanBeUpdated2() {
        try {
            TcContextHolder sessionIdContextHolder = new SessionIdTcContextHolder("sessionId");
            SOAExecutionContext.current().init(sessionIdContextHolder);
            Assert.assertEquals(sessionIdContextHolder, SOAExecutionContext.current().getTcContextHolder());

            TcContextHolder sessionIdContextHolder2 = new SessionIdTcContextHolder("sessionId");
            SOAExecutionContext.current().init(sessionIdContextHolder2);
            Assert.assertSame(sessionIdContextHolder2, SOAExecutionContext.current().getTcContextHolder());
        } finally {
            SOAExecutionContext.current().cleanupSiliently();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotBeUpdated() {
        try {
            TcContextHolder credentialContextHolder = new CredentialTcContextHolder("username", "password");
            SOAExecutionContext.current().init(credentialContextHolder);
            Assert.assertEquals(credentialContextHolder, SOAExecutionContext.current().getTcContextHolder());

            TcContextHolder sessionIdContextHolder = new SessionIdTcContextHolder("sessionId");
            SOAExecutionContext.current().init(sessionIdContextHolder);
            Assert.assertEquals(sessionIdContextHolder, SOAExecutionContext.current().getTcContextHolder());
        } finally {
            SOAExecutionContext.current().cleanupSiliently();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotBeUpdated2() {
        try {
            TcContextHolder credentialContextHolder = new CredentialTcContextHolder("username", "password");
            SOAExecutionContext.current().init(credentialContextHolder);
            Assert.assertEquals(credentialContextHolder, SOAExecutionContext.current().getTcContextHolder());

            TcContextHolder credentialContextHolder2 = new CredentialTcContextHolder("username", "password2");
            SOAExecutionContext.current().init(credentialContextHolder2);
        } finally {
            SOAExecutionContext.current().cleanupSiliently();
        }
    }
    
    @Test
    public void testTcServicesAreTcContextHolderLevel(){
    	TcContextHolder credentialContextHolder = new CredentialTcContextHolder("username", "password");
    	Assert.assertSame(credentialContextHolder.getTcBOMService(), credentialContextHolder.getTcBOMService());
    	
    	TcContextHolder sessionIdContextHolder = new SessionIdTcContextHolder("sessionId");
    	Assert.assertSame(sessionIdContextHolder.getTcClassificationService(), sessionIdContextHolder.getTcClassificationService());
    	
    	Assert.assertNotEquals(credentialContextHolder.getTcDataManagementService(), sessionIdContextHolder.getTcDataManagementService());
    }
}

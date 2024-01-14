package com.eingsoft.emop.tc.connection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.eingsoft.emop.tc.util.MockDataUtil;

public class ConnectionBuilderFactoryTest {

    @Before
    public void setup() {
        MockDataUtil.initConnectionBuilderInfo();
    }

    @Test
    public void testCheckSystemProperty() {
        String appName = System.getProperty("tc.appName");
        System.setProperty("tc.appName", "");
        try {
            ConnectionBuilderFactory.createConnectionBuilder();
            throw new RuntimeException("Not reachable");
        } catch (IllegalStateException e) {
            Assert.assertEquals(
                "system property tc.appName is empty, please specify it in application.yml or JVM argument.",
                e.getMessage());
        }
        System.setProperty("tc.appName", appName);
    }

    @Test
    public void testConnectionBuilderShouldnotSingleton() {
        ConnectionBuilder builder1 = ConnectionBuilderFactory.createConnectionBuilder();
        ConnectionBuilder builder2 = ConnectionBuilderFactory.createConnectionBuilder();
        Assert.assertNotNull(builder1);
        Assert.assertNotNull(builder2);
        Assert.assertNotEquals(builder1, builder2);
    }
}

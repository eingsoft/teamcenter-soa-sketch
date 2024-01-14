package com.eingsoft.emop.tc.connection;

import static com.eingsoft.emop.tc.propertyresolver.PropertyResolver.getPropertyResolver;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * create an instance of {@link ConnectionBuilder}, it will get all the necessary information about tc connection from
 * system property. For a stand alone java application, just simply set the system property through JVM startup command
 * of "-Dxxx=xxxx", for a spring boot application, put the necessary configuration into yml configuration will be a
 * better choice.
 * 
 * @author beam
 *
 */
public final class ConnectionBuilderFactory {

    private static final String PROTOCOL = "tc.protocol";
    private static final String HOST = "tc.host";
    private static final String PORT = "tc.port";
    private static final String APP = "tc.appName";
    public static final String USERNAME = "tc.username";
    public static final String PASSWORD = "tc.password";
    public static final String POOLED = "tc.pooled";
    public static final String SOCKET_TIMEOUT = "tc.soTimeout";
    public static final String CONNECTION_TIMEOUT = "tc.connTimeout";
    public static final String PROXY_HOST = "tc.proxyHost";
    public static final String PROXY_PORT = "tc.proxyPort";

    public static void setProperties(String host, int port) {
        getPropertyResolver().setProperty(PROTOCOL, "http");
        getPropertyResolver().setProperty(HOST, host);
        getPropertyResolver().setProperty(PORT, String.valueOf(port));
        getPropertyResolver().setProperty(APP, "tc");
    }

    public static ConnectionBuilder createConnectionBuilder() {
        String protocol = getMandatoryValueFromSystemProperty(PROTOCOL);
        String host = getMandatoryValueFromSystemProperty(HOST);
        String port = getMandatoryValueFromSystemProperty(PORT);
        String appName = getMandatoryValueFromSystemProperty(APP);
        return new ConnectionBuilder(protocol, host, Integer.valueOf(port), appName);
    }

    private static String getMandatoryValueFromSystemProperty(String propName) {
        String propVal = getPropertyResolver().getProperty(propName);
        if (isEmpty(propVal)) {
            throw new IllegalStateException("system property " + propName
                + " is empty, please specify it in application.yml or JVM argument.");
        }
        return propVal;
    }
}

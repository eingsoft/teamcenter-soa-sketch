package com.eingsoft.emop.tc.connection;

import com.eingsoft.emop.tc.util.ReflectionUtil;
import com.teamcenter.soa.client.Connection;
import com.teamcenter.soa.internal.client.HttpTransport;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.cookie.BasicClientCookie2;

public class TC12CookieHandler implements ICookieHandler {
    @Override
    public void addCookie(Connection connection, String appName, String domain, String cookieName, String cookieValue) throws Exception {
        BasicClientCookie2 cookie = new BasicClientCookie2(cookieName, cookieValue);
        //pay attention to the cookieSpec
        cookie.setDomain(domain);
        cookie.setPath("/" + appName);
        HttpClientContext cookieManager = getCookieManager(connection);
        cookieManager.getCookieStore().addCookie(cookie);
    }

    private static HttpClientContext getCookieManager(Connection connection) throws Exception {
        HttpTransport httpTransport = (HttpTransport) ReflectionUtil.getFieldValue(connection, "m_transport");
        HttpClientContext cookieManager = (HttpClientContext) ReflectionUtil.getFieldValue(httpTransport, "httpState");
        return cookieManager;
    }

    @Override
    public boolean hasInitialized(Connection conn) {
        HttpClientContext cookieManager = null;
        try {
            cookieManager = getCookieManager(conn);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cookieManager.getCookieStore().getCookies().size() > 0;
    }
}

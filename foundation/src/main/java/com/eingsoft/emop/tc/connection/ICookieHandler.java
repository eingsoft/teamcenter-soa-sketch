package com.eingsoft.emop.tc.connection;

import com.teamcenter.soa.client.Connection;

public interface ICookieHandler {

    void addCookie(Connection connection, String appName, String domain, String cookieName, String cookieValue) throws Exception;

    boolean hasInitialized(Connection conn);
}

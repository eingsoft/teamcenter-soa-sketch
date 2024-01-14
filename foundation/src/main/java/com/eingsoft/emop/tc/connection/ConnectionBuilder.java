package com.eingsoft.emop.tc.connection;

import com.eingsoft.emop.tc.propertyresolver.PropertyResolver;
import com.eingsoft.emop.tc.service.CredentialTcContextHolder.SimpleCredentialManager;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.teamcenter.schemas.soa._2006_03.exceptions.InvalidCredentialsException;
import com.teamcenter.schemas.soa._2006_03.exceptions.InvalidUserException;
import com.teamcenter.services.strong.core.SessionService;
import com.teamcenter.soa.client.Connection;
import com.teamcenter.soa.client.ConnectionUpdater;
import com.teamcenter.soa.client.CredentialManager;
import com.teamcenter.soa.exceptions.CanceledOperationException;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.util.ServiceLoader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.eingsoft.emop.tc.connection.ConnectionBuilderFactory.*;

/**
 * A builder to build connection, for the same connection (both tc server information and session id / credential
 * manager are the same), it will share same connection instance.
 *
 * Pay attention, the connection builder shouldn't be singleton, and don't create the instance manually, please use
 * {@link ConnectionBuilderFactory} to create the instance.
 *
 * @author beam
 *
 */
@Log4j2
public final class ConnectionBuilder {

    public static final String SKIP_INITIAL_TC_CREDENTIAL_LOGIN = "SKIP_INITIAL_TC_CREDENTIAL_LOGIN";

    private Connection connection;

    @Getter
    private String hostName;
    @Getter
    private int port;
    @Getter
    private String appName;
    @Getter
    private String appPath;

    private static Cache<String, Connection> connectionInstances = CacheBuilder.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS).maximumSize(1000).build();

    ConnectionBuilder(final String protocol, final String hostName, final int port, final String appName) {
        this.hostName = hostName;
        this.port = port;
        this.appName = appName;

        appPath = toPath(protocol, hostName, port, appName);
        connection = new Connection(appPath, new NotSupportedCredentialManager(), "REST", protocol.toUpperCase());
        log.info("Init ConnectionBuilder with params[protocol={}, host={}, port={}, appName={}]", protocol, hostName,
            port, appName);
    }

    ConnectionBuilder(String hostName, int port) {
        this("http", hostName, port, "tc");
    }

    private String toPath(String protocol, String hostName, int port, String appName) {
        return protocol + "://" + hostName + ":" + port + "/" + appName;
    }

    private void addCookie(String name, String value) throws Exception {
        getCookieHandler().addCookie(connection, appName, hostName, name, value);
    }

    private static ICookieHandler getCookieHandler() {
        ServiceLoader<ICookieHandler> serviceLoader = ServiceLoader.load(ICookieHandler.class);
        for (ICookieHandler implementation : serviceLoader) {
            return implementation;
        }
        throw new RuntimeException("please define ICookieHandler in META-INF");
    }

    private ConnectionBuilder addProxy() {
        String proxyHost = PropertyResolver.getPropertyResolver().getProperty(PROXY_HOST);
        if (StringUtils.isNotEmpty(proxyHost)) {
            connection.setOption(Connection.HTTP_PROXY_HOST, String.valueOf(proxyHost));
            log.info("updated proxy host to {}", proxyHost);
        }
        String proxyPort = PropertyResolver.getPropertyResolver().getProperty(PROXY_PORT);
        if (StringUtils.isNotEmpty(proxyPort)) {
            connection.setOption(Connection.HTTP_PROXY_PORT, String.valueOf(proxyPort));
            log.info("updated proxy port to {}", proxyPort);
        }
        return this;
    }

    private ConnectionBuilder setTimeout() {
        String soTimeout = PropertyResolver.getPropertyResolver().getProperty(SOCKET_TIMEOUT);
        if (StringUtils.isNotEmpty(soTimeout)) {
            connection.setOption(Connection.OPT_SERVER_TIMEOUT, soTimeout);
            log.info("updated soTimeout to {}", soTimeout);
        }
        String connTimeout = PropertyResolver.getPropertyResolver().getProperty(CONNECTION_TIMEOUT);
        if (StringUtils.isNotEmpty(connTimeout)) {
            connection.setOption(Connection.OPT_CONNECTION_TIMEOUT, connTimeout);
            log.info("updated connTimeout to {}", connTimeout);
        }
        return this;
    }

    private void build() throws Exception {
        Connection.addRequestListener(EmopRequestListener.getInstance());
        connection.getModelManager().addPartialErrorListener(new EmopPartialErrorListener());
        addProxy();
        setTimeout();
        new ConnectionUpdater(connection).updateSender();
    }

    public ConnectionBuilder notCompress() {
        connection.setOption(Connection.OPT_USE_COMPRESSION, String.valueOf(false));
        return this;
    }

    public Connection build(@NonNull final String sessionId) {
        String key = String.join("-", appPath, sessionId);
        try {
            // it is very important to cache the connection instance, otherwise, tcp connection won't be reused.
            return connectionInstances.get(key, () -> {
                build();
                addCookie("JSESSIONID", sessionId);
                return connection;
            });
        } catch (ExecutionException e) {
            log.error("cannot obtain connection instance through session id", e);
            throw new RuntimeException(e);
        }
    }

    public Connection build(@NonNull final SimpleCredentialManager credentialManager) {
        String key = String.join("-", appPath, credentialManager.toString());
        try {
            // it is very important to cache the connection instance, otherwise, tcp connection won't be reused.
            return connectionInstances.get(key, () -> {
                if (!Boolean.getBoolean(SKIP_INITIAL_TC_CREDENTIAL_LOGIN)) {
                    SessionService.getService(connection).login(credentialManager.toCredentials());
                }
                build();
                connection.setCredentialManager(credentialManager);
                return connection;
            });
        } catch (Exception e) {
            log.error("cannot obtain connection instance through credential manager", e);
            throw new RuntimeException(e);
        }
    }

    public static void removeConnectionCache(String identifier) {
        connectionInstances.invalidate(identifier);
    }

    public static void destoryAllConnections() {
        for (Connection conn : connectionInstances.asMap().values()) {
            if (!getCookieHandler().hasInitialized(conn)) {
                continue;
            }
            try {
                SessionService.getService(conn).logout();
                log.info("successfully logout session {}", conn);
            } catch (Exception e) {
                log.debug("fail to logout from tc, ignore exception.", e);
            }
        }
    }

    @Override
    public String toString() {
        return appPath;
    }

    public class NotSupportedCredentialManager implements CredentialManager {

        private static final String SESSION_EXPIRE_ERROR = "Session Expired, please relogin.";

        @Override
        public int getCredentialType() {
            return CredentialManager.CLIENT_CREDENTIAL_TYPE_STD;
        }

        @Override
        public String[] getCredentials(InvalidCredentialsException arg0) throws CanceledOperationException {
            // TODO: consider to get use the stored information
            throw new CanceledOperationException(SESSION_EXPIRE_ERROR);
        }

        @Override
        public String[] getCredentials(InvalidUserException arg0) throws CanceledOperationException {
            // TODO: consider to get use the stored information
            throw new CanceledOperationException(SESSION_EXPIRE_ERROR);
        }

        @Override
        public void setUserPassword(String user, String password, String discriminator) {}

        @Override
        public void setGroupRole(String group, String role) {}

    }
}

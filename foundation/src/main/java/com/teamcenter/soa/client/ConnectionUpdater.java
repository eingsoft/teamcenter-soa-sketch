package com.teamcenter.soa.client;

import com.teamcenter.soa.internal.client.*;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Method;

import static com.eingsoft.emop.tc.util.ReflectionUtil.getFieldValue;
import static com.eingsoft.emop.tc.util.ReflectionUtil.setFieldValue;

/**
 * 
 * @author beam
 * 
 *         update the cache policy of the given connection
 *
 */
@Log4j2
public class ConnectionUpdater {

    private final Connection conn;

    private boolean isCachePolicyUpdated;
    private String cachPolicyName;

    public ConnectionUpdater(@NonNull Connection conn) {
        this.conn = conn;
    }

    public ConnectionUpdater updateSender() {
        Sender sender = conn.m_senderInternal;
        if (sender instanceof XmlRestSender && !(sender.getClass().equals(EmopXmlRestSender.class))) {
            conn.m_senderInternal = new EmopXmlRestSender((XmlRestSender)sender, conn.sessionManager);
            log.info("updated tc sender implementation to " + EmopXmlRestSender.class.getName());
        }
        return this;
    }

    public ConnectionUpdater update(final String cachPolicyName) throws Exception {
        isCachePolicyUpdated = false;
        this.cachPolicyName = cachPolicyName;
        if (!ProxyFactory.isProxyClass(conn.sessionManager.getClass())) {
            ProxyFactory factory = new ProxyFactory() {
                @Override
                protected ClassLoader getClassLoader() {
                    // set the correct classloader
                    return SessionManager.class.getClassLoader();
                }
            };
            factory.setSuperclass(SessionManager.class);
            try {
                SessionManager sm =
                    (SessionManager)factory.create(
                        new Class<?>[] {CredentialManager.class, Connection.class},
                        new Object[] {getFieldValue(conn.sessionManager, "cm"),
                            getFieldValue(conn.sessionManager, "connection")}, new MethodHandler() {
                            @Override
                            public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args)
                                throws Throwable {
                                SessionManager target = conn.sessionManager;
                                Object obj = proceed.invoke(conn.sessionManager, args);
                                if ("cacheStateInformation".equals(thisMethod.getName())
                                    && "getTCSessionInfo".equals(args[1])) {
                                    updateCachePolicy(target);
                                }
                                return obj;
                            }
                        });
                conn.sessionManager = sm;
                log.info("Updated session manager.");
            } catch (Exception e) {
                throw new RuntimeException("failed to create proxy of " + conn.sessionManager.getClass().getName()
                    + " Object.", e);
            }
        }
        return this;
    }

    private void updateCachePolicy(SessionManager target) throws Exception {
        /**
         * if the cache policy is already updated, no need update the tcServerId again, which means, the later on
         * refresh/reassign server id will take effect.
         */
        if (isCachePolicyUpdated) {
            return;
        }
        isCachePolicyUpdated = true;

        PolicyManager pm = (PolicyManager)getFieldValue(target, "mPolicyManager");
        setFieldValue(pm, "mCurrentPolicy", cachPolicyName);
        log.info("Updated cache policy tc request header to " + cachPolicyName);
    }
}

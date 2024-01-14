package com.eingsoft.emop.tc.service;

import static com.eingsoft.emop.tc.connection.ConnectionBuilderFactory.createConnectionBuilder;
import static com.eingsoft.emop.tc.propertyresolver.PropertyResolver.getPropertyResolver;
import java.util.Arrays;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.StringUtils;

import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.TcOperationThroughCredential;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;
import com.eingsoft.emop.tc.connection.ConnectionBuilder;
import com.teamcenter.schemas.soa._2006_03.exceptions.InvalidCredentialsException;
import com.teamcenter.schemas.soa._2006_03.exceptions.InvalidUserException;
import com.teamcenter.services.strong.core._2011_06.Session.Credentials;
import com.teamcenter.soa.client.Connection;
import com.teamcenter.soa.client.CredentialManager;
import com.teamcenter.soa.exceptions.CanceledOperationException;

/**
 * connect to Teamcenter through a credential, the connection information is stored in {@link ConnectionBuilder}
 * 
 * user cannot create instance directly, use {@link TcOperationThroughCredential} annotation or method in {@link TcContextHolderAware}
 * 
 * @author beam
 *
 */
@Log4j2
@EqualsAndHashCode(callSuper = false)
@ScopeDesc(Scope.Request)
public class CredentialTcContextHolder extends TcContextHolder {

    private final SimpleCredentialManager credentialManager;
    @Getter
    private boolean isSessionCreated;

    protected CredentialTcContextHolder(@NonNull String username, @NonNull String password, String group, String locale,
        boolean isEphemeral) {
        super(createConnectionBuilder());
        credentialManager = new SimpleCredentialManager(username, password, group, locale, isEphemeral);

        log.debug("Init " + CredentialTcContextHolder.class.getSimpleName()
            + " with params[tcServer={},username={},password={}]", connectionBuilder.toString(), username,
            toPassword(password.length()));
    }

    protected CredentialTcContextHolder(@NonNull String username, @NonNull String password) {
      this(username, password, "",
          getPropertyResolver().getProperty("tc.locale") == null ? "zh_CN" : getPropertyResolver().getProperty("tc.locale"), false);
    }

    protected CredentialTcContextHolder(@NonNull String username, @NonNull String password, boolean isEphemeral) {
      this(username, password, "",
          getPropertyResolver().getProperty("tc.locale") == null ? "zh_CN" : getPropertyResolver().getProperty("tc.locale"), isEphemeral);
    }

    protected CredentialTcContextHolder(@NonNull String username, @NonNull String password, String locale) {
        this(username, password, "", locale, false);
    }

    private String toPassword(int len) {
        char[] charArray = new char[len];
        Arrays.fill(charArray, '*');
        return new String(charArray);
    }

    @Override
    @ScopeDesc(Scope.TcCredential)
    public Connection getConnection() {
        Connection conn = connectionBuilder.build(credentialManager);
        isSessionCreated = true;
        return conn;
    }
    
    @EqualsAndHashCode
    public static class SimpleCredentialManager implements CredentialManager {
        private String username;
        private String password;
        private String group = "";
        private String role = "";
        private String locale;
        /**
         * if the credential is ephemeral, the credential login will take affect within current thread or request, it
         * will be logout at {@link SOAExecutionContext} cleanupSiliently
         */
        @Getter
        private boolean isEphemeral;

        public SimpleCredentialManager(@NonNull String username, @NonNull String password, String group, String locale,
            boolean isEphemeral) {
            this.username = username;
            this.password = password;
            this.group = group;
            this.locale = locale;
            this.isEphemeral = isEphemeral;
        }

        @Override
        public int getCredentialType() {
            return CredentialManager.CLIENT_CREDENTIAL_TYPE_STD;
        }

        @Override
        public String[] getCredentials(InvalidCredentialsException arg0) throws CanceledOperationException {
            return new String[] {this.username, this.password, this.group, this.role, this.toString()};
        }

        @Override
        public String[] getCredentials(InvalidUserException arg0) throws CanceledOperationException {
            return new String[] {this.username, this.password, this.group, this.role, this.toString()};
        }

        @Override
        public void setGroupRole(String group, String role) {}

        @Override
        public void setUserPassword(String username, String password, String arg2) {
            this.username = username;
            this.password = password;
        }

        @Override
        public String toString() {
            if (isEphemeral) {
                return String.join(",", username, String.valueOf(password.hashCode()), locale, group, role,
                    String.valueOf(System.identityHashCode(this)));
            } else {
                return String.join(",", username, String.valueOf(password.hashCode()), locale, group, role);
            }
        }

        public Credentials toCredentials() {
            Credentials credentials = new Credentials();
            credentials.user = this.username;
            credentials.password = this.password;
            credentials.descrimator = "EMOPAppClient" + System.currentTimeMillis();
            if (StringUtils.isNotEmpty(locale)) {
                credentials.locale = locale;
            }
            return credentials;
        }
    }

    @Override
    public int getPriority() {
        return 10;
    }

    public boolean isEphemeral() {
        return credentialManager.isEphemeral;
    }

    public String getUsername() {
        try {
            return credentialManager.getCredentials((InvalidCredentialsException)null)[0];
        } catch (CanceledOperationException e) {
            log.warn(e.getMessage());
            return "uknown";
        }
    }

    public String getIdentifier() {
        return String.join("-", connectionBuilder.getAppPath(), credentialManager.toString());
    }
}

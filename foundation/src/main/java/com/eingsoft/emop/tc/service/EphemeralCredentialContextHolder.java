package com.eingsoft.emop.tc.service;

import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.annotation.TcOperationThroughCredential;
import com.eingsoft.emop.tc.connection.ConnectionBuilder;

/**
 * connect to Teamcenter through a credential, the connection information is stored in {@link ConnectionBuilder}, and
 * the credential login will take affect within current thread or request, it will be logout at
 * {@link SOAExecutionContext} cleanupSiliently.
 * 
 * user cannot create instance directly, use {@link TcOperationThroughCredential} annotation or method in {@link TcContextHolderAware}
 * 
 * @author beam
 *
 */
public class EphemeralCredentialContextHolder extends CredentialTcContextHolder {

    protected EphemeralCredentialContextHolder(String username, String password) {
        super(username, password, true);
    }

    protected EphemeralCredentialContextHolder(String username, String password, String group, String locale) {
        super(username, password, group, locale, true);
    }

    protected EphemeralCredentialContextHolder(String username, String password, String locale) {
        super(username, password, "", locale, true);
    }
}

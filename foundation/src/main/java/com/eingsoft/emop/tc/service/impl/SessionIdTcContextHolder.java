package com.eingsoft.emop.tc.service.impl;

import static com.eingsoft.emop.tc.connection.ConnectionBuilderFactory.createConnectionBuilder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;
import com.eingsoft.emop.tc.connection.ConnectionBuilder;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.teamcenter.soa.client.Connection;

/**
 * connect to Teamcenter through a teamcenter rich client session id, the connection information is stored in
 * {@link ConnectionBuilder}
 * 
 * @author beam
 *
 */
@Log4j2
@EqualsAndHashCode(callSuper = false)
@ScopeDesc(Scope.Request)
public class SessionIdTcContextHolder extends TcContextHolder {

    private final String sessionId;
    @Getter
    private boolean isSessionCreated;

    public SessionIdTcContextHolder(@NonNull String sessionId) {
        super(createConnectionBuilder());
        this.sessionId = sessionId;
        log.debug("Init " + SessionIdTcContextHolder.class.getSimpleName() + " with params[tcServer={}, sessionId={}]",
            connectionBuilder.toString(), sessionId);
    }

    @Override
    @ScopeDesc(Scope.TcSessionId)
    public Connection getConnection() {
        isSessionCreated = true;
        return connectionBuilder.build(sessionId);
    }

    @Override
    public int getPriority() {
        return 5;
    }

}
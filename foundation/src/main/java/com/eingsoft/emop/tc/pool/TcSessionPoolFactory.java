package com.eingsoft.emop.tc.pool;

import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.connection.ConnectionBuilder;
import com.eingsoft.emop.tc.service.CredentialTcContextHolder;
import com.eingsoft.emop.tc.service.CredentialTcContextHolderAware;
import com.teamcenter.services.strong.core.SessionService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import static com.eingsoft.emop.tc.connection.ConnectionBuilderFactory.USERNAME;
import static com.eingsoft.emop.tc.propertyresolver.PropertyResolver.getPropertyResolver;

@Log4j2
public class TcSessionPoolFactory implements PooledObjectFactory<CredentialTcContextHolder>, CredentialTcContextHolderAware {

  @Override
  public void activateObject(PooledObject<CredentialTcContextHolder> paramPooledObject) throws Exception {
    // it is necessary to init SOAExecutionContext before using it, tc.pool.testOnBorrow means it will be used after makeObject
    SOAExecutionContext.current().tryToInitSOADiagnosticInfo(getPropertyResolver().getProperty(USERNAME));
    // just try to get current user info, make sure it is already logined
    if(!paramPooledObject.getObject().isSessionCheckNecessary()) {
      paramPooledObject.getObject().getTcSOAService().getUser();
      paramPooledObject.getObject().sessionChecked();
    }
  }

  @Override
  public void destroyObject(PooledObject<CredentialTcContextHolder> paramPooledObject) throws Exception {
    if (paramPooledObject.getObject().isSessionCreated()) {
      try {
        SessionService.getService(paramPooledObject.getObject().getConnection()).logout();
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      } finally {
        ConnectionBuilder.removeConnectionCache((paramPooledObject.getObject()).getIdentifier());
      }
    }
    paramPooledObject.getObject().getConnection();
  }

  @Override
  public PooledObject<CredentialTcContextHolder> makeObject() throws Exception {
    return new DefaultPooledObject<CredentialTcContextHolder>(createCredentialContextHolderFromConfig());
  }

  @Override
  public void passivateObject(PooledObject<CredentialTcContextHolder> paramPooledObject) throws Exception {}

  @Override
  public boolean validateObject(PooledObject<CredentialTcContextHolder> paramPooledObject) {
    return true;
  }

}

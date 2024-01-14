package com.eingsoft.emop.tc.service;

import static com.eingsoft.emop.tc.connection.ConnectionBuilderFactory.PASSWORD;
import static com.eingsoft.emop.tc.connection.ConnectionBuilderFactory.USERNAME;
import static com.eingsoft.emop.tc.propertyresolver.PropertyResolver.getPropertyResolver;

public interface CredentialTcContextHolderAware {

  default EphemeralCredentialContextHolder createCredentialContextHolderFromConfig() {
    return new EphemeralCredentialContextHolder(getPropertyResolver().getProperty(USERNAME), getPropertyResolver().getProperty(PASSWORD));
  }

  default EphemeralCredentialContextHolder createCredentialContextHolder(String username, String password) {
    return new EphemeralCredentialContextHolder(username, password);
  }
}

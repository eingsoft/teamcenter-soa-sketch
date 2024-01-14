package com.eingsoft.emop.tc.service;

import static com.eingsoft.emop.tc.util.ReflectionUtil.isJUnitTestContext;
import static com.eingsoft.emop.tc.util.ServiceUtil.getService;
import java.security.Permission;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.TcSOAServiceDataException;
import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;
import com.eingsoft.emop.tc.connection.ConnectionBuilder;
import com.teamcenter.services.internal.strong.core.ICTService;
import com.teamcenter.services.internal.strong.structuremanagement.RestructureService;
import com.teamcenter.services.strong.administration.IRMService;
import com.teamcenter.services.strong.administration.PreferenceManagementService;
import com.teamcenter.services.strong.classification.ClassificationService;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core.DigitalSignatureService;
import com.teamcenter.services.strong.core.DispatcherManagementService;
import com.teamcenter.services.strong.core.FileManagementService;
import com.teamcenter.services.strong.core.LOVService;
import com.teamcenter.services.strong.core.ProjectLevelSecurityService;
import com.teamcenter.services.strong.core.ReservationService;
import com.teamcenter.services.strong.core.SessionService;
import com.teamcenter.services.strong.core.StructureManagementService;
import com.teamcenter.services.strong.projectmanagement.ScheduleManagementService;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.services.strong.structuremanagement.StructureService;
import com.teamcenter.services.strong.workflow.WorkflowService;
import com.teamcenter.soa.client.Connection;
import com.teamcenter.soa.client.model.ServiceData;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

/**
 * Keep the context information connecting to teamcenter, all the TcXXXService are
 * {@link TcContextHolder} level, which means different {@link TcContextHolder} instance will have
 * different set of service instances.
 * 
 */
@Log4j2
@ScopeDesc(Scope.Request)
public abstract class TcContextHolder {

  /**
   * it contains the configured connection information
   */
  protected final ConnectionBuilder connectionBuilder;
  // registered services
  @Getter
  private Map<String, Object> services = new HashMap<String, Object>();

  @Getter
  private long lastSessionCheckTime = 0;

  protected TcContextHolder(ConnectionBuilder connectionBuilder) {
    this.connectionBuilder = connectionBuilder;
  }

  static {
    System.setSecurityManager(new SecurityManager() {
      @Override
      public void checkExit(int status) {
        if (!isJUnitTestContext()) {
          throw new SecurityException("exitVM." + status + " is not allowed.");
        }
      }

      @Override
      public void checkPermission(Permission perm) {
        // permit All
      }

      @Override
      public void checkPermission(Permission perm, Object context) {
        // permit All
      }
    });

    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        ConnectionBuilder.destoryAllConnections();
      }
    });
  }

  /**
   * The bigger, the higher priority
   * 
   */
  public abstract int getPriority();

  /**
   * Indicate whether the context is update-able when there is already an instance existing in the
   * {@link SOAExecutionContext}
   */
  public boolean canBeUpdated(@NonNull TcContextHolder newHolder) {
    if (newHolder.getPriority() == this.getPriority()) {
      return this.equals(newHolder);
    } else {
      return newHolder.getPriority() - this.getPriority() > 0;
    }
  }

  public abstract Connection getConnection();

  public abstract boolean isSessionCreated();

  public DataManagementService getDataManagementService() {
    return DataManagementService.getService(getConnection());
  }

  public com.teamcenter.services.strong.manufacturing.DataManagementService getMfgDataManagementService() {
    return com.teamcenter.services.strong.manufacturing.DataManagementService.getService(getConnection());
  }

  public StructureManagementService getStructureManagementService() {
    return StructureManagementService.getService(getConnection());
  }

  public StructureService getStructureService() {
    return StructureService.getService(getConnection());
  }

  public RestructureService getRestructureService() {
    return RestructureService.getService(getConnection());
  }

  public com.teamcenter.services.strong.bom.StructureManagementService getBOMStructureManagementService() {
    return com.teamcenter.services.strong.bom.StructureManagementService.getService(getConnection());
  }

  public com.teamcenter.services.strong.cad.StructureManagementService getCADStructureManagementService() {
    return com.teamcenter.services.strong.cad.StructureManagementService.getService(getConnection());
  }

  public FileManagementService getFileManagementService() {
    return FileManagementService.getService(getConnection());
  }

  public ReservationService getReservationService() {
    return ReservationService.getService(getConnection());
  }

  public ProjectLevelSecurityService getProjectLevelSecurityService() {
    return ProjectLevelSecurityService.getService(getConnection());
  }

  public DigitalSignatureService getDigitalSignatureService() {
    return DigitalSignatureService.getService(getConnection());
  }

  public DispatcherManagementService getDispatcherManagementService() {
    return DispatcherManagementService.getService(getConnection());
  }

  public SessionService getSessionService() {
    return SessionService.getService(getConnection());
  }

  public SavedQueryService getSavedQueryService() {
    return SavedQueryService.getService(getConnection());
  }

  public ClassificationService getClassificationService() {
    return ClassificationService.getService(getConnection());
  }

  public WorkflowService getWorkflowService() {
    return WorkflowService.getService(getConnection());
  }

  public LOVService getLOVService() {
    return LOVService.getService(getConnection());
  }

  public ScheduleManagementService getScheduleManagementService() {
    return ScheduleManagementService.getService(getConnection());
  }

  public PreferenceManagementService getPreferenceManagementService() {
    return PreferenceManagementService.getService(getConnection());
  }

  public IRMService getIRMService() {
    return IRMService.getService(getConnection());
  }

  public ICTService getICTService() {
    return ICTService.getService(getConnection());
  }

  /**
   * 如果ServiceData 中有错误， 则打印出来，并记录LOG, 不抛异常.
   * 
   * Not necessary any more, refer to EmopPartialErrorListener
   * 
   * @param serviceData
   */
  @Deprecated
  public void printAndLogMessageFromServiceData(ServiceData serviceData) {
    printAndLogMessageFromServiceData(serviceData, false);
  }

  /**
   * 如果ServiceData 中有错误， 则打印出来，并记录LOG; 参数为true, 抛出Runtime Exception.
   * 
   * @param serviceData
   * @param isThrowRuntimeException false, NO throws exception
   */
  public void printAndLogMessageFromServiceData(ServiceData serviceData, boolean isThrowRuntimeException) {
    if (serviceData == null) {
      log.warn("The service data is null, no any log message to print.");
      return;
    }
    StringBuffer errorMsg = new StringBuffer();
    for (int i = 0; i < serviceData.sizeOfPartialErrors(); i++) {
      errorMsg.append(Arrays.toString(serviceData.getPartialError(i).getMessages()));
    }
    if (!errorMsg.toString().isEmpty()) {
      log.error(errorMsg.toString());
      if (isThrowRuntimeException) {
        throw new TcSOAServiceDataException(errorMsg.toString());
      }
    }
  }

  public TcBOMService getTcBOMService() {
    return getService(TcBOMService.class, this);
  }

  public TcOrgnizationService getTcOrgnizationService() {
    return getService(TcOrgnizationService.class, this);
  }

  public TcSOAService getTcSOAService() {
    return getService(TcSOAService.class, this);
  }

  public TcQueryService getTcQueryService() {
    return getService(TcQueryService.class, this);
  }

  public TcLoadService getTcLoadService() {
    return getService(TcLoadService.class, this);
  }

  public TcClassificationService getTcClassificationService() {
    return getService(TcClassificationService.class, this);
  }

  public TcWorkflowService getTcWorkflowService() {
    return getService(TcWorkflowService.class, this);
  }

  public TcLOVService getTcLOVService() {
    return getService(TcLOVService.class, this);
  }

  public TcScheduleMgmtService getTcScheduleMgmtService() {
    return getService(TcScheduleMgmtService.class, this);
  }

  public TcRelationshipService getTcRelationshipService() {
    return getService(TcRelationshipService.class, this);
  }

  public TcFileManagementService getTcFileManagementService() {
    return getService(TcFileManagementService.class, this);
  }

  public TcDataManagementService getTcDataManagementService() {
    return getService(TcDataManagementService.class, this);
  }

  public TcDispatcherManagementService getTcDispatcherManagementService() {
    return getService(TcDispatcherManagementService.class, this);
  }

  public TcBOMPrintService getTcBOMPrintService() {
    return getService(TcBOMPrintService.class, this);
  }

  public TcBOPService getTcBOPService() {
    return getService(TcBOPService.class, this);
  }

  public TcPreferenceService getTcPreferenceService() {
    return getService(TcPreferenceService.class, this);
  }

  public TcIRMService getTcIRMService() {
    return getService(TcIRMService.class, this);
  }

  public TcProjectService getTcProjectService() {
    return getService(TcProjectService.class, this);
  }

  public boolean isSessionCheckNecessary() {
    return System.currentTimeMillis() - lastSessionCheckTime > Long.getLong("SESSION_CHECK_INTERVAL", 1000 * 60 * 5);
  }

  public void sessionChecked() {
    lastSessionCheckTime = System.currentTimeMillis();
  }
}

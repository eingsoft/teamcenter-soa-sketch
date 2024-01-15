package com.eingsoft.emop.tc;

import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;
import com.eingsoft.emop.tc.connection.ConnectionBuilder;
import com.eingsoft.emop.tc.connection.EmopRequestListener;
import com.eingsoft.emop.tc.connection.EmopRequestListener.SOADiagnosticInfo;
import com.eingsoft.emop.tc.pool.TcSessionPool;
import com.eingsoft.emop.tc.propertyresolver.PropertyResolver;
import com.eingsoft.emop.tc.service.CredentialTcContextHolder;
import com.eingsoft.emop.tc.service.CredentialTcContextHolderAware;
import com.eingsoft.emop.tc.service.EphemeralCredentialContextHolder;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.cache.ModelObjectCache;
import com.eingsoft.emop.tc.service.cache.SimpleModelObjectCache;
import com.eingsoft.emop.tc.transformer.ModelObjectPropValueTransformer;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core.SessionService;
import com.teamcenter.services.strong.core._2006_03.DataManagement.CreateItemsOutput;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ObjectFactory;
import com.teamcenter.soa.client.model.strong.BOMWindow;
import com.teamcenter.soa.internal.client.model.EmopObjectFactory;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.Serializable;
import java.util.*;

import static com.eingsoft.emop.tc.connection.ConnectionBuilderFactory.POOLED;
import static com.eingsoft.emop.tc.connection.ConnectionBuilderFactory.USERNAME;
import static com.eingsoft.emop.tc.propertyresolver.PropertyResolver.getPropertyResolver;

@Log4j2
@ScopeDesc(Scope.Request)
public class SOAExecutionContext implements Serializable, CredentialTcContextHolderAware {

    /**
     *
     */
    private static final long serialVersionUID = -49715282663077051L;

    /**
     * the tcContextHolder instance used to consume current SOA services
     */
    private TcContextHolder tcContextHolder;

    /**
     * the ModelObject object cache
     */
    @Getter
    private ModelObjectCache modelObjectCache = new SimpleModelObjectCache();

    /**
     * the opened BOM window during the SOA execution
     */
    @Getter
    private Set<BOMWindow> openedBOMWindow = new HashSet<BOMWindow>();

    /**
     * the opened BOP lines during the SOA execution
     */
    @Getter
    private Set<ModelObject> openedBOPLines = new HashSet<ModelObject>();

    /**
     * the SOA performance counter information
     */
    private SOADiagnosticInfo diagnosticInfo;

    /**
     * if it is set to true, the tc server cache will be skipped, and it will retrieve data from db always.
     * <p>
     * Use the refreshObjects in {@link DataManagementService} to refresh objects.
     */
    @Getter
    private boolean disableTCServerCache;

    /**
     * record down the refreshed objects, make sure one uid is only necessary to refresh once.
     */
    @Getter
    private Set<String> refreshedObjectUids = new HashSet<String>();

    /**
     * attach additional information to ThreadLocal, it will be more convenient for applications to clean up resources
     * in one place.
     */
    private Map<String, Object> additionalInfos = new HashMap<String, Object>();

    /**
     * record down all created model objects, to make sure it is rollback-able for all the created objects
     */
    private Set<ModelObject> createdModelObjects = new HashSet<ModelObject>();

    /**
     * request level {@link ModelObjectPropValueTransformer}
     */
    private List<ModelObjectPropValueTransformer> propValTransformers = new ArrayList<ModelObjectPropValueTransformer>();

    /**
     * partial errors when executing SOA
     */
    @Getter
    private List<String> partialErrors = new ArrayList<>();

    private static ThreadLocal<SOAExecutionContext> execContext = new ThreadLocal<SOAExecutionContext>();

    static {
        ObjectFactory.registerObjectFactory(new EmopObjectFactory());
        log.info("Updated ObjectFactory to " + EmopObjectFactory.class.getName());
    }

    private SOAExecutionContext() {
    }

    public static SOAExecutionContext current() {
        SOAExecutionContext ctx = execContext.get();
        if (ctx == null) {
            ctx = new SOAExecutionContext();
            execContext.set(ctx);
        }
        return ctx;
    }

    public void disableModelObjectCache() {
        modelObjectCache.disable();
    }

    public void disableTCServerCache() {
        this.disableTCServerCache = true;
        log.debug("Disabling tc server cache will refresh all ModelObjects through DataManagementService.refreshObjects() API, if you are retrieving a large number of ModelObjects in one request or thread, it may lead to performance issue, we strongly recommend you to use " + EphemeralCredentialContextHolder.class + " instead of " + CredentialTcContextHolder.class + ".");
    }

    public void initWithSameCache(SOAExecutionContext ctx) {
        if (ctx.getDiagnosticInfo() != null) {
            init(ctx.getDiagnosticInfo().getUsername(), ctx.getTcContextHolder());
        } else {
            init("unknown", ctx.getTcContextHolder());
        }
        // should share the same cache
        modelObjectCache = ctx.getModelObjectCache();
    }

    public void init(@NonNull TcContextHolder tcContextHolder) {
        String username = "unknown";
        if (tcContextHolder instanceof CredentialTcContextHolder) {
            username = ((CredentialTcContextHolder) tcContextHolder).getUsername();
        }
        this.init(username, tcContextHolder);
    }

    //not pool forcely
    public void initWithoutPool(@NonNull String username, @NonNull String password) {
        CredentialTcContextHolder ctxHoder = createCredentialContextHolder(username, password);
        this.init(username, ctxHoder);
    }

    @SneakyThrows
    public void init() {
        if(!getPropertyResolver().getBooleanProperty(POOLED, true)){
            throw new RuntimeException("pool is not enabled, please init the context manually with other init method.");
        }
        TcContextHolder tcContextHolder = TcSessionPool.getInstance().borrowObject(20 * 1000);
        this.init(PropertyResolver.getPropertyResolver().getProperty(USERNAME), tcContextHolder);
    }

    public void init(@NonNull String username, @NonNull TcContextHolder tcContextHolder) {
        if (this.tcContextHolder != null) {
            if (this.tcContextHolder.canBeUpdated(tcContextHolder)) {
                log.debug("tcContextHolder is already initialized with instance " + this.tcContextHolder.toString() + ", trying to replace with " + tcContextHolder);
            } else {
                throw new IllegalStateException("tcContextHolder is already initialized with instance " + this.tcContextHolder.toString() + " while trying to init with " + tcContextHolder);
            }
        }
        this.tcContextHolder = tcContextHolder;
        tryToInitSOADiagnosticInfo(username);
    }

    public void tryToInitSOADiagnosticInfo(String username) {
        if (!hasSOADiagnosticInfo()) {
            this.diagnosticInfo = new SOADiagnosticInfo(username, Boolean.getBoolean("traceSoaUsage"));
        }
    }

    public void cleanupSiliently() {
        cleanupSiliently(true);
    }

    public void cleanupSiliently(boolean forceLogout) {
        if (tcContextHolder != null) {
            tcContextHolder.getTcBOMService().closeAllBOMWindowSiliently();
            tcContextHolder.getTcBOPService().closeAllBOPLinesSiliently();
            if (forceLogout && tcContextHolder instanceof CredentialTcContextHolder) {
                if (tcContextHolder instanceof CredentialTcContextHolder && PropertyResolver.getPropertyResolver().getBooleanProperty(POOLED, true)) {
                    TcSessionPool.getInstance().returnObject((CredentialTcContextHolder) tcContextHolder);
                }
                if (((CredentialTcContextHolder) tcContextHolder).isEphemeral() && tcContextHolder.isSessionCreated()) {
                    try {
                        if (!PropertyResolver.getPropertyResolver().getBooleanProperty(POOLED, true)) {
                            SessionService.getService(tcContextHolder.getConnection()).logout();
                        }
                        ConnectionBuilder.removeConnectionCache(((CredentialTcContextHolder) tcContextHolder).getIdentifier());
                    } catch (Exception e) {
                        log.warn("fail to logout ephemeral credential(" + ((CredentialTcContextHolder) tcContextHolder).getUsername() + ", xxxxxx).", e);
                    }
                }
            }
        }
        try {
            execContext.remove();
        } catch (Exception e) {
            log.warn("fail to cleanup thread local resources.", e);
        }
    }

    public boolean hasSOADiagnosticInfo() {
        return diagnosticInfo != null && getDiagnosticInfo().hasData();
    }

    public SOADiagnosticInfo getDiagnosticInfo() {
        if (diagnosticInfo == null) {
            throw new IllegalStateException(EmopRequestListener.class + " hasn't been initialized yet, please manually initialize & cleanup it.");
        }
        return diagnosticInfo;
    }

    public TcContextHolder getTcContextHolder() {
        if (tcContextHolder == null) {
            throw new IllegalStateException(TcContextHolder.class + " hasn't been initialized yet, please manually initialize & cleanup it.");
        }
        return tcContextHolder;
    }

    public boolean isTcContextHolderPresent() {
        return tcContextHolder != null;
    }

    public void attachAdditionalInfo(String key, Object value) {
        additionalInfos.put(key, value);
    }

    public Object get(String key) {
        return additionalInfos.get(key);
    }

    public <T> T get(String key, Class<T> clz) {
        return (T) additionalInfos.get(key);
    }

    public void createdModelObjects(Collection<? extends ModelObject> objs) {
        createdModelObjects.addAll(objs);
    }

    public void createdModelObject(ModelObject obj) {
        if (obj != null) {
            createdModelObjects.add(obj);
        }
    }

    public void createdItem(CreateItemsOutput output) {
        if (output != null) {
            createdModelObjects.add(output.item);
            createdModelObjects.add(output.itemRev);
        }
    }

    public void rollbackSiliently() {
        log.info("Trying to rollback created " + createdModelObjects.size() + " objects.");
        try {
            getTcContextHolder().getTcDataManagementService().deleteModelObjects(createdModelObjects);
        } catch (Exception e) {
            log.error("fail to cleanup created ModelObjects " + createdModelObjects, e);
        }
    }

    public Collection<ModelObjectPropValueTransformer> getTransformers() {
        return Collections.unmodifiableCollection(propValTransformers);
    }

    public void register(@NonNull ModelObjectPropValueTransformer interceptor) {
        propValTransformers.add(interceptor);
        Collections.sort(propValTransformers, (o1, o2) -> {
            return o1.getPriority() - o2.getPriority();
        });
    }

    public boolean unregister(@NonNull ModelObjectPropValueTransformer interceptor) {
        return propValTransformers.remove(interceptor);
    }
}

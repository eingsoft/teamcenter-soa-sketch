package com.eingsoft.emop.tc.util;

import static com.eingsoft.emop.tc.util.MockDataUtil.createTcContextHolder;
import static com.eingsoft.emop.tc.util.MockDataUtil.initConnectionBuilderInfo;

import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.service.CredentialTcContextHolder;
import com.eingsoft.emop.tc.service.CredentialTcContextHolderAware;
import com.eingsoft.emop.tc.service.MockService;
import com.eingsoft.emop.tc.service.TcBOMPrintService;
import com.eingsoft.emop.tc.service.TcBOMService;
import com.eingsoft.emop.tc.service.TcBOPService;
import com.eingsoft.emop.tc.service.TcClassificationService;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcFileManagementService;
import com.eingsoft.emop.tc.service.TcLOVService;
import com.eingsoft.emop.tc.service.TcLoadService;
import com.eingsoft.emop.tc.service.TcQueryService;
import com.eingsoft.emop.tc.service.TcRelationshipService;
import com.eingsoft.emop.tc.service.TcSOAService;
import com.eingsoft.emop.tc.service.TcScheduleMgmtService;
import com.eingsoft.emop.tc.service.TcWorkflowService;
import com.eingsoft.emop.tc.service.ViolateMockService;
import com.eingsoft.emop.tc.service.impl.SessionIdTcContextHolder;

public class ServiceUtilTest implements CredentialTcContextHolderAware {
    private MockService mockService = ServiceUtil.getService(MockService.class, createTcContextHolder());

    @Before
    public void setup() {
        initConnectionBuilderInfo();
    }

    @Test
    public void testProxyResultAutomaticallyForModelObject() {
        ModelObject modelObject = mockService.getModelObject();
        Assert.assertTrue(modelObject instanceof ModelObject);
        Assert.assertEquals("uidA", modelObject.getUid());
    }

    @Test
    public void testProxyResultAutomaticallyForModelObjectsList() {
        List<ModelObject> modelObjects = mockService.getModelObjects();
        Assert.assertTrue(modelObjects instanceof List);
        Assert.assertEquals(2, modelObjects.size());
        Assert.assertEquals("uidA", modelObjects.get(0).getUid());
        Assert.assertEquals("uidB", modelObjects.get(1).getUid());
        Assert.assertTrue(modelObjects.get(0) instanceof ModelObject);
        Assert.assertTrue(modelObjects.get(1) instanceof ModelObject);
    }

    @Test
    public void testProxyResultAutomaticallyForModelObjectsArray() {
        ModelObject[] modelObjects = mockService.getModelObjectsArray();
        Assert.assertTrue(modelObjects instanceof ModelObject[]);
        Assert.assertEquals(2, modelObjects.length);
        Assert.assertEquals("uidA", modelObjects[0].getUid());
        Assert.assertEquals("uidB", modelObjects[1].getUid());
        Assert.assertTrue(modelObjects[0] instanceof ModelObject);
        Assert.assertTrue(modelObjects[1] instanceof ModelObject);

    }

    @Test
    public void testProxyResultWithoutProxy() {
        Assert.assertFalse(mockService.getObject() instanceof ModelObject);

        Assert.assertEquals(0, mockService.getPrimitiveType());

        Assert.assertEquals(1, mockService.getStrs().size());
        Assert.assertEquals("str", mockService.getStrs().get(0));

        Assert.assertEquals(1, mockService.getObjects().size());
        Assert.assertFalse(mockService.getObjects().get(0) instanceof ModelObject);

        Assert.assertEquals(1, mockService.getPrimitiveArray().length);
        Assert.assertEquals(1, mockService.getPrimitiveArray()[0]);
    }

    @Test
    public void testAllServiceSignatureMeetsStandards() {
        TcContextHolder tcContextHolder = new SessionIdTcContextHolder(UUID.randomUUID().toString());
        ServiceUtil.getService(TcBOMService.class, tcContextHolder);
        ServiceUtil.getService(TcClassificationService.class, tcContextHolder);
        ServiceUtil.getService(TcFileManagementService.class, tcContextHolder);
        ServiceUtil.getService(TcLoadService.class, tcContextHolder);
        ServiceUtil.getService(TcLOVService.class, tcContextHolder);
        ServiceUtil.getService(TcQueryService.class, tcContextHolder);
        ServiceUtil.getService(TcRelationshipService.class, tcContextHolder);
        ServiceUtil.getService(TcScheduleMgmtService.class, tcContextHolder);
        ServiceUtil.getService(TcSOAService.class, tcContextHolder);
        ServiceUtil.getService(TcWorkflowService.class, tcContextHolder);
        ServiceUtil.getService(TcBOMPrintService.class, tcContextHolder);
        ServiceUtil.getService(TcBOPService.class, tcContextHolder);
    }

    @Test
    public void testServicsInTcContextHolderInstance() {
        TcContextHolder tcContextHolder = new SessionIdTcContextHolder(UUID.randomUUID().toString());
        TcContextHolder tcContextHolder2 = createCredentialContextHolder("infodba", "infodba");
        Assert.assertNotNull(ServiceUtil.getService(TcBOMService.class, tcContextHolder));
        Assert.assertSame(ServiceUtil.getService(TcBOMService.class, tcContextHolder),
            ServiceUtil.getService(TcBOMService.class, tcContextHolder));
        Assert.assertNotNull(ServiceUtil.getService(TcBOMService.class, tcContextHolder2));
        Assert.assertNotEquals(ServiceUtil.getService(TcBOMService.class, tcContextHolder),
            ServiceUtil.getService(TcBOMService.class, tcContextHolder2));
    }

    @Test(expected = IllegalStateException.class)
    public void testViolateService() {
        TcContextHolder tcContextHolder = createTcContextHolder();
        ServiceUtil.getService(ViolateMockService.class, tcContextHolder);
    }
}

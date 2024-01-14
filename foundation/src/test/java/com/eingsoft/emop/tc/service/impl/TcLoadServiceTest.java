package com.eingsoft.emop.tc.service.impl;

import static com.eingsoft.emop.tc.util.MockDataUtil.addProperty;
import static com.eingsoft.emop.tc.util.MockDataUtil.createModelObject;
import static com.eingsoft.emop.tc.util.MockDataUtil.createModelObjectPropertyDescription;
import static com.eingsoft.emop.tc.util.MockDataUtil.createTcContextHolder;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcLoadService;
import com.eingsoft.emop.tc.util.ProxyUtil;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.PropertyDescription;
import com.teamcenter.soa.client.model.ServiceData;

public class TcLoadServiceTest {

    @Test
    public void testLoadPropertiesWithoutCache() {
        SOAExecutionContext.current().getModelObjectCache().disable();
        try {
            TcLoadService service = spy(new TcLoadServiceImpl(mock(TcContextHolder.class)));
            List<ModelObject> objs = Arrays.asList(createModelObject("Type", "uid"));
            doReturn(objs).when(service).loadProperties(objs, emptyList());
            service.loadProperties(objs);
            verify(service, times(1)).loadProperties(objs, emptyList());
        } finally {
            SOAExecutionContext.current().cleanupSiliently();
        }
    }

    @Test
    public void testLoadPropertiesWithCache() {
        SOAExecutionContext.current().init(createTcContextHolder());
        try {
            TcLoadService service = spy(new TcLoadServiceImpl(mock(TcContextHolder.class)));
            List<com.eingsoft.emop.tc.model.ModelObject> objs =
                ProxyUtil.spy(Arrays.asList(createModelObject("Type", "uid")), createTcContextHolder());
            doReturn(objs).when(service).loadProperties(objs, emptyList());
            service.loadProperties(objs);
            verify(service, times(0)).loadProperties(objs, emptyList());
            Assert.assertEquals(1, SOAExecutionContext.current().getModelObjectCache().asMap().size());
            Assert.assertEquals(objs.get(0), SOAExecutionContext.current().getModelObjectCache().asMap().get("uid"));

            List<com.eingsoft.emop.tc.model.ModelObject> comparsion =
                ProxyUtil.spy(Arrays.asList(createModelObject("Type", "uid1"), createModelObject("Type", "uid2")),
                    createTcContextHolder());
            doReturn(comparsion).when(service).loadProperties(comparsion, emptyList());
            service.loadProperties(Arrays.asList(createModelObject("Type", "uid"), createModelObject("Type", "uid1"),
                createModelObject("Type", "uid2")));
            verify(service, times(0)).loadProperties(comparsion, emptyList());
            Assert.assertEquals(3, SOAExecutionContext.current().getModelObjectCache().asMap().size());
        } finally {
            SOAExecutionContext.current().cleanupSiliently();
        }
    }

    @Test
    public void testLoadPropertiesByGivenNamesWithoutCache() {
        SOAExecutionContext.current().disableModelObjectCache();
        try {
            TcLoadServiceImpl service = spy(new TcLoadServiceImpl(mock(TcContextHolder.class)));
            List<ModelObject> objs = Arrays.asList(createModelObject("Type", "uid"));
            List<String> props = Arrays.asList("PropA");
            doAnswer(a -> a.getArguments()[0]).when(service).loadPropertiesInternal(objs, props);
            service.loadProperties(objs, props);
            verify(service, times(1)).loadPropertiesInternal(objs, props);
        } finally {
            SOAExecutionContext.current().cleanupSiliently();
        }
    }

    @Test
    public void testLoadPropertiesByGivenNamesWithCache() throws Exception {
        SOAExecutionContext.current().init(createTcContextHolder());
        try {
            // first load
            TcLoadServiceImpl service = spy(new TcLoadServiceImpl(mock(TcContextHolder.class)));
            List<com.eingsoft.emop.tc.model.ModelObject> objs =
                ProxyUtil.spy(Arrays.asList(createModelObject("Type", "uid")), createTcContextHolder());
            ModelObject objB = createModelObject("Type", "uidB");
            List<String> props = Arrays.asList("PropA");
            doAnswer(a -> a.getArguments()[0]).when(service).loadPropertiesInternal(objs, props);
            service.loadProperties(objs, props);
            verify(service, times(1)).loadPropertiesInternal(objs, props);
            Assert.assertEquals(1, SOAExecutionContext.current().getModelObjectCache().asMap().size());
            Assert.assertEquals(objs.get(0), SOAExecutionContext.current().getModelObjectCache().asMap().get("uid"));

            // second time load, should from cache
            Hashtable<String, PropertyDescription> propertiesDef = new Hashtable<String, PropertyDescription>();
            propertiesDef.put("PropA", mock(PropertyDescription.class));
            propertiesDef.put("PropB", mock(PropertyDescription.class));
            when(objs.get(0).getTypeObject().getPropDescs()).thenReturn(propertiesDef);
            addProperty(objs.get(0), "PropA", createModelObjectPropertyDescription(objB));
            service.loadProperties(objs, props);
            verify(service, times(1)).loadPropertiesInternal(objs, props);

            // add additional property load, although in cache, but still some properties haven't been loaded, so need
            // reload
            props = Arrays.asList("PropA", "PropB");
            doAnswer(a -> a.getArguments()[0]).when(service).loadPropertiesInternal(objs, props);
            service.loadProperties(objs, props);
            verify(service, times(1)).loadPropertiesInternal(objs, props);
        } finally {
            SOAExecutionContext.current().cleanupSiliently();
        }
    }

    @Test
    public void testLoadPropertiesInternal() {
        TcContextHolder holder = mock(TcContextHolder.class);
        TcLoadServiceImpl service = new TcLoadServiceImpl(holder);
        List<com.eingsoft.emop.tc.model.ModelObject> objs =
            ProxyUtil.spy(Arrays.asList(createModelObject("Type", "uid")), createTcContextHolder());
        List<String> props = Arrays.asList("PropA");
        DataManagementService dms = mock(DataManagementService.class);
        ServiceData sd = mock(ServiceData.class);
        when(sd.sizeOfPlainObjects()).thenReturn(1);
        when(sd.getPlainObject(0)).thenReturn(objs.get(0));
        when(dms.getProperties(new ModelObject[] {objs.get(0)}, new String[] {props.get(0)})).thenReturn(sd);
        when(holder.getDataManagementService()).thenReturn(dms);
        List<com.eingsoft.emop.tc.model.ModelObject> result = service.loadPropertiesInternal(objs, props);
        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.get(0) instanceof com.eingsoft.emop.tc.model.ModelObject);
        verify(sd, times(1)).getPlainObject(0);
    }
}

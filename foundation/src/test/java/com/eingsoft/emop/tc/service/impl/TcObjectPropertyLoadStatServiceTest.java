package com.eingsoft.emop.tc.service.impl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.eingsoft.emop.tc.model.ModelObject;
import com.teamcenter.soa.client.model.Type;

public class TcObjectPropertyLoadStatServiceTest {

//    private TcObjectPropertyLoadStatService service = TcObjectPropertyLoadStatService.getInstance();
//
//    private static final String TYPE_NAME = "Item";
//
//    @Before
//    public void init() {
//        service.cleanUp();
//    }
//
//    @Test(expected = NullPointerException.class)
//    public void nullIsNotAllowed() {
//        service.addReferencedProperty(null, null);
//    }
//
//    @Test
//    public void skipNullPropertyStat() {
//        service.addReferencedProperties(TYPE_NAME, new String[] {"propA", null, "propB"});
//        Assert.assertEquals(1, service.getStats().size());
//        Frequency freq = service.getStats().get(TYPE_NAME).getFrequency();
//        Assert.assertEquals(2, freq.getUniqueCount());
//
//        System.out.println(service.toString());
//    }
//
//    @Test
//    public void testGetBestLoadPropertyNames() {
//        service.addReferencedProperties(TYPE_NAME, new String[] {"propA", null, "propB"});
//        service.addReferencedProperties(TYPE_NAME, new String[] {"propC", "propD", "propE"});
//        service.addReferencedProperties(TYPE_NAME, new String[] {"propA", null, "propB"});
//        service.addReferencedProperties(TYPE_NAME, new String[] {"propC", null, "propD"});
//        service.addReferencedProperties(TYPE_NAME, new String[] {"propA", "propC"});
//        service.addReferencedProperties(TYPE_NAME, new String[] {"propA", null, "propB"});
//
//        Assert.assertEquals(1, service.getStats().size());
//        Frequency freq = service.getStats().get(TYPE_NAME).getFrequency();
//        Assert.assertEquals(5, freq.getUniqueCount());
//        Assert.assertEquals(4, freq.getCount("propA"));
//        Assert.assertEquals(3, freq.getCount("propB"));
//        Assert.assertEquals(3, freq.getCount("propC"));
//        Assert.assertEquals(2, freq.getCount("propD"));
//        Assert.assertEquals(1, freq.getCount("propE"));
//
//        ModelObject obj = createModelObject();
//        String[] propNames = service.getRecommendedLoadPropertyNames(obj);
//        Assert.assertEquals(6, propNames.length);
//        Assert.assertArrayEquals(new String[] {"propA", "propB", "propC", "propD", "propDefault", "propE"}, propNames);
//
//        int loopCount = 10;
//        for (int i = 0; i < loopCount; i++) {
//            service.addReferencedProperties(TYPE_NAME, new String[] {"propA", "propB", "propC"});
//        }
//        // cached result
//        Assert.assertEquals(6, propNames.length);
//        Assert.assertArrayEquals(new String[] {"propA", "propB", "propC", "propD", "propDefault", "propE"}, propNames);
//
//        // force recalculate it
//        propNames = service.forceCalculateRecommendedProperties(obj);
//        Assert.assertEquals(4, propNames.length);
//        Assert.assertArrayEquals(new String[] {"propA", "propB", "propC", "propDefault"}, propNames);
//
//        System.out.println(service.toString());
//    }
//
//    private ModelObject createModelObject() {
//        ModelObject obj = Mockito.mock(ModelObject.class);
//        Type type = Mockito.mock(Type.class);
//        Mockito.when(type.getName()).thenReturn(TYPE_NAME);
//        Mockito.when(obj.getPropertyNames()).thenReturn(new String[] {"propDefault"});
//        Mockito.when(obj.getTypeObject()).thenReturn(type);
//        return obj;
//    }
//
//    @Test
//    public void testDefaultSigmaSetting() {
//        for (int i = 0; i < 100; i++) {
//            service.addReferencedProperties(TYPE_NAME, new String[] {"propB", "propC"});
//        }
//        for (int i = 0; i < 20; i++) {
//            service.addReferencedProperties(TYPE_NAME, new String[] {"propA", "propC"});
//        }
//
//        String[] propNames = service.forceCalculateRecommendedProperties(createModelObject());
//        Assert.assertArrayEquals(new String[] {"propB", "propC", "propDefault"}, propNames);
//
//        for (int i = 0; i < 2; i++) {
//            service.addReferencedProperties(TYPE_NAME, new String[] {"propA"});
//        }
//        // propA is back
//        propNames = service.forceCalculateRecommendedProperties(createModelObject());
//        Assert.assertArrayEquals(new String[] {"propA", "propB", "propC", "propDefault"}, propNames);
//    }
}

package com.eingsoft.emop.tc.util;

import static com.eingsoft.emop.tc.util.MockDataUtil.addProperty;
import static com.eingsoft.emop.tc.util.MockDataUtil.createCalendarPropertyDescription;
import static com.eingsoft.emop.tc.util.MockDataUtil.createModelObjectArrayPropertyDescription;
import static com.eingsoft.emop.tc.util.MockDataUtil.createModelObjectPropertyDescription;
import static com.eingsoft.emop.tc.util.MockDataUtil.createTcContextHolder;
import static com.eingsoft.emop.tc.util.ProxyUtil.proxy;
import static com.eingsoft.emop.tc.util.ProxyUtil.spy;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import javassist.util.proxy.ProxyFactory;

import org.apache.commons.codec.binary.Hex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.eingsoft.emop.tc.model.Gettable;
import com.eingsoft.emop.tc.model.NullObjectGettable;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcObjectPropertyLoadStatService;
import com.eingsoft.emop.tc.util.ProxyUtil.Proxyer;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.Property;
import com.teamcenter.soa.client.model.Type;
import com.teamcenter.soa.client.model.strong.BOMLine;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.client.model.strong.MEProcessRevision;
import com.teamcenter.soa.internal.client.model.ModelObjectImpl;

public class ProxyUtilTest {
    private Type type = Mockito.mock(Type.class);

    @Before
    public void init() {
        TcObjectPropertyLoadStatService.getInstance().cleanUp();
        Mockito.when(type.getName()).thenReturn("TypeA");
    }

    @Test
    public void testProxyNull() {
        Assert.assertNull(ProxyUtil.proxy(null));
        Assert.assertNull(ProxyUtil.spy((ModelObject)null, null));
    }

    @Test
    public void testProxyList() {
        List<ModelObject> objs = Arrays.asList(new ModelObjectImpl(type, "Uid"));
        List<com.eingsoft.emop.tc.model.ModelObject> emopModelObjects = ProxyUtil.spy(objs, null);
        List<ModelObject> proxiedModelObjects = ProxyUtil.proxy(objs, null);
        Assert.assertEquals(1, emopModelObjects.size());
        Assert.assertEquals(1, proxiedModelObjects.size());
        Assert.assertEquals(objs.get(0), emopModelObjects.get(0));
        Assert.assertEquals(objs.get(0), proxiedModelObjects.get(0));
        Assert.assertTrue(proxiedModelObjects.get(0) instanceof com.eingsoft.emop.tc.model.ModelObject);
    }

    @Test
    public void testCorrectModelObjectProxied() {
        ModelObject obj = new ModelObjectImpl(type, "Uid");
        com.eingsoft.emop.tc.model.ModelObject proxied = (com.eingsoft.emop.tc.model.ModelObject)ProxyUtil.proxy(obj);
        Assert.assertEquals("Uid", proxied.getUid());
    }

    @Test
    public void testUnproxy() {
        ModelObject obj = new ModelObjectImpl(type, "Uid");
        com.eingsoft.emop.tc.model.ModelObject proxied = (com.eingsoft.emop.tc.model.ModelObject)ProxyUtil.proxy(obj);
        Assert.assertNotNull(proxied.unproxy());
        Assert.assertTrue(proxied.unproxy() instanceof ModelObject);
        Assert.assertFalse(proxied.unproxy() instanceof com.eingsoft.emop.tc.model.ModelObject);
        Assert.assertFalse(proxied.unproxy() instanceof Gettable);
        Assert.assertSame(obj, proxied.unproxy());
        Assert.assertSame(obj, ProxyUtil.unproxy(proxied));
        Assert.assertSame(obj, ProxyUtil.unproxy(obj));

        proxied = (com.eingsoft.emop.tc.model.ModelObject)ProxyUtil.spy(obj, Mockito.mock(TcContextHolder.class));
        Assert.assertNotNull(proxied.unproxy());
        Assert.assertSame(obj, proxied.unproxy());
    }

    @Test
    public void testUnproxyList() {
        List<ModelObject> objs = Arrays.asList(new ModelObjectImpl(type, "UidA"), new ModelObjectImpl(type, "UidB"));
        List<ModelObject> unProxiedObjs = ProxyUtil.unproxy(ProxyUtil.proxy(objs, createTcContextHolder()));
        Assert.assertEquals(2, unProxiedObjs.size());
        Assert.assertSame(objs.get(0), unProxiedObjs.get(0));
        Assert.assertSame(objs.get(1), unProxiedObjs.get(1));

        unProxiedObjs = ProxyUtil.unproxy(objs);
        Assert.assertEquals(2, unProxiedObjs.size());
        Assert.assertSame(objs.get(0), unProxiedObjs.get(0));
        Assert.assertSame(objs.get(1), unProxiedObjs.get(1));
    }

    @Test
    public void testCorrectModelObjectProxiedWithTcContextHolder() {
        ModelObject obj = new ModelObjectImpl(type, "Uid");
        TcContextHolder holder = Mockito.mock(TcContextHolder.class);
        com.eingsoft.emop.tc.model.ModelObject proxied =
            (com.eingsoft.emop.tc.model.ModelObject)ProxyUtil.proxy(obj, holder);
        Assert.assertEquals("Uid", proxied.getUid());
        Assert.assertSame(holder, proxied.getTcContextHolder());
    }

    @Test
    public void testSpy() {
        ModelObject obj = new ModelObjectImpl(type, "Uid");
        com.eingsoft.emop.tc.model.ModelObject proxied = ProxyUtil.spy(obj, null);
        Assert.assertEquals("Uid", proxied.getUid());

        MEProcessRevision rev = new MEProcessRevision(type, "Uid2");
        proxied = ProxyUtil.spy(rev, null);
        Assert.assertEquals("Uid2", proxied.getUid());
    }

    @Test
    public void testCorrectBOMLineProxied() {
        BOMLine obj = Mockito.mock(BOMLine.class);
        Mockito.when(obj.getUid()).thenReturn("Uid");
        BOMLine proxied = ProxyUtil.proxy(obj);
        Assert.assertEquals("Uid", proxied.getUid());
        Assert.assertTrue(proxied instanceof BOMLine);
        Assert.assertTrue(ProxyUtil.spy(obj, null) instanceof BOMLine);
    }

    @Test
    public void testGetPropertyStat() throws Exception {
//        ModelObject obj = new ModelObjectImpl(type, "Uid");
//        Property prop = Mockito.mock(Property.class);
//        addProperty(obj, "PropA", prop);
//        ModelObject proxied = ProxyUtil.proxy(obj);
//        Assert.assertEquals(prop, proxied.getPropertyObject("PropA"));
//        Map<String, PropertyReferencedStat> stats = TcObjectPropertyLoadStatService.getInstance().getStats();
//        Assert.assertEquals(1, stats.size());
//        Assert.assertEquals(1, stats.get("TypeA").getFrequency().getSumFreq());
//        Assert.assertEquals(1, stats.get("TypeA").getFrequency().getCount("PropA"));
//        Assert.assertArrayEquals(new String[] {"PropA"}, TcObjectPropertyLoadStatService.getInstance()
//            .getRecommendedLoadPropertyNames((com.eingsoft.emop.tc.model.ModelObject)ProxyUtil.proxy(obj)));
    }

    @Test
    public void testModelObjectProperty() throws Exception {
        ModelObject objA = new ModelObjectImpl(type, "UidA");
        ModelObject objB = new ModelObjectImpl(type, "UidB");
        Property propA = createModelObjectPropertyDescription(objB);
        Property propB = Mockito.mock(Property.class);
        addProperty(objA, "PropA", propA);
        addProperty(objB, "PropB", propB);

        ModelObject proxied = ProxyUtil.proxy(objA);
        Assert.assertSame(propA, proxied.getPropertyObject("PropA"));
        Assert.assertTrue(ProxyFactory
            .isProxyClass(proxied.getPropertyObject("PropA").getModelObjectValue().getClass()));
        Assert.assertSame(propB, proxied.getPropertyObject("PropA").getModelObjectValue().getPropertyObject("PropB"));
//        Map<String, PropertyReferencedStat> sta").getFrequency().getCount("PropB"));
    }
//    ts = TcObjectPropertyLoadStatService.getInstance().getStats();
    //        Assert.assertEquals(1, stats.size());
//        Assert.assertEquals(4, stats.get("TypeA").getFrequency().getSumFreq());
//        Assert.assertEquals(3, stats.get("TypeA").getFrequency().getCount("PropA"));
//        Assert.assertEquals(1, stats.get("TypeA
    @Test
    public void testModelObjectGet() throws Exception {
        ModelObject objA = new ModelObjectImpl(type, "UidA");
        ModelObject objB = new ModelObjectImpl(type, "UidB");
        Property propA = createModelObjectPropertyDescription(objB);
        Property propB = Mockito.mock(Property.class);
        addProperty(objA, "PropA", propA);
        addProperty(objB, "PropB", propB);

        ModelObject proxied = ProxyUtil.proxy(objA, createTcContextHolder());
        Assert.assertTrue(ProxyFactory.isProxyClass(ProxyUtil.spy(proxied, createTcContextHolder()).get("PropA")
            .getClass()));
        Assert.assertEquals(objB, ProxyUtil.spy(proxied, createTcContextHolder()).get("PropA"));
        Assert.assertSame(propB, proxied.getPropertyObject("PropA").getModelObjectValue().getPropertyObject("PropB"));
//        Map<String, PropertyReferencedStat> stats = TcObjectPropertyLoadStatService.getInstance().getStats();
//        Assert.assertEquals(1, stats.size());
//        Assert.assertEquals(4, stats.get("TypeA").getFrequency().getSumFreq());
//        Assert.assertEquals(3, stats.get("TypeA").getFrequency().getCount("PropA"));
//        Assert.assertEquals(1, stats.get("TypeA").getFrequency().getCount("PropB"));
    }
    
    @Test
    public void testGetModelObject() throws Exception {
        ModelObject objA = new ModelObjectImpl(type, "UidA");
        ModelObject objB = new ModelObjectImpl(type, "UidB");
        Property propA = createModelObjectPropertyDescription(objB);
        addProperty(objA, "PropA", propA);

        com.eingsoft.emop.tc.model.ModelObject obj = ProxyUtil.spy(objA, createTcContextHolder());
        Assert.assertTrue(obj.getModelObject("PropA") instanceof com.eingsoft.emop.tc.model.ModelObject);
        Assert.assertEquals("UidB", obj.getModelObject("PropA").getUid());
    }

    @Test
    public void testNullObject() throws Exception {
        ModelObject objA = new ModelObjectImpl(type, "UidA");
        Property propA = createModelObjectPropertyDescription(null);
        addProperty(objA, "PropA", propA);

        com.eingsoft.emop.tc.model.ModelObject obj = ProxyUtil.spy(objA, null);
        Assert.assertNull(obj.get("PropA"));
        Assert.assertTrue(obj.getOptional("PropA") instanceof NullObjectGettable);
    }

    @Test
    public void testNullObjectForGetMethod() throws Exception {
        Gettable.SKIP_NULL_OR_EMPTY.set(true);
        try {
            ModelObject objA = new ModelObjectImpl(type, "UidA");
            Property propA = createModelObjectPropertyDescription(null);
            addProperty(objA, "PropA", propA);

            com.eingsoft.emop.tc.model.ModelObject obj = ProxyUtil.spy(objA, null);
            Assert.assertNotNull(obj.get("PropA"));
            Assert.assertTrue(obj.get("PropA") instanceof NullObjectGettable);
            Assert.assertTrue(obj.getOptional("PropA") instanceof NullObjectGettable);
        } finally {
            Gettable.SKIP_NULL_OR_EMPTY.remove();
        }
    }

    @Test
    public void testEmptyList() throws Exception {
    	ModelObject objA = new ModelObjectImpl(type, "UidA");
        Property propA = createModelObjectArrayPropertyDescription(new ModelObject[] {});
        addProperty(objA, "PropA", propA);

        com.eingsoft.emop.tc.model.ModelObject obj = ProxyUtil.spy(objA, createTcContextHolder());
        Assert.assertEquals(((List)obj.get("PropA")).size(), 0);
        Assert.assertTrue(obj.getOptional("PropA") instanceof NullObjectGettable);
    }
    
    @Test
    public void testCalendarProp() throws Exception {
        ItemRevision objA = new ItemRevision(type, "UidA");
		Calendar cal = Calendar.getInstance();
        Property propA = createCalendarPropertyDescription(cal);
        addProperty(objA, "checked_out_date", propA);

        Assert.assertEquals(cal, proxy(objA, createTcContextHolder()).get_checked_out_date());
        
        Assert.assertTrue(spy(objA, createTcContextHolder()).get("checked_out_date") instanceof Calendar);
        Assert.assertEquals(cal, spy(objA, createTcContextHolder()).get("checked_out_date"));
    }
    
    @Test
	public void testModelObjectArray() throws Exception {
		Item objA = new Item(type, "ItemUid");
		ModelObject[] revs = new ModelObject[] { new ItemRevision(type, "RevUid") };
		Property propA = createModelObjectArrayPropertyDescription(revs);
		addProperty(objA, "revision_list", propA);

		Item item = proxy(objA, createTcContextHolder());
		Assert.assertSame(item.get_revision_list(), revs);
		
		Assert.assertTrue(spy(objA, createTcContextHolder()).get("revision_list") instanceof List<?>);
		Assert.assertEquals(((List<?>) spy(objA, createTcContextHolder()).get("revision_list")).get(0), revs[0]);
	}

    @Test
    public void testHasProperty() throws Exception {
        ModelObject objA = new ModelObjectImpl(type, "UidA");
        Property propA = createModelObjectArrayPropertyDescription(new ModelObject[] {});
        addProperty(objA, "PropA", propA);

        com.eingsoft.emop.tc.model.ModelObject obj = ProxyUtil.spy(objA, createTcContextHolder());
        Assert.assertTrue(obj.hasProperty("PropA"));
        Assert.assertFalse(obj.hasProperty("PropB"));
    }

    @Test
    public void testGetLoadedPropertyNames() throws Exception {
        ModelObject objA = new ModelObjectImpl(type, "UidA");
        Property propA = createModelObjectArrayPropertyDescription(new ModelObject[] {});
        addProperty(objA, "PropA", propA);

        com.eingsoft.emop.tc.model.ModelObject obj = ProxyUtil.spy(objA, createTcContextHolder());
        Assert.assertEquals(1, obj.getLoadedPropertyNames().size());
        Assert.assertTrue(obj.getLoadedPropertyNames().contains("PropA"));
    }

    @Test
    public void testModelObjectArrayProperty() throws Exception {
        ModelObject objA = new ModelObjectImpl(type, "UidA");
        ModelObject objB = new ModelObjectImpl(type, "UidB");
        ModelObject objC = new ModelObjectImpl(type, "UidC");
        ModelObject[] objs = new ModelObject[] {objB, objC};
        Property propA = createModelObjectArrayPropertyDescription(objs);
        Property propB = Mockito.mock(Property.class);
        Property propC = Mockito.mock(Property.class);
        addProperty(objA, "children", propA);
        addProperty(objB, "PropB", propB);
        addProperty(objC, "PropC", propC);

        ModelObject proxied = ProxyUtil.proxy(objA);
        Assert.assertSame(propA, proxied.getPropertyObject("children"));
        Arrays.stream(proxied.getPropertyObject("children").getModelObjectArrayValue()).forEach(
            m -> Assert.assertTrue(ProxyFactory.isProxyClass(m.getClass())));
        Assert.assertSame(propB,
            proxied.getPropertyObject("children").getModelObjectArrayValue()[0].getPropertyObject("PropB"));
        Assert.assertSame(propC,
            proxied.getPropertyObject("children").getModelObjectArrayValue()[1].getPropertyObject("PropC"));
//        Map<String, PropertyReferencedStat> stats = TcObjectPropertyLoadStatService.getInstance().getStats();
//        Assert.assertEquals(1, stats.size());
//        Assert.assertEquals(6, stats.get("TypeA").getFrequency().getSumFreq());
//        Assert.assertEquals(4, stats.get("TypeA").getFrequency().getCount("children"));
//        Assert.assertEquals(1, stats.get("TypeA").getFrequency().getCount("PropB"));
//        Assert.assertEquals(1, stats.get("TypeA").getFrequency().getCount("PropC"));
    }

    @Test
    public void testRelMethod() throws Exception {
        ModelObject objA = new ModelObjectImpl(type, "UidA");
        Type typeB = Mockito.mock(Type.class);
        Mockito.when(typeB.getName()).thenReturn("TypeB");
        ModelObject objB = new ModelObjectImpl(typeB, "UidB");
        ModelObject objC = new ModelObjectImpl(type, "UidC");
        ModelObject[] objs = new ModelObject[] {objB, objC};
        Property propA = createModelObjectArrayPropertyDescription(objs);
        Property propB = Mockito.mock(Property.class);
        Property propC = Mockito.mock(Property.class);
        addProperty(objA, "ref", propA);
        addProperty(objB, "PropB", propB);
        addProperty(objC, "PropC", propC);

        ModelObject proxied = ProxyUtil.proxy(objA, createTcContextHolder());
        List<ModelObject> refs = (List<ModelObject>)((com.eingsoft.emop.tc.model.ModelObject)proxied).rel("ref");
        Assert.assertEquals(2, refs.size());
        Assert.assertEquals(objB, refs.get(0));
        Assert.assertEquals(objC, refs.get(1));

        proxied = ProxyUtil.proxy(objA, createTcContextHolder());
        refs = (List<ModelObject>)((com.eingsoft.emop.tc.model.ModelObject)proxied).rel("ref", "TypeB");
        Assert.assertEquals(1, refs.size());
        Assert.assertEquals(objB, refs.get(0));

        proxied = ProxyUtil.proxy(objA, createTcContextHolder());
        refs = (List<ModelObject>)((com.eingsoft.emop.tc.model.ModelObject)proxied).rel("ref", "TypeBPlus");
        Assert.assertEquals(0, refs.size());
    }

    @Test
    public void testEquals() {
        ModelObject objA = new ModelObjectImpl(type, "UidA");
        ModelObject objAPlus = new ModelObjectImpl(type, "UidA");
        ModelObject objB = new ModelObjectImpl(type, "UidB");
        Assert.assertEquals(objA, ProxyUtil.proxy(objA));
        Assert.assertEquals(ProxyUtil.proxy(objA), ProxyUtil.proxy(objA));
        Assert.assertEquals(ProxyUtil.proxy(objA), ProxyUtil.proxy(objAPlus));
        Assert.assertNotEquals(ProxyUtil.proxy(objA), ProxyUtil.proxy(objB));
    }

    @Test
    public void testHashCode() {
        ModelObject objA = new ModelObjectImpl(type, "UidA");
        Assert.assertEquals("UidA".hashCode(), ProxyUtil.proxy(objA).hashCode());
    }

    @Test
    public void testGetMethodPattern() {
        Pattern pattern = Proxyer.MODEL_OBJECT_GET_METHOD_PATTERN;
        Matcher matcher = pattern.matcher("get_abc");
        Assert.assertTrue(matcher.find());
        Assert.assertEquals("abc", matcher.group("prop"));

        matcher = pattern.matcher("get_abc_def");
        Assert.assertTrue(matcher.find());
        Assert.assertEquals("abc_def", matcher.group("prop"));

        matcher = pattern.matcher("get_abc0def");
        Assert.assertTrue(matcher.find());
        Assert.assertEquals("abc0def", matcher.group("prop"));

        matcher = pattern.matcher("get_abc_def0");
        Assert.assertTrue(matcher.find());
        Assert.assertEquals("abc_def0", matcher.group("prop"));

        matcher = pattern.matcher("get_abc_dAf");
        Assert.assertTrue(matcher.find());
        Assert.assertEquals("abc_dAf", matcher.group("prop"));
        
        matcher = pattern.matcher("get_fnd0OriginalLocationCode");
        Assert.assertTrue(matcher.find());
        Assert.assertEquals("fnd0OriginalLocationCode", matcher.group("prop"));

        matcher = pattern.matcher("getabc_def");
        Assert.assertFalse(matcher.find());

        matcher = pattern.matcher("get_abcdef$");
        Assert.assertFalse(matcher.find());

        matcher = pattern.matcher("get");
        Assert.assertFalse(matcher.find());

        matcher = pattern.matcher("get_");
        Assert.assertFalse(matcher.find());
    }
    
    @Test
    public void generateExpiryToken() throws Exception {
        long days = -1;
        long expiry = System.currentTimeMillis() + days * 24 * 3600 * 1000;
        Cipher dcipher = Cipher.getInstance("DES");
        dcipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec("#E@M@OP!".getBytes(), "DES"));
        System.out.println(Hex.encodeHex(dcipher.doFinal(String.valueOf(expiry).getBytes())));
    }
}
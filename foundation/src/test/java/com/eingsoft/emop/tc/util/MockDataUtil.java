package com.eingsoft.emop.tc.util;

import static com.eingsoft.emop.tc.util.ProxyUtil.spy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Collectors;

import org.mockito.Mockito;

import com.eingsoft.emop.tc.service.TcBOMService;
import com.eingsoft.emop.tc.service.TcBOPService;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcLoadService;
import com.teamcenter.schemas.soa._2006_03.base.PropertyValue;
import com.teamcenter.soa.client.model.ClientDataModel;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.Property;
import com.teamcenter.soa.client.model.Type;
import com.teamcenter.soa.client.model.strong.BOMLine;
import com.teamcenter.soa.internal.client.model.ModelObjectImpl;
import com.teamcenter.soa.internal.client.model.PropertyCalenderImpl;
import com.teamcenter.soa.internal.client.model.PropertyDescriptionImpl;
import com.teamcenter.soa.internal.client.model.PropertyModelObjectArrayImpl;
import com.teamcenter.soa.internal.client.model.PropertyModelObjectImpl;
import com.teamcenter.soa.internal.client.model.PropertyStringImpl;

public class MockDataUtil {

    public static Property createModelObjectPropertyDescription(ModelObject propVal) throws Exception {
        PropertyDescriptionImpl propDesc = Mockito.mock(PropertyDescriptionImpl.class);
        Mockito.when(propDesc.getType()).thenReturn(9);
        Mockito.when(propDesc.isArray()).thenReturn(false);
        Property propA = new PropertyModelObjectImpl(propDesc, Arrays.asList(createPV("value")), Mockito.mock(ClientDataModel.class));
        ReflectionUtil.setFieldValue(propA, "mValue", propVal);
        return propA;
    }

    public static Property createStringPropertyDescription(String val) throws Exception {
        PropertyDescriptionImpl propDesc = Mockito.mock(PropertyDescriptionImpl.class);
        Mockito.when(propDesc.getType()).thenReturn(8);
        Mockito.when(propDesc.isArray()).thenReturn(false);
        Property propA = new PropertyStringImpl(propDesc, Arrays.asList(createPV("value")));
        ReflectionUtil.setFieldValue(propA, "mValue", val);
        return propA;
    }

    public static Property createCalendarPropertyDescription(Calendar cal) throws Exception {
        PropertyDescriptionImpl propDesc = Mockito.mock(PropertyDescriptionImpl.class);
        Mockito.when(propDesc.getType()).thenReturn(2);
        Mockito.when(propDesc.isArray()).thenReturn(false);
        Property propA = new PropertyCalenderImpl(propDesc, Arrays.asList(createPV("2021-09-11T12:02:50-0600")));
        ReflectionUtil.setFieldValue(propA, "mValue", cal);
        return propA;
    }

    public static Property createModelObjectArrayPropertyDescription(ModelObject[] propVal) throws Exception {
        PropertyDescriptionImpl propDesc = Mockito.mock(PropertyDescriptionImpl.class);
        Mockito.when(propDesc.getType()).thenReturn(9);
        Mockito.when(propDesc.isArray()).thenReturn(true);
        Property propA = new PropertyModelObjectArrayImpl(propDesc, Arrays.asList(createPV("value")), Mockito.mock(ClientDataModel.class));
        ReflectionUtil.setFieldValue(propA, "mValues", propVal);
        return propA;
    }

    private static PropertyValue createPV(String value) {
        PropertyValue pv = Mockito.mock(PropertyValue.class);
        Mockito.when(pv.getValue()).thenReturn(value);
        return pv;
    }

    public static void addProperty(ModelObject obj, String propertyName, Property prop) throws Exception {
        if (obj instanceof com.eingsoft.emop.tc.model.ModelObject) {
            obj = ((com.eingsoft.emop.tc.model.ModelObject) obj).unproxy();
        }
        Hashtable<String, Property> properties = (Hashtable<String, Property>) ReflectionUtil.getFieldValue(obj, "m_props");
        properties.put(propertyName, prop);
    }

    public static ModelObject createModelObject(String type, String uid) {
        Type t = Mockito.mock(Type.class);
        Mockito.when(t.getName()).thenReturn(type);
        return new ModelObjectImpl(t, uid);
    }

    public static <T extends ModelObjectImpl> T createModelObject(String type, String uid, Class<T> clz) throws Exception {
        Type t = Mockito.mock(Type.class);
        Mockito.when(t.getName()).thenReturn(type);
        return clz.getConstructor(Type.class, String.class).newInstance(t, uid);
    }

    public static BOMLine createBOMLine(String uid, BOMLine[] children, ModelObject item, ModelObject itemRevision, TcContextHolder holder) throws Exception {
        BOMLine bl = mock(BOMLine.class);
        Type type = mock(Type.class);
        when(type.getName()).thenReturn("BOMLine");
        when(bl.getTypeObject()).thenReturn(type);
        when(bl.getUid()).thenReturn(uid);
        Property propertyChildren = createModelObjectArrayPropertyDescription(children);
        when(bl.getPropertyObject("bl_all_child_lines")).thenReturn(propertyChildren);
        Property propertyItem = createModelObjectPropertyDescription(item);
        when(bl.getPropertyObject("bl_item")).thenReturn(propertyItem);
        Property propertyItemRev = createModelObjectPropertyDescription(itemRevision);
        when(bl.getPropertyObject("bl_revision")).thenReturn(propertyItemRev);
        Property propertyTitle = createStringPropertyDescription("title");
        when(bl.getPropertyObject("bl_indented_title")).thenReturn(propertyTitle);
        return (BOMLine) spy(bl, holder);
    }

    public static TcContextHolder createTcContextHolder() {
        TcContextHolder holder = mock(TcContextHolder.class);
        TcLoadService loadService = mock(TcLoadService.class);
        when(holder.getTcLoadService()).thenReturn(loadService);
        TcBOMService bomService = mock(TcBOMService.class);
        when(holder.getTcBOMService()).thenReturn(bomService);
        TcBOPService bopService = mock(TcBOPService.class);
        when(holder.getTcBOPService()).thenReturn(bopService);
        when(loadService.loadProperties((ModelObject) any())).thenAnswer(a -> ProxyUtil.proxy((ModelObject) a.getArguments()[0], holder));
        when(holder.getTcLoadService().loadProperties((List<ModelObject>) any())).thenAnswer(i -> i.getArguments()[0]);
        return holder;
    }

    public static void initConnectionBuilderInfo() {
        System.setProperty("tc.protocol", "http");
        System.setProperty("tc.host", "192.168.1.64");
        System.setProperty("tc.port", "7001");
        System.setProperty("tc.appName", "tc");
        System.setProperty("tc.pooled", "false");
    }
}

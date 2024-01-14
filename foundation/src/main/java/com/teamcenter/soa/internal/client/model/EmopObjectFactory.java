package com.teamcenter.soa.internal.client.model;

import com.eingsoft.emop.tc.util.ModelObjectUtil;
import com.teamcenter.soa.client.model.ClientDataModel;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.Property;
import com.teamcenter.soa.client.model.PropertyDescription;
import com.teamcenter.soa.internal.client.model.DefaultObjectFactory;
import com.teamcenter.soa.internal.client.model.ModelObjectImpl;
import com.teamcenter.soa.internal.client.model.PropertyDescriptionImpl;
import com.teamcenter.soa.internal.client.model.PropertyImpl;

public class EmopObjectFactory extends DefaultObjectFactory {

    @Override
    public Property addProperty(ModelObject var1, PropertyDescription var2,
        com.teamcenter.schemas.soa._2006_03.base.Property var3, ClientDataModel var4) {
        boolean var5 = var3.isSetModifiable() ? var3.isModifiable() : var2.isModifiable();
        PropertyImpl var6 = PropertyImpl.createPropertyObject(var3.getUiValues(), var3.getUiValue(), var3.getValues(), var5, (PropertyDescriptionImpl)var2, var4);
        ModelObjectUtil.removeNullElements(var6);
        ((ModelObjectImpl)var1).addProperty(var2.getName(), var6);
        return var6;
    }
}

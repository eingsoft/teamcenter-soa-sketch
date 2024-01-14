package com.eingsoft.emop.tc.util;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Objects;

import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.Property;
import com.teamcenter.soa.internal.client.model.ModelObjectImpl;
import com.teamcenter.soa.internal.client.model.PropertyModelObjectArrayImpl;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ModelObjectUtil {

    public static void removeNullElementsInObjectArrayProperty(ModelObject modelObject) {
        if (modelObject == null) {
            return;
        }
        if (modelObject instanceof com.eingsoft.emop.tc.model.ModelObject) {
            modelObject = ProxyUtil.unproxy(modelObject);
        }
        if (modelObject instanceof ModelObjectImpl) {
            Hashtable<String, Property> properties = ((ModelObjectImpl)modelObject).copyProperties();
            for (Entry<String, Property> entry : properties.entrySet()) {
                removeNullElements(entry.getValue());
            }
        }
    }

    public static void removeNullElements(Property property) {
        try {
            if (property != null && property instanceof PropertyModelObjectArrayImpl) {
                ModelObject[] mValues = (ModelObject[])ReflectionUtil.getFieldValue(property, "mValues");
                if (mValues != null) {
                    // EMOP-2574 avoid TC soa library bug: NPE
                    int originalCount = mValues.length;
                    mValues = Arrays.stream(mValues).filter(Objects::nonNull).toArray(ModelObject[]::new);
                    if (mValues.length != originalCount) {
                        log.debug("reset PropertyModelObjectArrayImpl properties, removed "
                            + (originalCount - mValues.length) + " null elements in the properties.");
                        ReflectionUtil.setFieldValue(property, "mValues", mValues);
                    }
                }
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
        }
    }
}

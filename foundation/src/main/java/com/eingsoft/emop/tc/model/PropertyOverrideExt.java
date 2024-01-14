package com.eingsoft.emop.tc.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.NonNull;

/**
 * use a customized class to derive property value in runtime, and override(change) existing properties value, pay
 * attention that, it won't really change the property value of the given {@link ModelObject}.
 * 
 * here is a sample usage:
 * 
 * <pre>
 * public abstract class PackBOMLineExt extends PropertyOverrideExt {
 * 
 *     private String packedQuantity = &quot;&quot;;
 * 
 *     public PackBOMLineExt(@NonNull ModelObject modelObject) {
 *         super(modelObject);
 *     }
 * 
 *     public void addQuantity(BOMLineExt line) {
 *         Double quantity1 = Optional.ofNullable(Doubles.tryParse(this.packedQuantity)).orElse(1.0);
 *         Double quantity2 = Optional.ofNullable(Doubles.tryParse(line.packedQuantity)).orElse(1.0);
 *         packedQuantity = Double.toString(quantity1 + quantity2);
 *         set("bl_quantity", packedQuantity);
 *     }
 * }
 * 
 * ModelObject bomline1 = ...//load bomline from Teamcenter service
 * ModelObject bomline2 = ...//load bomline from Teamcenter service
 * BOMLineExt bomlineExt1 = ModelObjectExtensionUtil.ext(bomline1, BOMLineExt.class);
 * BOMLineExt bomlineExt2 = ModelObjectExtensionUtil.ext(bomline2, BOMLineExt.class);
 * 
 * bomlineExt1.addQuantity(bomlineExt2);
 * 
 * Assert.assertEquals("2.0", bomlineExt1.get("bl_quantity"));
 * </pre>
 * 
 * @author beam
 *
 */
public abstract class PropertyOverrideExt implements ModelObjectExt {

    /**
     * keep the override properties values
     */
    private final Map<String, Object> overrideProperties = new HashMap<String, Object>();

    @Getter
    private final ModelObject modelObject;

    public PropertyOverrideExt(@NonNull ModelObject modelObject) {
        this.modelObject = modelObject;
    }

    @Override
    public Object get(@NonNull String propertyName) {
        if (overrideProperties.containsKey(propertyName)) {
            return overrideProperties.get(propertyName);
        } else {
            return getModelObject().get(propertyName);
        }
    }

    protected Object set(@NonNull String propertyName, Object val) {
        return overrideProperties.put(propertyName, val);
    }
}

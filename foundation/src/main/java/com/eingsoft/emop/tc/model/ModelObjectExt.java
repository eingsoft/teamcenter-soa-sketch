package com.eingsoft.emop.tc.model;

import lombok.NonNull;

/**
 * A {@link ModelObject} extension class should implement this class, the extension class will extend all the behavior
 * of given {@link ModelObject}, at the same time, provide additional functionalities in the extension class, the
 * extension class should meet below conditions:
 * 
 * 1. it could be an abstract to avoid unnecessary override of {@link ModelObject} interface
 * 
 * 2. it should provide a constructor receiving an instance of {@link ModelObject}
 * 
 * 3. it should implement method of getModelObject(), and return the received instance from constructor
 * 
 * <pre>
 * public abstract class PackBOMLineExt extends BOMLine implements ModelObjectExt {
 * 
 *     private String packedQuantity = &quot;&quot;;
 *     &#064;Getter
 *     private final ModelObject modelObject;
 * 
 *     public PackBOMLineExt(ModelObject modelObject) {
 *         super(ReflectionUtil.getTypeFromModelObject(modelObject), modelObject.getUid());
 *         this.modelObject = modelObject;
 *     }
 * 
 *     public void addQuantity(PackBOMLineExt2 line) {
 *         Double quantity1 = Optional.ofNullable(Doubles.tryParse(this.packedQuantity)).orElse(1.0);
 *         Double quantity2 = Optional.ofNullable(Doubles.tryParse(line.packedQuantity)).orElse(1.0);
 *         packedQuantity = Double.toString(quantity1 + quantity2);
 *     }
 * 
 *     &#064;Override
 *     public Object get(@NonNull String propertyName) {
 *         if (PROP_BL_QUANTITY.equals(propertyName)) {
 *             return packedQuantity;
 *         } else {
 *             return getModelObject().get(propertyName);
 *         }
 *     }
 * }
 * 
 * ModelObject bomline1 = ...//load bomline from Teamcenter service
 * ModelObject bomline2 = ...//load bomline from Teamcenter service
 * BOMLineExt bomlineExt1 = ModelObjectExtensionUtil.ext(bomline1, BOMLineExt.class);
 * BOMLineExt bomlineExt2 = ModelObjectExtensionUtil.ext(bomline2, BOMLineExt.class);
 * 
 * Assert.assertEquals("1.0", bomlineExt1.get_bl_quantity()); //actually load bl_quantity from the target ModelObject which is bomline1
 * Assert.assertEquals("1.0", bomlineExt2.get_bl_quantity()); //actually load bl_quantity from the target ModelObject which is bomline2
 *         
 * bomlineExt1.addQuantity(bomlineExt2);
 * 
 * Assert.assertEquals("2.0", bomlineExt1.get("bl_quantity"));
 * </pre>
 * 
 * Above sample could also be simplified using {@link PropertyOverrideExt}, please refer to {@link PropertyOverrideExt}
 * for more detail.
 * 
 * @author beam
 *
 */
public interface ModelObjectExt extends ModelObject {

    public static final String METHOD_GETMODELOBJECT = "getModelObject";

    @NonNull
    ModelObject getModelObject();
}

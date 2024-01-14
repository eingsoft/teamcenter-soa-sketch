package com.eingsoft.emop.tc.util;

import static com.eingsoft.emop.tc.BMIDE.PROP_BL_QUANTITY;
import static com.eingsoft.emop.tc.util.MockDataUtil.addProperty;
import static com.eingsoft.emop.tc.util.MockDataUtil.createModelObject;
import static com.eingsoft.emop.tc.util.MockDataUtil.createStringPropertyDescription;
import static com.eingsoft.emop.tc.util.MockDataUtil.createTcContextHolder;
import static com.eingsoft.emop.tc.util.ModelObjectExtensionUtil.ext;
import static com.eingsoft.emop.tc.util.ProxyUtil.spy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import lombok.Getter;
import lombok.NonNull;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.model.ModelObjectExt;
import com.eingsoft.emop.tc.model.PropertyOverrideExt;
import com.google.common.primitives.Doubles;
import com.teamcenter.soa.client.model.Type;
import com.teamcenter.soa.client.model.strong.BOMLine;

public class ModelObjectExtensionUtilTest {

    private static final double BL_QUANTITY_DEF_VAL = 1.0;

    @Test
    public void testPropertyOverrideExt() {
        ModelObject bl1 = ProxyUtil.spy(createModelObject("BOMLine", "BOM::12673"), createTcContextHolder());
        ModelObject bl2 = ProxyUtil.spy(createModelObject("BOMLine", "BOM::12674"), createTcContextHolder());
        ModelObject bl3 = ProxyUtil.spy(createModelObject("BOMLine", "BOM::12675"), createTcContextHolder());

        PackBOMLineExt bomline1 = ext(bl1, PackBOMLineExt.class);
        PackBOMLineExt bomline2 = ext(bl2, PackBOMLineExt.class);
        PackBOMLineExt bomline3 = ext(bl3, PackBOMLineExt.class);

        bomline1.addQuantity(bomline2);
        bomline1.addQuantity(bomline3);

        Assert.assertEquals("3.0", bomline1.get(PROP_BL_QUANTITY));
    }

    static public abstract class PackBOMLineExt extends PropertyOverrideExt {

        private String packedQuantity = "";

        public PackBOMLineExt(@NonNull ModelObject modelObject) {
            super(modelObject);
        }

        public void addQuantity(PackBOMLineExt line) {
            Double quantity1 = Optional.ofNullable(Doubles.tryParse(this.packedQuantity)).orElse(BL_QUANTITY_DEF_VAL);
            Double quantity2 = Optional.ofNullable(Doubles.tryParse(line.packedQuantity)).orElse(BL_QUANTITY_DEF_VAL);
            packedQuantity = Double.toString(quantity1 + quantity2);
            set(PROP_BL_QUANTITY, packedQuantity);
        }
    }

    @Test
    public void testPropertyOverrideExt2() {
        ModelObject bl1 = ProxyUtil.spy(createModelObject("BOMLine", "BOM::12673"), createTcContextHolder());
        ModelObject bl2 = ProxyUtil.spy(createModelObject("BOMLine", "BOM::12674"), createTcContextHolder());

        PackBOMLineExt1 bomline1 = ext(bl1, PackBOMLineExt1.class);
        PackBOMLineExt1 bomline2 = ext(bl2, PackBOMLineExt1.class);

        bomline1.addQuantity(bomline2);

        Assert.assertEquals("2.0", bomline1.get(PROP_BL_QUANTITY));
    }

    static public abstract class PackBOMLineExt1 extends PackBOMLineExt {
        public PackBOMLineExt1(@NonNull ModelObject modelObject) {
            super(modelObject);
        }
    }

    @Test
    public void testModelObjectExt() throws Exception {

        ModelObject bl1 = createBOMLine(0);
        ModelObject bl2 = createBOMLine(1);
        ModelObject bl3 = createBOMLine(2);

        Assert.assertEquals("title0", bl1.get("bl_indented_title"));
        Assert.assertEquals("title1", bl2.get("bl_indented_title"));
        Assert.assertEquals("title2", bl3.get("bl_indented_title"));

        PackBOMLineExt2 bomline1 = ext(bl1, PackBOMLineExt2.class);
        PackBOMLineExt2 bomline2 = ext(bl2, PackBOMLineExt2.class);
        PackBOMLineExt2 bomline3 = ext(bl3, PackBOMLineExt2.class);

        Assert.assertEquals("title0", bomline1.get("bl_indented_title"));
        Assert.assertEquals("title1", bomline2.get("bl_indented_title"));
        Assert.assertEquals("title2", bomline3.get("bl_indented_title"));

        Assert.assertEquals("title0", bomline1.get_bl_indented_title());
        Assert.assertEquals("title1", bomline2.get_bl_indented_title());
        Assert.assertEquals("title2", bomline3.get_bl_indented_title());

        bomline1.addQuantity(bomline2);
        bomline1.addQuantity(bomline3);

        Assert.assertEquals("3.0", bomline1.get(PROP_BL_QUANTITY));
    }

    private ModelObject createBOMLine(int i) throws Exception {
        Type type = Mockito.mock(Type.class);
        when(type.getName()).thenReturn("BOMLine");
        BOMLine item = new BOMLine(type, "BOM::1267" + i);
        addProperty(item, "bl_indented_title", createStringPropertyDescription("title" + i));
        return spy(item, createTcContextHolder());
    }

    static public abstract class PackBOMLineExt2 extends BOMLine implements ModelObjectExt {

        private String packedQuantity = "";
        @Getter
        private final ModelObject modelObject;

        public PackBOMLineExt2(ModelObject modelObject) {
            super(ReflectionUtil.getTypeFromModelObject(modelObject), modelObject.getUid());
            this.modelObject = modelObject;
        }

        public void addQuantity(PackBOMLineExt2 line) {
            Double quantity1 = Optional.ofNullable(Doubles.tryParse(this.packedQuantity)).orElse(BL_QUANTITY_DEF_VAL);
            Double quantity2 = Optional.ofNullable(Doubles.tryParse(line.packedQuantity)).orElse(BL_QUANTITY_DEF_VAL);
            packedQuantity = Double.toString(quantity1 + quantity2);
        }

        @Override
        public Object get(@NonNull String propertyName) {
            if (PROP_BL_QUANTITY.equals(propertyName)) {
                return packedQuantity;
            } else {
                return getModelObject().get(propertyName);
            }
        }
    }

    @Test
    public void testInvalidConstructor() {
        ModelObject bl1 = ProxyUtil.spy(createModelObject("BOMLine", "BOM::12673"), createTcContextHolder());
        try {
            ext(bl1, InvalidConstructorExt.class);
            throw new RuntimeException("not reachable");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("failed to create ModelObjectExt of"));
        }
    }

    static public abstract class InvalidConstructorExt implements ModelObjectExt {}

    @Test
    public void testShouldImplementGetModelObjectMethod() {
        ModelObject bl1 = ProxyUtil.spy(createModelObject("BOMLine", "BOM::12673"), createTcContextHolder());
        try {
            ShouldImplementGetModelObjectMethodExt bl = ext(bl1, ShouldImplementGetModelObjectMethodExt.class);
            bl.getObjectName();
            throw new RuntimeException("not reachable");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("should provide the implementation of method getModelObject"));
        }
    }

    static public abstract class ShouldImplementGetModelObjectMethodExt implements ModelObjectExt {
        public ShouldImplementGetModelObjectMethodExt(ModelObject obj) {}
    }

    @Test
    public void testShouldReturnNonNullModelObject() {
        ModelObject bl1 = ProxyUtil.spy(createModelObject("BOMLine", "BOM::12673"), createTcContextHolder());
        try {
            ShouldReturnNonNullModelObjectExt bl = ext(bl1, ShouldReturnNonNullModelObjectExt.class);
            bl.getObjectName();
            throw new RuntimeException("not reachable");
        } catch (NullPointerException e) {
            Assert.assertTrue(e.getMessage().contains("encountered null when invoking getModelObject from"));
        }
    }

    static public abstract class ShouldReturnNonNullModelObjectExt implements ModelObjectExt {
        public ShouldReturnNonNullModelObjectExt(ModelObject obj) {}

        public ModelObject getModelObject() {
            return null;
        }
    }
}

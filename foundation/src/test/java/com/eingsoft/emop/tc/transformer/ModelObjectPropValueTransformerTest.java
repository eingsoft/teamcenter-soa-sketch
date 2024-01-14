package com.eingsoft.emop.tc.transformer;

import static com.eingsoft.emop.tc.util.MockDataUtil.addProperty;
import static com.eingsoft.emop.tc.util.MockDataUtil.createStringPropertyDescription;
import static com.eingsoft.emop.tc.util.MockDataUtil.createTcContextHolder;
import static com.eingsoft.emop.tc.util.ProxyUtil.spy;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.math.NumberUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.teamcenter.soa.client.model.Type;
import com.teamcenter.soa.client.model.strong.BOMLine;
import com.teamcenter.soa.client.model.strong.Item;

public class ModelObjectPropValueTransformerTest {

    private ModelObjectPropValueTransformer transformer;
    private ModelObjectPropValueTransformer transformer2;
    private ModelObjectPropValueTransformer removeLastZeros;
    private TcContextHolder tcContextHolder;

    @Before
    public void setup() {
        transformer = new ModelObjectPropValueTransformer() {
            @Override
            public Object transform(ModelObject modelObject, String propertyName, Object originalResult) {
                return "Transformed By transformer1: " + originalResult;
            }

            @Override
            public int getPriority() {
                return 2;
            }

            @Override
            public String getName() {
                return "transformer1";
            }
        };
        transformer2 = new ModelObjectPropValueTransformer() {
            @Override
            public Object transform(ModelObject modelObject, String propertyName, Object originalResult) {
                return "Transformed By transformer2: " + originalResult;
            }

            @Override
            public int getPriority() {
                return 0;
            }

            @Override
            public String getName() {
                return "transformer2";
            }
        };
        removeLastZeros = new ModelObjectPropValueTransformer() {
            @Override
            public Object transform(ModelObject modelObject, String propertyName, Object originalResult) {
				if (originalResult != null && "BOMLine".equals(modelObject.getTypeObject().getName())
						&& "bl_quantity".equals(propertyName)) {
					String str = originalResult.toString();
					if (NumberUtils.isCreatable(str)) {
						return str.indexOf(".") < 0 ? str : str.replaceAll("0*$", "").replaceAll("\\.$", "");
					}
				}
				return originalResult;
			}
            
            @Override
            public int getPriority() {
                return 5;
            }

            @Override
            public String getName() {
                return "removeLastZerosTransformer";
            }
        };
        tcContextHolder = createTcContextHolder();
    }

    @After
    public void teardown() {
        ModelObjectPropValueTransformerChain.getInstance().unregisterGlobally(transformer);
        ModelObjectPropValueTransformerChain.getInstance().unregisterGlobally(transformer2);
        ModelObjectPropValueTransformerChain.getInstance().unregisterGlobally(removeLastZeros);
        // all the request level transformers will be unregistered
        SOAExecutionContext.current().cleanupSiliently();
    }

    @Test
    public void testMultiTransformers() throws Exception {
        ModelObjectPropValueTransformerChain.getInstance().registerGlobally(transformer2);
        ModelObjectPropValueTransformerChain.getInstance().registerRequestLevel(transformer);
        // add it again, but only one will take affect finally
        ModelObjectPropValueTransformerChain.getInstance().registerGlobally(transformer);
        ModelObjectPropValueTransformerChain.getInstance().registerRequestLevel(removeLastZeros);

        Type type = Mockito.mock(Type.class);
        when(type.getName()).thenReturn("Item");
        Item item = new Item(type, "UidA");
        addProperty(item, "fnd0OriginalLocationCode", createStringPropertyDescription("1.02000"));

        ModelObject obj = spy(item, tcContextHolder);

        /**
         * I cannot transform on the TC raw ModelObject
         */
        Assert.assertEquals("1.02000", item.get_fnd0OriginalLocationCode());
        /**
         * I can transform on the proxied EMOP ModelObject
         */
        Assert.assertEquals("Transformed By transformer2: Transformed By transformer1: 1.02000",
            ((Item)obj).get_fnd0OriginalLocationCode());
    }

    @Test
    public void testRemoveLastZerosTransformer() throws Exception {
        ModelObjectPropValueTransformerChain.getInstance().registerRequestLevel(removeLastZeros);

        Type type = Mockito.mock(Type.class);
        when(type.getName()).thenReturn("BOMLine");
        BOMLine bomline = new BOMLine(type, "UidA");
        addProperty(bomline, "bl_quantity", createStringPropertyDescription("1.02000"));

        ModelObject obj = spy(bomline, tcContextHolder);

        /**
         * I cannot transform on the TC raw ModelObject
         */
        Assert.assertEquals("1.02000", bomline.get_bl_quantity());
        /**
         * I can transform on the proxied EMOP ModelObject
         */
        Assert.assertEquals("1.02", ((BOMLine)obj).get_bl_quantity());
        Assert.assertEquals("1.02", obj.get("bl_quantity"));
    }

    @Test
    public void testRemoveLastZerosTransformer2() throws Exception {
        ModelObjectPropValueTransformerChain.getInstance().registerRequestLevel(removeLastZeros);

        Type type = Mockito.mock(Type.class);
        when(type.getName()).thenReturn("BOMLine");
        BOMLine bomline = new BOMLine(type, "UidA");
        addProperty(bomline, "bl_quantity", createStringPropertyDescription("A1.02000"));

        ModelObject obj = spy(bomline, tcContextHolder);

        /**
         * I cannot transform on the TC raw ModelObject
         */
        Assert.assertEquals("A1.02000", bomline.get_bl_quantity());
        /**
         * I can transform on the proxied EMOP ModelObject, but it is not a number, return back the original result
         */
        Assert.assertEquals("A1.02000", ((BOMLine)obj).get_bl_quantity());
        Assert.assertEquals("A1.02000", obj.get("bl_quantity"));
    }
}

package com.eingsoft.emop.tc.service.impl;

import com.eingsoft.emop.tc.BMIDE;
import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.service.BOMPreloadConfig;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcObjectPropertyLoadStatService;
import com.teamcenter.soa.client.model.strong.BOMLine;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.eingsoft.emop.tc.util.MockDataUtil.*;

public class TcBOMServiceTest {

    private TcContextHolder holder = createTcContextHolder();

    @Before
    public void init() {
        TcObjectPropertyLoadStatService.getInstance().cleanUp();
        System.setProperty("tc.protocol", "http");
        System.setProperty("tc.host", "192.168.52.99");
        System.setProperty("tc.port", "7001");
        System.setProperty("tc.appName", "tc");
        System.setProperty("tc.username", "king");
        System.setProperty("tc.password", "king");
        System.setProperty("tc.pooled", "false");
        SOAExecutionContext.current().initWithoutPool("king", "king");
    }

    @After
    public void teardown() {
        SOAExecutionContext.current().cleanupSiliently();
    }

    @Test
    public void testSimplestPreLoadBOM() throws Exception {
        BOMLine bl1 = createBOMLine("bl1", new BOMLine[]{}, null, null, holder);
        TcBOMServiceImpl service = new TcBOMServiceImpl(holder);
        service.preLoadBOM(bl1, new BOMPreloadConfig() {
            @Override
            public boolean isItemInBOMLineNeedInitializeProperties() {
                return false;
            }

            @Override
            public boolean isItemRevisionInBOMLineNeedInitializeProperties() {
                return false;
            }
        });
        Assert.assertEquals(1, SOAExecutionContext.current().getModelObjectCache().asMap().size());
        List<String> cachedAttrs = TcObjectPropertyLoadStatService.getInstance().getCache().get("BOMLine");
        Assert.assertEquals(2, cachedAttrs.size());
        // retrieve bl_all_child_lines, and the debug purpose of bl_indented_title
        Assert.assertTrue(cachedAttrs.contains("bl_all_child_lines"));
        Assert.assertTrue(cachedAttrs.contains("bl_indented_title"));
    }

    @Test
    public void testOneLevelPreLoadBOM() throws Exception {
        BOMLine bl1 = createBOMLine("bl1", new BOMLine[]{}, createModelObject("Item", "UidA"), createModelObject("ItemRevision", "UidB"), holder);
        TcBOMServiceImpl service = new TcBOMServiceImpl(holder);
        service.preLoadBOM(bl1, new BOMPreloadConfig() {
        });
        Assert.assertEquals(3, SOAExecutionContext.current().getModelObjectCache().asMap().size());
        List<String> cachedAttrs = TcObjectPropertyLoadStatService.getInstance().getCache().get("BOMLine");
        Assert.assertEquals(4, cachedAttrs.size());
        Assert.assertTrue(cachedAttrs.contains("bl_all_child_lines"));
        Assert.assertTrue(cachedAttrs.contains("bl_item"));
        Assert.assertTrue(cachedAttrs.contains("bl_revision"));
        Assert.assertTrue(cachedAttrs.contains("bl_indented_title"));
        System.out.println(TcObjectPropertyLoadStatService.getInstance());
    }

    @Test
    public void testTwoLevelPreLoadBOM() throws Exception {
        BOMLine bl21 = createBOMLine("bl21", new BOMLine[]{}, createModelObject("Item", "Uid21A"), createModelObject("ItemRevision", "Uid21B"), holder);
        BOMLine bl22 = createBOMLine("bl22", new BOMLine[]{}, createModelObject("Item", "Uid22A"), createModelObject("ItemRevision", "Uid22B"), holder);
        BOMLine bl1 = createBOMLine("bl1", new BOMLine[]{bl21, bl22}, createModelObject("Item", "Uid1A"), createModelObject("ItemRevision", "Uid1B"), holder);
        TcBOMServiceImpl service = new TcBOMServiceImpl(holder);
        service.preLoadBOM(bl1, new BOMPreloadConfig() {
        });
        Assert.assertEquals(9, SOAExecutionContext.current().getModelObjectCache().asMap().size());
        List<String> cachedAttrs = TcObjectPropertyLoadStatService.getInstance().getCache().get("BOMLine");
        Assert.assertEquals(4, cachedAttrs.size());
        // retrieve bl_all_child_lines * 3, bl_item *3 , bl_revision *3 , and the debug purpose of bl_indented_title * 3
        Assert.assertTrue(cachedAttrs.contains("bl_all_child_lines"));
        Assert.assertTrue(cachedAttrs.contains("bl_item"));
        Assert.assertTrue(cachedAttrs.contains("bl_revision"));
        Assert.assertTrue(cachedAttrs.contains("bl_indented_title"));
    }
}

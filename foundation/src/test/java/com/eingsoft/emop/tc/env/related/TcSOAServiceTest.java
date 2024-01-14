package com.eingsoft.emop.tc.env.related;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.eingsoft.emop.tc.service.impl.SessionIdTcContextHolder;
import com.teamcenter.services.strong.core._2010_09.DataManagement;
import org.junit.*;
import com.eingsoft.emop.tc.BMIDE;
import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.service.CredentialTcContextHolder;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.util.ProxyUtil;
import com.google.common.collect.Lists;
import com.teamcenter.soa.client.model.strong.Fnd0TableRow;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.ItemRevision;

@Ignore
public class TcSOAServiceTest {

  @Before
  public void init() {}

  private void initContext() {
    System.setProperty("tc.protocol", "http");
    System.setProperty("tc.host", "plmwebtest");
    System.setProperty("tc.port", "7001");
    System.setProperty("tc.appName", "tc");
    SOAExecutionContext.current().init("king", "king");
  }

  @After
  public void teardown() {
    // SOAExecutionContext.current().cleanupSiliently();
  }

  @Test
  public void testHasBypass() {
    System.setProperty("tc.protocol", "http");
    System.setProperty("tc.host", "plmwebtest");
    System.setProperty("tc.port", "7001");
    System.setProperty("tc.appName", "tc");
    TcContextHolder context = new SessionIdTcContextHolder("kbmvdlPF1Xry0qNd23VHhpGvKJ7v2Pn26f6DV9d0XkTwy2z0h9T8!269948110");
    SOAExecutionContext.current().init(context);
    TcContextHolder contextHolder = SOAExecutionContext.current().getTcContextHolder();
    contextHolder.getTcSOAService().hasByPass();
  }

  private void printTableInfo(TcContextHolder contextHolder, ModelObject revision) {
    StringBuffer sb = new StringBuffer();
    List<Fnd0TableRow> rows = (List<Fnd0TableRow>) revision.get("jm8_tables_test");
    for (Fnd0TableRow row : rows) {
      // 给表中的行数据 修改对应某行某列的属性
      // contextHolder.getTcSOAService().setProperties(row, "jm8_test_brand", "king");
      ModelObject mo = ProxyUtil.spy(row, contextHolder);
      String brand = (String) mo.get("jm8_test_brand");
      String colour = (String) mo.get("jm8_test_colour");
      sb.append("Brand: ").append(brand).append("\n");
      sb.append("Colour: ").append(colour).append("\n");
    }
    System.out.println("表数据：\n" + sb.toString());
  }

  @Test
  public void testAddRows2Table() {
    initContext();
    TcContextHolder contextHolder = SOAExecutionContext.current().getTcContextHolder();
    Item item = contextHolder.getTcDataManagementService().findItem("001205");
    ItemRevision latestItemRev = contextHolder.getTcDataManagementService().getLatestItemRevision(item);
    ModelObject revision = ProxyUtil.spy(latestItemRev, contextHolder);
    System.out.println("find item revision info: " + revision.getDisplayVal(BMIDE.PROP_OBJECT_STRING));

    // 初始化第一次打印表数据，值应该是空的
    printTableInfo(contextHolder, revision);

    Map<String, String[]> row1 = new HashMap<>();
    row1.put("jm8_test_brand", new String[] {"AAA"});
    row1.put("jm8_test_colour", new String[] {"red"});

    Map<String, String[]> row2 = new HashMap<>();
    row2.put("jm8_test_brand", new String[] {"BBB"});
    row2.put("jm8_test_colour", new String[] {"red"});

    contextHolder.getTcSOAService().addRowsToTable(latestItemRev, "JM8_table_test", "jm8_tables_test", Lists.newArrayList(row1, row2));

    // 添加表数据后 打印表数据，应该是有值
    printTableInfo(contextHolder, revision);
  }

  @Test
  public void testUpadateAndDeleteTableRow() {
    initContext();
    TcContextHolder contextHolder = SOAExecutionContext.current().getTcContextHolder();
    Item item = contextHolder.getTcDataManagementService().findItem("001205");
    ItemRevision latestItemRev = contextHolder.getTcDataManagementService().getLatestItemRevision(item);
    ModelObject revision = ProxyUtil.spy(latestItemRev, contextHolder);
    System.out.println("find item revision info: " + revision.getDisplayVal(BMIDE.PROP_OBJECT_STRING));

    // 查找表中的行数据
    List<Fnd0TableRow> rows = (List<Fnd0TableRow>) revision.get("jm8_tables_test");
    for (Fnd0TableRow row : rows) {
      // 给表中的行数据 修改对应某行某列的属性
      contextHolder.getTcSOAService().setProperties(row, "jm8_test_brand", "king");
      ModelObject mo = ProxyUtil.spy(row, contextHolder);
      String brand = (String) mo.get("jm8_test_brand");

      if ("34344".equals(brand)) {
        // 删除表中行的数据
        contextHolder.getTcDataManagementService().deleteModelObjects(Lists.newArrayList(mo));
      }
    }
  }

  @Test
  public void testCreate() {
    initContext();
    TcContextHolder contextHolder = SOAExecutionContext.current().getTcContextHolder();
    Map<String, Object> propMap = new HashMap<>();
    propMap.put("object_desc", "desc");
    propMap.put("uom_tag", "PCS");
    Map<String, Object> revPropMap = new HashMap<>();
    revPropMap.put("jm8_materialgroup", "1416");
    revPropMap.put("jm8_make_buy_plant", "1003");
    revPropMap.put("jm8_ifpurchase", false);
    revPropMap.put("jm8_ifcbb", true);

    Item item = contextHolder.getTcSOAService().createItems("JM8_Part", "666116-888", "king-uom-tag", "", propMap, revPropMap,
        contextHolder.getTcSOAService().getNewstuffFolder(), BMIDE.REL_CONTENTS);
    Assert.assertNotNull(item);
  }

  @Test
  public void testUpdate() {
    initContext();
    TcContextHolder contextHolder = SOAExecutionContext.current().getTcContextHolder();

    Map<String, List<String>> propMap = new HashMap<>();
    propMap.put("object_name", Lists.newArrayList("king6"));
    propMap.put("uom_tag", Lists.newArrayList("KG"));
    Map<String, List<String>> revPropMap = new HashMap<>();
    revPropMap.put("object_desc", Lists.newArrayList("king-desc"));

    Item item = contextHolder.getTcDataManagementService().findItem("004535");

    DataManagement.PropInfo itemPropInfo = contextHolder.getTcSOAService().buildPropInfo(item, propMap);
    DataManagement.PropInfo revPropInfo = contextHolder.getTcSOAService().buildPropInfo(item, revPropMap);

    contextHolder.getTcSOAService().setProperties(Lists.newArrayList(itemPropInfo, revPropInfo));
    contextHolder.getTcSOAService().setProperties(item, "uom_tag", "L");
  }

  @Test
  public void testWherereference() {
    initContext();
    TcContextHolder contextHolder = SOAExecutionContext.current().getTcContextHolder();
    try {
      ModelObject bomview = contextHolder.getTcLoadService().loadObject("Bkb1j6ntJMlDLB");
      List<ModelObject> revs = contextHolder.getTcSOAService().findWhereReferenced(bomview, null, "ItemRevision", 1);
      revs.stream().forEach(o -> System.out.println(o.getDisplayVal(BMIDE.PROP_OBJECT_STRING)));
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  @Test
  public void testSetReference() {}

  @Test
  public void testCreate2() {
    initContext();
    TcContextHolder contextHolder = SOAExecutionContext.current().getTcContextHolder();
    try {
      contextHolder.getTcLOVService().findUnitOfMeasureWithSymbol("JM8_Valueless", "uom_tag", "XXXXX");
      Assert.fail();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  @Test
  public void testUploadfile() {
    initContext();
    TcContextHolder contextHolder = SOAExecutionContext.current().getTcContextHolder();
    try {
      contextHolder.getTcFileManagementService().uploadFile("QTZx0mTSJMlDLB", "F:/sdf.txt",
          "king" + UUID.randomUUID().toString().substring(8), BMIDE.REL_SPECIFICATION, true);

      contextHolder.getTcFileManagementService().uploadFile("QTZx0mTSJMlDLB", "F:/bom.rar",
          "king" + UUID.randomUUID().toString().substring(8), BMIDE.REL_SPECIFICATION, true);

      contextHolder.getTcFileManagementService().uploadFile("QTZx0mTSJMlDLB", "F:/test.zip",
          "king" + UUID.randomUUID().toString().substring(8), BMIDE.REL_SPECIFICATION, true);

      contextHolder.getTcFileManagementService().uploadFile("QTZx0mTSJMlDLB", "F:/test.log",
          "king" + UUID.randomUUID().toString().substring(8), BMIDE.REL_SPECIFICATION, true);

      contextHolder.getTcFileManagementService().uploadFile("QTZx0mTSJMlDLB", "F:/SOA BMIDE配置步骤.pdf",
          "king" + UUID.randomUUID().toString().substring(8), BMIDE.REL_SPECIFICATION, true);

      contextHolder.getTcFileManagementService().uploadFile("QTZx0mTSJMlDLB", "F:/wo.docx",
          "king" + UUID.randomUUID().toString().substring(8), BMIDE.REL_SPECIFICATION, true);

      contextHolder.getTcFileManagementService().uploadFile("QTZx0mTSJMlDLB", "F:/ex.xlsx",
          "king" + UUID.randomUUID().toString().substring(8), BMIDE.REL_SPECIFICATION, true);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  @Test
  public void testFillItemInput() {
    HashMap<String, Object> map = new HashMap<>();
    map.put("java.lang.String", "1231");
    map.put("java.lang.String[]", new String[] {"123"});
    map.put("java.lang.Boolean", true);
    map.put("java.lang.Boolean[]", new boolean[] {true});
    map.put("java.lang.Integer", 123);
    map.put("int[]", new int[] {1});
    map.put("java.lang.Float", 123f);
    map.put("float[]", new float[] {3f});
    map.put("java.lang.Double", 123.13);
    map.put("double[]", new double[] {123.412});
    map.put("java.util.Date", new Date());
    map.put("java.util.Date[]", new Date[] {});
    map.forEach((k, v) -> {
      switch (v.getClass().getTypeName()) {
        case "java.lang.String":
          Assert.assertTrue("java.lang.String".equals(k));
          break;
        case "java.lang.String[]":
          Assert.assertTrue("java.lang.String[]".equals(k));
          break;
        case "java.lang.Boolean":
          Assert.assertTrue("java.lang.Boolean".equals(k));
          break;
        case "java.lang.Boolean[]":
          Assert.assertTrue("java.lang.Boolean[]".equals(k));
          break;
        case "java.lang.Integer":
          Assert.assertTrue("java.lang.Integer".equals(k));
          break;
        case "int[]":
          Assert.assertTrue("int[]".equals(k));
          break;
        case "java.lang.Float":
          Assert.assertTrue("java.lang.Float".equals(k));
          break;
        case "float[]":
          Assert.assertTrue("float[]".equals(k));
          break;
        case "java.lang.Double":
          Assert.assertTrue("java.lang.Double".equals(k));
          break;
        case "double[]":
          Assert.assertTrue("double[]".equals(k));
          break;
        case "java.util.Date":
          Assert.assertTrue("java.util.Date".equals(k));
          break;
        case "java.util.Date[]":
          Assert.assertTrue("java.util.Date[]".equals(k));
      }
    });

  }

}

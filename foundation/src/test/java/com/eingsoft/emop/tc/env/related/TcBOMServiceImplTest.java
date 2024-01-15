package com.eingsoft.emop.tc.env.related;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import com.eingsoft.emop.tc.BMIDE;
import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.connection.ConnectionBuilderFactory;
import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcContextHolderAware;
import com.eingsoft.emop.tc.service.TcDataManagementService;
import com.eingsoft.emop.tc.service.TcDispatcherManagementService.DatasetInfoUids;
import com.eingsoft.emop.tc.service.TcLoadService;
import com.eingsoft.emop.tc.service.impl.TcBOMServiceImpl;
import com.eingsoft.emop.tc.service.impl.TcFileManagementServiceImpl.FileRetrival;
import com.eingsoft.emop.tc.service.impl.TcFileManagementServiceImpl.FilenameFilter;
import com.eingsoft.emop.tc.util.ProxyUtil;
import com.eingsoft.emop.tc.xpath.XpathHelper;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.strong.core._2013_05.LOV.LOVValueRow;
import com.teamcenter.services.strong.manufacturing.DataManagementService;
import com.teamcenter.services.strong.manufacturing._2011_06.DataManagement.OpenContextInput;
import com.teamcenter.services.strong.manufacturing._2011_06.DataManagement.OpenContextsResponse;
import com.teamcenter.soa.client.model.strong.BOMLine;
import com.teamcenter.soa.client.model.strong.BOMWindow;
import com.teamcenter.soa.client.model.strong.Dataset;
import com.teamcenter.soa.client.model.strong.ImanQuery;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.client.model.strong.MEProcessRevision;
import com.teamcenter.soa.client.model.strong.Mfg0BvrProcess;
import com.teamcenter.soa.client.model.strong.RevisionRule;
import com.teamcenter.soa.exceptions.NotLoadedException;

@Ignore
public class TcBOMServiceImplTest implements TcContextHolderAware {
  private static final String username = "king";
  private static final String password = "king";

  private TcBOMServiceImpl bomService;

  private static final String PRECISE_RULE_NAME = "Precise; Working";

  @Before
  public void setup() {
    System.setProperty("traceSoaUsage", "true");
    ConnectionBuilderFactory.setProperties("10.16.4.138", 7001);
    SOAExecutionContext.current().initWithoutPool(username, password);
    bomService = new TcBOMServiceImpl(getTcContextHolder());
  }

  @After
  public void teardown() {
    System.out.println(SOAExecutionContext.current().getDiagnosticInfo());
    SOAExecutionContext.current().cleanupSiliently();
  }

  @Test
  public void testReplaceBomline() {
    TcDataManagementService tcDataManagementService = getTcContextHolder().getTcDataManagementService();
    ItemRevision topItemRev = tcDataManagementService.findLatestItemRevision("001152");
    ItemRevision replacedRev = tcDataManagementService.findLatestItemRevision("001170");
    ItemRevision replacedRev2 = tcDataManagementService.findLatestItemRevision("001167");
    BOMWindow window = bomService.openBOMWindowWithTopLine(topItemRev);
    ModelObject topLine = ProxyUtil.spy(window, getTcContextHolder()).getModelObject("top_line");
    List<BOMLine> children = (List<BOMLine>) topLine.get(BMIDE.PROP_BL_ALL_CHILD_LINES);
    System.out.println(ProxyUtil.spy(children.get(0), getTcContextHolder()).getDisplayVal(BMIDE.PROP_BL_TITLE));
    bomService.replaceBomLine(children.get(0), replacedRev, 1);
    bomService.saveBOMWindow(window);
    bomService.closeBOMWindow(window);
  }

  @Test
  public void testFindRevisionRule() throws Exception {
    RevisionRule rule = bomService.findRevisionRule(PRECISE_RULE_NAME);
    Assert.assertEquals(PRECISE_RULE_NAME, rule.get_object_name());
  }

  @Test
  public void testDeletedObjects() {
    SOAExecutionContext.current().getTcContextHolder().getTcLoadService()
        .getDeletedObjects(Arrays.asList("k5YxzIqj4bq1aC", "u1TxUgq34bq1aC"));
  }

  @Test
  public void testAllQueries() throws ServiceException {
    TcContextHolder ctxHolder = SOAExecutionContext.current().getTcContextHolder();
    StringBuilder sb = new StringBuilder();

    ctxHolder.getTcQueryService().getQueryByName("QueryTasksByDateRange3");

    sb.append("Dictionary:\n");
    List<ModelObject> objs =
        ctxHolder.getTcQueryService().executeQuery(ctxHolder.getTcQueryService().getQueryByName("__Configuration_Item_Name_or_ID"),
            Arrays.asList("名称", "Item ID"), Arrays.asList("3", "3"), ModelObject.class);
    objs.forEach(
        o -> sb.append(o.getUid() + "," + o.getTypeObject().getName() + "," + o.getObjectName() + "," + o.get("object_desc") + "\n"));

    System.out.println(sb.toString());
  }

  @Test
  public void createQuery() throws ServiceException, NotLoadedException {
    TcContextHolder ctxHolder = SOAExecutionContext.current().getTcContextHolder();
    ctxHolder.getTcQueryService().getQueryByName("__QueryTasksByTimeRange2");
    ImanQuery query = ctxHolder.getTcQueryService().createSavedQuery("__QueryTasksByTimeRange",
        "Query tasks updated during given time range, don't delete it, it is used by EMOP workflow monitor", "EPMTask",
        "SELECT qid FROM EPMTask WHERE \"last_mod_date\" >= \"${lastModDateFrom = }\" AND \"last_mod_date\" < \"${lastModDateTo = }\" ");
    System.out.println(query.get_query_name());
  }

  @Test
  public void testRefreshObjects() {
    for (int i = 0; i < 100; i++) {
      ModelObject obj = bomService.getTcContextHolder().getTcLoadService().loadObject("B9XpdG6o4bq1aC");
      bomService.getTcContextHolder().getTcDataManagementService().refreshObjects(Arrays.asList(obj));
      obj = bomService.getTcContextHolder().getTcLoadService().loadObject("B9XpdG6o4bq1aC");
      System.out.println(obj.get("object_name"));
    }
    System.out.println(SOAExecutionContext.current().getDiagnosticInfo());
  }

  @Test
  public void testOpenBOMWindowUsingRevRule() throws Exception {
    ItemRevision rev = bomService.getTcContextHolder().getTcLoadService().getItemRevision("wpdpPfOJ4bq1aC");
    BOMWindow bomWindow = bomService.openBOMWindowWithTopLine(rev, PRECISE_RULE_NAME);
    ModelObject bomLine = ProxyUtil.spy(bomWindow.get_top_line(), bomService.getTcContextHolder());
    System.out.println(getTcContextHolder().getTcBOMPrintService().pretty(bomLine));
  }

  @Test
  public void testOpenBOPLines() throws Exception {
    ItemRevision rev = bomService.getTcContextHolder().getTcLoadService().getItemRevision("ijUtESw04bq1aC");
    Assert.assertTrue(rev instanceof MEProcessRevision);
    Mfg0BvrProcess bomLine = bomService.getTcContextHolder().getTcBOPService().getTopLine(rev);
    System.out.println(getTcContextHolder().getTcBOMPrintService().pretty(bomLine));
  }

  @Test
  public void testPrettyPrint() throws Exception {
    System.out.println(getTcContextHolder().getTcBOMPrintService().pretty("wpdpPfOJ4bq1aC"));
  }

  @Test
  public void testDispatcherRequest() throws ServiceException {
    getTcContextHolder().getTcDispatcherManagementService()
        .sendCreoToJTConversionRequestByUids(Arrays.asList(new DatasetInfoUids("RuVptn5x4bq1aC", "RyUptn5x4bq1aC")));
  }

  @Test
  public void testGetMultiFilesInDataset() throws NotLoadedException {
    Dataset dataset = (Dataset) getTcContextHolder().getTcLoadService().loadObject("R8Vtqa9h4bq1aC");
    Assert.assertEquals(2, dataset.get_ref_list().length);
  }

  @Test
  @Ignore
  public void testReplaceDatasetFile() {
    getTcContextHolder().getTcFileManagementService().updateFile("R8Vtqa9h4bq1aC", "d:/temp/MyExcel2.xlsx");
  }

  @Test
  public void testLoadBOMThroughOpenContext() {
    SOAExecutionContext.current().disableTCServerCache();
    ItemRevision rev = bomService.getTcContextHolder().getTcLoadService().getItemRevision("TzWtRNvH4bq1aC");
    DataManagementService service = getTcContextHolder().getMfgDataManagementService();
    OpenContextInput input = new OpenContextInput();
    input.object = rev;
    OpenContextsResponse response = service.openContexts(new OpenContextInput[] {input});
    BOMLine topline = (BOMLine) response.serviceData.getCreatedObject(0);
    BOMWindow bomWindow = (BOMWindow) response.serviceData.getCreatedObject(1);
    String retStr = getTcContextHolder().getTcBOMPrintService().pretty(topline, (bomline) -> {
      return bomline.getUid() + "," + bomline.get("bl_quantity");
    });
    service.closeContexts(new com.teamcenter.soa.client.model.ModelObject[] {topline});
    System.out.println(retStr);
  }

  @Test
  public void getInitialLOVValues() {
    List<LOVValueRow> rows = getTcContextHolder().getTcLOVService().getInitialLOVValues("d9Brand", "D9_MaterialRevision");
    Assert.assertTrue(rows.size() > 0);

    rows = getTcContextHolder().getTcLOVService().getInitialLOVValues("d9Texture", "D9_MaterialRevision");
    Assert.assertTrue(rows.size() > 0);
  }

  @Test
  public void testXpath() {
    ModelObject folder = (ModelObject) getTcContextHolder().getTcSOAService().getNewstuffFolder();
    String objType = (String) folder.get("contents[@object_string='测试通知']/@type/name");
    Assert.assertEquals("MSExcel", objType);
    List<String> allSubFolderNames = (List<String>) folder.xpathValues("contents[@type/name='Folder'][empty(@contents)]/@object_string");
    System.out.println(allSubFolderNames);
    ModelObject rootTask = getTcContextHolder().getTcLoadService().loadObject("BFXxsvne4bq1aC");
    String objName = (String) rootTask
        .get("target_attachments[@type/name='D9_MaterialRevision']/IMAN_specification[@type/name='MSExcelX'][position()<2]/object_string");
    System.out.println(objName);
    List<ModelObject> objs = getTcContextHolder().getTcLoadService().loadObjects(Arrays.asList("wxe1kuye4bq1aC", "wxe1kuye4bq1aC",
        "gRS1kyQv4bq1aC", "hqc1Uf$94bq1aC", "iJQ1Uf$94bq1aC", "gRU1k6EP4bq1aC", "gRU1k6G54bq1aC"));
    List<List<?>> result =
        XpathHelper.xpathValuesBatch(objs, "IMAN_specification[@type/name='ProAsm' or @type/name='ProPrt']/object_string");
    System.out.println(result);

    List<?> result2 = XpathHelper.xpathBatch(objs, "IMAN_specification[@type/name='ProAsm']/object_string");
    System.out.println(result2);
  }

  @Test
  public void testRetrieveFile() {
    List<ModelObject> datasets =
        getTcContextHolder().getTcLoadService().loadObjects(Arrays.asList("Aid1kuye4bq1aC", "AKX1kyAX4bq1aC", "AKe1kyQv4bq1aC"));
    Map<ModelObject, List<FileRetrival>> result =
        getTcContextHolder().getTcFileManagementService().retrieveFiles(datasets, new FilenameFilter() {
          @Override
          public boolean accept(String filename) {
            return true;
          }
        });
    System.out.println(result);
  }

  @Test
  public void testOrgnizationService() {
    List<ModelObject> groups = getTcContextHolder().getTcOrgnizationService().getRootGroups();
    groups.forEach(g -> {
      System.out.println(g.getObjectName());
      System.out.println(getTcContextHolder().getTcOrgnizationService().getChildGroups(g.getUid()));
    });
    List<ModelObject> users = getTcContextHolder().getTcOrgnizationService().getGroupMembers("xXJpIL0ixPEQAC", "xrHpILUzxPEQAC");
    users.forEach(u -> System.out.println(u.get("userid")));
  }
}

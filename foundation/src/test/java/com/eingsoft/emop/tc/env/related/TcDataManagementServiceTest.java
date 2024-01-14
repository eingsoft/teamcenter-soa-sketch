package com.eingsoft.emop.tc.env.related;

import com.eingsoft.emop.tc.BMIDE;
import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.service.CredentialTcContextHolder;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcDataManagementService;
import com.eingsoft.emop.tc.service.TcSOAService;
import com.eingsoft.emop.tc.service.impl.TcDataManagementServiceImpl;
import com.eingsoft.emop.tc.service.impl.TcDataManagementServiceImpl.ItemRevInfo;
import com.eingsoft.emop.tc.service.impl.TcDataManagementServiceImpl.ParentRelationInfo;
import com.eingsoft.emop.tc.service.impl.TcDataManagementServiceImpl.RelationInfo;
import com.eingsoft.emop.tc.util.ItemRevIdUtil;
import com.eingsoft.emop.tc.util.ProxyUtil;
import com.google.common.collect.Lists;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.exceptions.NotLoadedException;
import com.teamcenter.soaictstubs.ICCTUserService;
import com.teamcenter.soaictstubs.StringHolder;
import com.teamcenter.soaictstubs.stringSeqValue_u;
import org.junit.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static com.eingsoft.emop.tc.util.MockDataUtil.createModelObject;
import static com.eingsoft.emop.tc.util.MockDataUtil.createTcContextHolder;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@Ignore
public class TcDataManagementServiceTest {

  private TcContextHolder holder = createTcContextHolder();
  private TcDataManagementService tcDMS = new TcDataManagementServiceImpl(holder);

  @Before
  public void init() {}

  private void initContext() {
    System.setProperty("tc.protocol", "http");
    System.setProperty("tc.host", "10.16.4.151");
    System.setProperty("tc.port", "7001");
    System.setProperty("tc.appName", "tc");
    System.setProperty("tc.pooled","false");
    SOAExecutionContext.current().init("king", "king");
  }

  @After
  public void teardown() {
    SOAExecutionContext.current().cleanupSiliently();
  }

  @Test
  public void testRefreshOnlyOnce() {
    SOAExecutionContext.current().init(holder);

    DataManagementService dms = mock(DataManagementService.class);
    ModelObject obj = createModelObject("Item", "uid");
    when(holder.getDataManagementService()).thenReturn(dms);
    tcDMS.refreshObjects(Arrays.asList(obj));
    verify(dms, times(1)).refreshObjects(any());

    tcDMS.refreshObjects(Arrays.asList(obj));
    // still once
    verify(dms, times(1)).refreshObjects(any());
  }

  @Test
  public void testFindItemsByid() throws NotLoadedException {
    initContext();
    TcDataManagementService dataManagementService = SOAExecutionContext.current().getTcContextHolder().getTcDataManagementService();
    List<Item> items = dataManagementService.findItems("0015*");
    for (Item item : items) {
      System.out.println(">>" + item.get_object_string());
    }
    ItemRevision itemRevision = dataManagementService.findLatestItemRevision("000878");
    if (itemRevision != null) {
      System.out.println("Latest Item Revision:" + itemRevision.get_item_id() + "/" + itemRevision.get_item_revision_id() + "-"
          + itemRevision.get_object_name());
    }
    ItemRevision itemRevision2 = dataManagementService.findLatestItemRevision("010438");
    if (itemRevision2 != null) {
      System.out.println("Latest Item Revision2:" + itemRevision2.get_item_id() + "/" + itemRevision2.get_item_revision_id() + "-"
          + itemRevision2.get_object_name());
    }
  }

  @Test
  public void testHandlerCustomService() {
    initContext();
    TcContextHolder contextHolder = SOAExecutionContext.current().getTcContextHolder();
    ICCTUserService userService = new ICCTUserService(contextHolder.getConnection());
    stringSeqValue_u seqValue = new stringSeqValue_u();
    seqValue.seqValue(new String[]{"ASa9kt3O58XUaB,QGW9kt3O58XUaB"});
    StringHolder responseHolder = new StringHolder();
    try {
      userService.callMethod("MATERIALS_MULTI_FORM", seqValue, responseHolder);
      System.out.println(responseHolder.value);
    }catch (Exception e){
      e.printStackTrace();
    }
  }

  @Test
  public void testFindItemRevInfos() throws NotLoadedException {
    initContext();
    TcDataManagementService dataManagementService = SOAExecutionContext.current().getTcContextHolder().getTcDataManagementService();
    Map<String, ItemRevInfo> result = dataManagementService.findItemRevInfoMapByItemIdAndObjectTypes("0000*",
        Lists.newArrayList("EM6_Dcnotice", "EM6_Standardpart", "EM6_Tecnoticedoc"));

    for (String itemId : result.keySet()) {
      System.out.println(itemId + ">>" + result.get(itemId).getItem().get_object_type());
    }
  }

  @Test
  public void testFindItemRevInfoMapByItemIds() throws NotLoadedException {
    initContext();
    TcDataManagementService dataManagementService = SOAExecutionContext.current().getTcContextHolder().getTcDataManagementService();
    Map<String, ItemRevInfo> result = dataManagementService.findItemRevInfoMapByItemIds(Lists.newArrayList("000433", "000432"));

    for (String itemId : result.keySet()) {
      System.out.println(itemId + ">>" + result.get(itemId).getItem().get_object_type());
    }

  }

  @Test
  public void testRevise() throws NotLoadedException {
    initContext();
    TcDataManagementService dataManagementService = SOAExecutionContext.current().getTcContextHolder().getTcDataManagementService();
    ItemRevision rev1 = dataManagementService.findLatestItemRevision("000878");
    ItemRevision rev2 = dataManagementService.findLatestItemRevision("21122-132/002-0001");
    ItemRevision rev3 = dataManagementService.findLatestItemRevision("003777");
    ItemRevision rev4 = dataManagementService.findLatestItemRevision("13G121-0002-9");
    // dataManagementService.revise(rev4);
    // dataManagementService.revise(rev1, "D");
    // dataManagementService.revises(Lists.newArrayList(rev2, rev3));
    // dataManagementService.reviseWithUids(Lists.newArrayList("w2WxI7SLJMlDLB","gtex7RGeJMlDLB","hSet8VAkJMlDLB"));
    // dataManagementService.reviseWithUids(Lists.newArrayList("333", "hSet8VAkJMlDLB"));
  }

  @Test
  public void testChangerOwner() throws NotLoadedException, ServiceException {
    initContext();
    TcDataManagementService dataManagementService = SOAExecutionContext.current().getTcContextHolder().getTcDataManagementService();
    ItemRevision itemRevision = dataManagementService.findLatestItemRevision("017106");
    if (itemRevision != null) {
      System.out.println("Latest Item Revision:" + itemRevision.get_object_string());
    }
    TcSOAService soaService = dataManagementService.getTcContextHolder().getTcSOAService();
    soaService.openByPass();
    dataManagementService.changeOwnership(Lists.newArrayList(itemRevision), soaService.getUser());
    soaService.closeByPass();
  }

  @Test
  public void testFindSpecificItemRev() throws NotLoadedException {
    initContext();
    TcDataManagementService dataManagementService = SOAExecutionContext.current().getTcContextHolder().getTcDataManagementService();
    ItemRevision revA = dataManagementService.findSpecificItemRevision("000878", "A");
    ItemRevision revB = dataManagementService.findSpecificItemRevision("000878", "B");
    ItemRevision revC = dataManagementService.findSpecificItemRevision("000878", "C");
    if (revA != null && revB != null && revC != null) {
      System.out.println("A Item Revision:" + revA.get_object_string());
      System.out.println("B Item Revision:" + revB.get_object_string());
      System.out.println("C Item Revision:" + revC.get_object_string());
    }
  }

  @Test
  public void testSaveAs() throws NotLoadedException {
    initContext();
    TcDataManagementService dataManagementService = SOAExecutionContext.current().getTcContextHolder().getTcDataManagementService();
    ModelObject rev = dataManagementService.loadObject("RJTt$DKxJMlDLB");
    if (rev == null) {
      return;
    }
    ItemRevision revA = dataManagementService.saveAs((ItemRevision) rev, null, null);
    ItemRevision revB = dataManagementService.saveAs((ItemRevision) rev, "110438", null);
    ItemRevision revC = dataManagementService.saveAs((ItemRevision) rev, "110439", "king-save-as");
    if (revA != null && revB != null && revC != null) {
      System.out.println("A Item Revision:" + revA.get_object_string());
      System.out.println("B Item Revision:" + revB.get_object_string());
      System.out.println("C Item Revision:" + revC.get_object_string());
    }
  }

  @Test
  public void testItemRevUtilMethod() throws NotLoadedException {
    Assert.assertEquals(true, ItemRevIdUtil.isFirstRevId("A"));
    Assert.assertEquals("", ItemRevIdUtil.getPreviousRevId("A"));
    Assert.assertEquals("B", ItemRevIdUtil.getNextRevId("A"));
    Assert.assertEquals("Y", ItemRevIdUtil.getPreviousRevId("Z"));
    Assert.assertEquals("AA", ItemRevIdUtil.getNextRevId("Z"));
    Assert.assertEquals(-1, ItemRevIdUtil.compareRevId("A", "C"));
    Assert.assertEquals(0, ItemRevIdUtil.compareRevId("C", "C"));
    Assert.assertEquals(1, ItemRevIdUtil.compareRevId("E", "B"));
  }

  @Test
  public void testPreviouAndNextRev() throws NotLoadedException {
    initContext();
    TcContextHolder contextHolder = SOAExecutionContext.current().getTcContextHolder();
    TcDataManagementService dataManagementService = contextHolder.getTcDataManagementService();
    ItemRevision revA = dataManagementService.findSpecificItemRevision("032259", "A");
    ItemRevision revB = dataManagementService.findSpecificItemRevision("032259", "B");
    ItemRevision revC = dataManagementService.findSpecificItemRevision("032259", "C");
    if (revA != null && revB != null && revC != null) {
      ItemRevision aPreviousRev = dataManagementService.findPreviousItemRevision(revA);
      System.out.println("A Previous Revision:" + aPreviousRev.get_object_string());
      ItemRevision aNextRev = dataManagementService.findNextItemRevision(revA);
      System.out.println("A Next Revision:" + aNextRev.get_object_string());
      ItemRevision bPreviousRev = dataManagementService.findPreviousItemRevision(revB);
      System.out.println("B Previous Revision:" + bPreviousRev.get_object_string());
      ItemRevision bNextRev = dataManagementService.findNextItemRevision(revB);
      System.out.println("B Next Revision:" + bNextRev.get_object_string());
      Assert.assertEquals(null, dataManagementService.findNextItemRevision(revC));
    }
  }



  @Test
  public void tesFindParentAndChildrenWithRels() throws NotLoadedException {
    initContext();
    TcContextHolder contextHolder = SOAExecutionContext.current().getTcContextHolder();
    TcDataManagementService dataManagementService = contextHolder.getTcDataManagementService();
    ModelObject doTask = contextHolder.getTcLoadService().loadObject("hgT1$uQSpMNbrA");
    ModelObject doTask2 = contextHolder.getTcLoadService().loadObject("hRQ1$uQSpMNbrA");
    // root_reference_attachments, root_target_attachments
    List<ParentRelationInfo> parentRelationInfos = dataManagementService.findParentAndChildrenWithRel(Lists.newArrayList(doTask, doTask2),
        "root_reference_attachments", "DocumentRevision,Form");

    for (ParentRelationInfo parentRelationInfo : parentRelationInfos) {
      System.out.println(parentRelationInfo.getParent().getDisplayVal(BMIDE.PROP_OBJECT_STRING));
      for (RelationInfo relationInfo : parentRelationInfo.getRelations()) {
        List<com.eingsoft.emop.tc.model.ModelObject> children = relationInfo.getChildren();
        List<String> childrenStr = children.stream().map(o -> o.getDisplayVal(BMIDE.PROP_OBJECT_STRING)).collect(Collectors.toList());
        System.out.println("relation " + relationInfo.getRelationName() + " with children " + childrenStr.toString());
      }
    }
  }

  public void tesFindChildren() throws NotLoadedException {
    initContext();
    TcContextHolder contextHolder = SOAExecutionContext.current().getTcContextHolder();
    TcDataManagementService dataManagementService = contextHolder.getTcDataManagementService();
    // wpe1QQGXJI9lWB schedule BYV1yNCCJI9lWB EPMTask
    ModelObject doTask = contextHolder.getTcLoadService().loadObject("BYV1yNCCJI9lWB");
    ModelObject schedule = contextHolder.getTcLoadService().loadObject("wpe1QQGXJI9lWB");
    ModelObject item = contextHolder.getTcLoadService().loadObject("glZ1yo5jJI9lWB");
    ModelObject form = contextHolder.getTcLoadService().loadObject("gpV1yo5jJI9lWB");
    Map<com.eingsoft.emop.tc.model.ModelObject, List<com.eingsoft.emop.tc.model.ModelObject>> map =
        dataManagementService.findChildren(Lists.newArrayList(doTask, schedule, item, form));
    for (ModelObject key : map.keySet()) {
      List<com.eingsoft.emop.tc.model.ModelObject> children = map.get(key);
      List<String> childrenStr = children.stream().map(o -> o.getDisplayVal(BMIDE.PROP_OBJECT_STRING)).collect(Collectors.toList());
      System.out.println("Parent " + ProxyUtil.spy(key, contextHolder).getDisplayVal(BMIDE.PROP_OBJECT_STRING) + " with children "
          + childrenStr.toString());
    }
  }

  @Test
  public void tesFindChildrenWithRel() throws NotLoadedException {
    initContext();
    TcContextHolder contextHolder = SOAExecutionContext.current().getTcContextHolder();
    TcDataManagementService dataManagementService = contextHolder.getTcDataManagementService();
    ModelObject doTask = contextHolder.getTcLoadService().loadObject("hgT1$uQSpMNbrA");

    Map<String, List<com.eingsoft.emop.tc.model.ModelObject>> ch = dataManagementService.findChildrenWithRel(doTask);

    for (String key : ch.keySet()) {
      List<com.eingsoft.emop.tc.model.ModelObject> children = ch.get(key);
      List<String> childrenStr = children.stream().map(o -> o.getDisplayVal(BMIDE.PROP_OBJECT_STRING)).collect(Collectors.toList());
      System.out.println("relation " + key + " with children " + childrenStr.toString());
    }

    List<com.eingsoft.emop.tc.model.ModelObject> children =
        dataManagementService.findChildrenWithRel(doTask, "root_reference_attachments", "Form");
    List<String> childrenStr = children.stream().map(o -> o.getDisplayVal(BMIDE.PROP_OBJECT_STRING)).collect(Collectors.toList());
    System.out.println("relation root_reference_attachements with children " + childrenStr.toString());
  }


  public void tesFindChildrenWithRels() throws NotLoadedException {
    initContext();
    TcContextHolder contextHolder = SOAExecutionContext.current().getTcContextHolder();
    TcDataManagementService dataManagementService = contextHolder.getTcDataManagementService();
    ModelObject doTask = contextHolder.getTcLoadService().loadObject("hgT1$uQSpMNbrA");
    ModelObject doTask2 = contextHolder.getTcLoadService().loadObject("hRQ1$uQSpMNbrA");
    // root_reference_attachments, root_target_attachments
    Map<com.eingsoft.emop.tc.model.ModelObject, List<com.eingsoft.emop.tc.model.ModelObject>> parent2childrenMap = dataManagementService
        .findChildrenWithRel(Lists.newArrayList(doTask, doTask2), "root_reference_attachments", "DocumentRevision,Form");

    for (com.eingsoft.emop.tc.model.ModelObject parent : parent2childrenMap.keySet()) {
      System.out.println(parent.getDisplayVal(BMIDE.PROP_OBJECT_STRING));
      List<com.eingsoft.emop.tc.model.ModelObject> children = parent2childrenMap.get(parent);
      List<String> childrenStr = children.stream().map(o -> o.getDisplayVal(BMIDE.PROP_OBJECT_STRING)).collect(Collectors.toList());
      System.out.println("relation root_reference_attachments  with children " + childrenStr.toString());
    }
  }

  @Test
  public void testFindLatestActivePsParent() {
    initContext();
    TcContextHolder contextHolder = SOAExecutionContext.current().getTcContextHolder();
    TcDataManagementService dataManagementService = contextHolder.getTcDataManagementService();
    ModelObject rev = contextHolder.getTcLoadService().loadObject("MCQx25EAJMlDLB");
    Map<String, com.eingsoft.emop.tc.model.ModelObject> map = dataManagementService.findLatestActivePSParentMap(rev);
    for (Entry<String, com.eingsoft.emop.tc.model.ModelObject> entry : map.entrySet()) {
      System.out.println(entry.getKey() + ": " + entry.getValue().getDisplayVal(BMIDE.PROP_OBJECT_STRING));
    }
  }

  @Test
  public void testDeleteRelation() {
    initContext();
    TcContextHolder contextHolder = SOAExecutionContext.current().getTcContextHolder();
    contextHolder.getTcSOAService().openByPass();
    ModelObject rev = contextHolder.getTcLoadService().loadObject("Q$Y1zF1HJMlDLB");
    ModelObject doc = contextHolder.getTcLoadService().loadObject("BMZxPGvxJMlDLB");
    contextHolder.getTcRelationshipService().deleteRelation(rev, doc, "TC_Attaches");
    contextHolder.getTcSOAService().closeByPass();
  }

  @Test
  public void testGenerateItemId() {
    initContext();
    TcContextHolder contextHolder = SOAExecutionContext.current().getTcContextHolder();
    String s1 = contextHolder.getTcDataManagementService().generateItemId("Part");
    String s2 = contextHolder.getTcDataManagementService().generateItemId("Part Revision");
    System.out.println("Part: " + s1);
    System.out.println("PartRevision: " + s2);
  }

}

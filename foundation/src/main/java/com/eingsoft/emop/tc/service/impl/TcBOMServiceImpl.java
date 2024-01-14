package com.eingsoft.emop.tc.service.impl;

import com.eingsoft.emop.tc.BMIDE;
import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.TcSOAServiceDataException;
import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;
import com.eingsoft.emop.tc.service.TcBOMService;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.util.ICCTArgUtil;
import com.eingsoft.emop.tc.util.ProxyUtil;
import com.google.common.collect.Lists;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.internal.strong.core._2011_06.ICT;
import com.teamcenter.services.internal.strong.core._2011_06.ICT.Arg;
import com.teamcenter.services.internal.strong.core._2011_06.ICT.InvokeICTMethodResponse;
import com.teamcenter.services.strong.bom._2008_06.StructureManagement.*;
import com.teamcenter.services.strong.cad.StructureManagementService;
import com.teamcenter.services.strong.cad._2007_01.StructureManagement.CloseBOMWindowsResponse;
import com.teamcenter.services.strong.cad._2007_01.StructureManagement.CreateBOMWindowsResponse;
import com.teamcenter.services.strong.cad._2008_06.StructureManagement.SaveBOMWindowsResponse;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.eingsoft.emop.tc.util.ProxyUtil.proxy;
import static java.util.Collections.emptyList;

@Log4j2
@ScopeDesc(Scope.TcContextHolder)
public class TcBOMServiceImpl extends AbstractTcBOMServiceImpl<BOMLine, ItemRevision> implements TcBOMService {

    public TcBOMServiceImpl(TcContextHolder tcContextHolder) {
        super(tcContextHolder);
    }

    /**
     * 根据参数返回BomWindowResponse， 当前参数为TOP line对应的对象
     *
     * @param modelObject Item or ItemRevision 类型
     * @return
     */
    @Override
    public CreateBOMWindowsResponse
    buildBomWinWithTopLineResponse(com.teamcenter.soa.client.model.ModelObject modelObject, String ruleName) {
        StructureManagementService structureManagementService = this.tcContextHolder.getCADStructureManagementService();
        StructureManagementService.CreateBOMWindowsInfo[] createBOMWindowsInfo =
                new StructureManagementService.CreateBOMWindowsInfo[1];
        createBOMWindowsInfo[0] = new StructureManagementService.CreateBOMWindowsInfo();
        if (modelObject instanceof Item) {
            createBOMWindowsInfo[0].item = (Item) modelObject;
        } else if (modelObject instanceof ItemRevision) {
            ItemRevision itemRev = (ItemRevision) modelObject;
            createBOMWindowsInfo[0].itemRev = itemRev;
            // createBOMWindowsInfo[0].bomView = null; //bom_view_tags from item
            // revision
        } else {
            throw new IllegalArgumentException("modelObject (" + modelObject.getUid()
                    + ") is expected to either Item or ItemRevision class, but it is " + modelObject.getClass().getName());
        }


        if (ruleName != null) {
            RevisionRule rule = null;
            try {
                rule = revRules.get(ruleName, () -> {
                    RevisionRule r = null;
                    try {
                        r = findRevisionRule(ruleName);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("specified the rule name (" + ruleName
                                + ") when opening BOM window, but encountered when finding the rule.", e);
                    }
                    if (r == null) {
                        throw new IllegalArgumentException("specified the rule name (" + ruleName
                                + ") when opening BOM window, but the rule doesn't found.");
                    }
                    return ProxyUtil.proxy(r, tcContextHolder);
                });
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }

            createBOMWindowsInfo[0].revRuleConfigInfo.revRule = rule;
        }
        /*
         * RevisionRuleConfigInfo revisionRuleConfigInfo = new
         * RevisionRuleConfigInfo(); revisionRuleConfigInfo.revRule =
         * tcContextHolder.getTcQueryService().queryLatestWorkRevRule();
         * createBOMWindowsInfo[0].revRuleConfigInfo= revisionRuleConfigInfo;
         */
        CreateBOMWindowsResponse bomWindowsResponse = structureManagementService.createBOMWindows(createBOMWindowsInfo);

        List<BOMWindow> bomWindowList =
                Arrays.stream(bomWindowsResponse.output).map(o -> o.bomWindow).collect(Collectors.toList());

        Set<BOMWindow> bomWindowSet = SOAExecutionContext.current().getOpenedBOMWindow();
        bomWindowSet.addAll(bomWindowList);

        tcContextHolder.printAndLogMessageFromServiceData(bomWindowsResponse.serviceData);

        return bomWindowsResponse;
    }

    /**
     * 根据参数返回BomWindowResponse， 当前参数为TOP line对应的对象
     *
     * @param modelObjects Item or ItemRevision 类型
     * @return
     */
    @Override
    public CreateBOMWindowsResponse buildBomWinWithTopLineResponses(
            List<com.teamcenter.soa.client.model.ModelObject> modelObjects, String ruleName) {
        StructureManagementService structureManagementService = this.tcContextHolder.getCADStructureManagementService();

        List<StructureManagementService.CreateBOMWindowsInfo> createBOMWindowsInfos =
                new ArrayList<>(modelObjects.size());

        for (com.teamcenter.soa.client.model.ModelObject modelObject : modelObjects) {
            StructureManagementService.CreateBOMWindowsInfo info =
                    new StructureManagementService.CreateBOMWindowsInfo();
            if (modelObject instanceof Item) {
                info.item = (Item) modelObject;
            } else if (modelObject instanceof ItemRevision) {
                ItemRevision itemRev = (ItemRevision) modelObject;
                info.itemRev = itemRev;
            } else {
                throw new IllegalArgumentException("modelObject (" + modelObject.getUid()
                        + ") is expected to either Item or ItemRevision class, but it is "
                        + modelObject.getClass().getName());
            }
            if (ruleName != null) {
                RevisionRule rule = null;
                try {
                    rule = revRules.get(ruleName, () -> {
                        RevisionRule r = null;
                        try {
                            r = findRevisionRule(ruleName);
                        } catch (Exception e) {
                            throw new IllegalArgumentException("specified the rule name (" + ruleName
                                    + ") when opening BOM window, but encountered when finding the rule.", e);
                        }
                        if (r == null) {
                            throw new IllegalArgumentException("specified the rule name (" + ruleName
                                    + ") when opening BOM window, but the rule doesn't found.");
                        }
                        return ProxyUtil.proxy(r, tcContextHolder);
                    });
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }

                info.revRuleConfigInfo.revRule = rule;
            }

            createBOMWindowsInfos.add(info);
        }

        CreateBOMWindowsResponse bomWindowsResponse = structureManagementService.createBOMWindows(
                createBOMWindowsInfos.stream().toArray(StructureManagementService.CreateBOMWindowsInfo[]::new));

        List<BOMWindow> bomWindowList =
                Arrays.stream(bomWindowsResponse.output).map(o -> o.bomWindow).collect(Collectors.toList());

        Set<BOMWindow> bomWindowSet = SOAExecutionContext.current().getOpenedBOMWindow();
        bomWindowSet.addAll(bomWindowList);

        tcContextHolder.printAndLogMessageFromServiceData(bomWindowsResponse.serviceData);

        return bomWindowsResponse;
    }

    @Override
    public void closeAllBOMWindow() {
        Set<BOMWindow> bomWindowSet = SOAExecutionContext.current().getOpenedBOMWindow();

        if (!bomWindowSet.isEmpty()) {
            StructureManagementService structureManagementService = tcContextHolder.getCADStructureManagementService();
            BOMWindow[] windows = bomWindowSet.toArray(new BOMWindow[bomWindowSet.size()]);
            structureManagementService.closeBOMWindows(windows);
            bomWindowSet.clear();
        }
    }

    @Override
    public void closeAllBOMWindowSiliently() {
        try {
            closeAllBOMWindow();
        } catch (Exception e) {
            log.error("close all BOMWindow fail", e);
        }
    }

    /**
     * 根据参数创建BOMWindow, 当前参数为TOP line对应的对象, 并且加载 top_line属性
     *
     * @param modelObject Item or ItemRevision 类型
     * @return
     */
    @Override
    public BOMWindow openBOMWindowWithTopLine(com.teamcenter.soa.client.model.ModelObject modelObject,
                                              String ruleName) {
        CreateBOMWindowsResponse bomWindowsResponse = buildBomWinWithTopLineResponse(modelObject, ruleName);
        BOMWindow bomWindow = bomWindowsResponse.output[0].bomWindow;
        return proxy(bomWindow, tcContextHolder);
    }

    @Override
    public BOMWindow openBOMWindowWithTopLine(com.teamcenter.soa.client.model.ModelObject modelObject) {
        return openBOMWindowWithTopLine(modelObject, null);
    }

    @Override
    public BOMLine getTopLine(com.teamcenter.soa.client.model.ModelObject itemRev, String ruleName) {
        CreateBOMWindowsResponse bomWindowsResponse = buildBomWinWithTopLineResponse(itemRev, ruleName);
        BOMLine bomLine = bomWindowsResponse.output[0].bomLine;
        return proxy(bomLine, tcContextHolder);
    }

    @Override
    public void saveBOMWindow(BOMWindow bomWindow) {
        StructureManagementService structureManagementService = this.tcContextHolder.getCADStructureManagementService();
        SaveBOMWindowsResponse saveResponse = structureManagementService.saveBOMWindows(new BOMWindow[]{bomWindow});

        tcContextHolder.printAndLogMessageFromServiceData(saveResponse.serviceData);
    }

    /**
     * @param lines The lines that need to be packed. If pack all option is selected, <br>
     *              the children of the lines will be packed.
     * @param flag  0:pack the lines 1:unpack the lines 2:pack all lines 3:unpack all lines
     */
    private ServiceData packOrUnpack(BOMLine[] lines, int flag) {
        ServiceData serviceData = tcContextHolder.getStructureService().packOrUnpack(lines, flag);
        tcContextHolder.printAndLogMessageFromServiceData(serviceData);
        return serviceData;
    }

    /**
     * 打包BOM行
     *
     * @param lines
     * @return
     */
    @Override
    public List<BOMLine> pack(List<BOMLine> lines) {
        if (lines.isEmpty()) {
            return emptyList();
        }
        ServiceData serviceData = packOrUnpack(lines.toArray(new BOMLine[lines.size()]), 0);
        List<BOMLine> packedBomLines = Lists.newArrayList();
        for (int i = 0; i < serviceData.sizeOfUpdatedObjects(); i++) {
            com.teamcenter.soa.client.model.ModelObject bomLine = serviceData.getUpdatedObject(i);
            if (bomLine instanceof BOMLine) {
                packedBomLines.add(proxy((BOMLine) bomLine, tcContextHolder));
            }
        }

        return packedBomLines.isEmpty() ? emptyList() : packedBomLines;
    }

    @Override
    public void closeBOMWindow(BOMWindow bomWindow) {
        if (bomWindow == null) {
            log.error("BOMWindow must not be null before close it.");
            return;
        }

        StructureManagementService structureManagementService = this.tcContextHolder.getCADStructureManagementService();
        CloseBOMWindowsResponse response = structureManagementService.closeBOMWindows(new BOMWindow[]{bomWindow});
        tcContextHolder.printAndLogMessageFromServiceData(response.serviceData);

        Set<BOMWindow> bomWindowSet = SOAExecutionContext.current().getOpenedBOMWindow();
        bomWindowSet.remove(bomWindow);
    }

    /**
     * 在父结点中添加子BOM行
     *
     * @param parentBOMLine
     * @param childrenItemRevs
     */
    @Override
    public List<BOMLine> addChildrenInBOMLine(BOMLine parentBOMLine, List<ItemRevision> childrenItemRevs) {
        List<BOMLine> bomLines = new ArrayList<BOMLine>();
        try {
            bomLines = addChildrenInBOMLine(parentBOMLine, childrenItemRevs, null);
        } catch (Exception e) {
            throw new TcSOAServiceDataException(e);
        }

        return bomLines;
    }

    /**
     * 在父结点中添加子BOM行， 并且更新属性值, allUpdateAttrs可选。
     *
     * @param parentBOMLine
     * @param childrenItemRevs
     * @param allUpdateAttrs
     * @throws Exception
     */
    @Override
    public List<BOMLine> addChildrenInBOMLine(BOMLine parentBOMLine, List<ItemRevision> childrenItemRevs,
                                              List<Map<String, String>> allUpdateAttrs) throws Exception {
        if (Objects.isNull(parentBOMLine) || childrenItemRevs == null || childrenItemRevs.isEmpty()) {
            return Collections.emptyList();
        }

        // Set the Child bomline to be added and their properties
        ItemLineInfo[] itemLineInfoArr = new ItemLineInfo[childrenItemRevs.size()];
        for (int i = 0; i < childrenItemRevs.size(); i++) {
            itemLineInfoArr[i] = new ItemLineInfo();
            itemLineInfoArr[i].itemRev = childrenItemRevs.get(i);
            // key: BOMLine property, value: BOMLine value.
            if (allUpdateAttrs != null && allUpdateAttrs.size() > 0) {
                itemLineInfoArr[i].itemLineProperties = allUpdateAttrs.get(i);
            }
        }
        AddOrUpdateChildrenToParentLineInfo[] addChToParInfoArr = new AddOrUpdateChildrenToParentLineInfo[1];
        addChToParInfoArr[0] = new AddOrUpdateChildrenToParentLineInfo();
        addChToParInfoArr[0].items = itemLineInfoArr;
        addChToParInfoArr[0].parentLine = parentBOMLine;

        com.teamcenter.services.strong.bom.StructureManagementService bomSMService =
                this.tcContextHolder.getBOMStructureManagementService();
        // Add children
        AddOrUpdateChildrenToParentLineResponse addUpdChToParResp =
                bomSMService.addOrUpdateChildrenToParentLine(addChToParInfoArr);

        tcContextHolder.printAndLogMessageFromServiceData(addUpdChToParResp.serviceData);

        List<BOMLine> bomLines = new ArrayList<>();
        for (BOMLinesOutput itemLine : addUpdChToParResp.itemLines) {
            bomLines.add(proxy(itemLine.bomline, tcContextHolder));
        }
        return bomLines;
    }

    /**
     * 删除当前行
     *
     * @param bomLines 删除当前行
     * @return
     */
    @Override
    public RemoveChildrenFromParentLineResponse removeBOMLines(List<BOMLine> bomLines) {
        if (bomLines == null || bomLines.isEmpty()) {
            return null;
        }
        com.teamcenter.services.strong.bom.StructureManagementService bomSMService =
                this.tcContextHolder.getBOMStructureManagementService();
        // This API just remove the BOM lines array, not sub line from parameter
        // bomLines.
        return bomSMService.removeChildrenFromParentLine(bomLines.toArray(new BOMLine[bomLines.size()]));
    }

    /**
     * 从父BOMLine列表中删除子BOMLines(注意删除的将会是当前行 TC API BUG )， 对象类型为BOMLine
     *
     * @param modelObjects to be removed bomlines
     * @return
     */
    @Override
    public RemoveChildrenFromParentLineResponse
    removeSubBOMLines(List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjects) {
        return removeBOMLines(modelObjects.stream().map(o -> (BOMLine) o).collect(Collectors.toList()));
    }

    @Override
    public String getRevIdAlt(String revUid) {
        Arg[] inputs = new Arg[5];

        inputs[0] = new Arg();
        inputs[0].val = "ItemRevision";

        inputs[1] = new Arg();
        inputs[1].val = "TYPE::ItemRevision::ItemRevision::WorkspaceObject";

        inputs[2] = new Arg();
        inputs[2].val = "PDR";

        inputs[3] = new Arg();
        inputs[3].structure = new ICT.Structure[1];
        inputs[3].structure[0] = new ICT.Structure();
        inputs[3].structure[0].args = new Arg[2];
        inputs[3].structure[0].args[0] = new Arg();
        inputs[3].structure[0].args[0].val = "true";
        inputs[3].structure[0].args[1] = new Arg();
        inputs[3].structure[0].args[1].val = revUid;

        inputs[4] = new Arg();
        inputs[4].val = "TYPE::ItemRevision::ItemRevision::WorkspaceObject";

        try {
            InvokeICTMethodResponse response = getTcContextHolder().getICTService().invokeICTMethod("ICCTItemRevision", "getNewRevisionIdAlt", inputs);
            getTcContextHolder().printAndLogMessageFromServiceData(response.serviceData, true);
            return response.output[1].val;
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void createBaseline(ModelObject revision, String desc, String workflowTemplateName,
                               String workName, String baselineName, String workDesc) {
        com.eingsoft.emop.tc.model.ModelObject proxyRev = ProxyUtil.spy(revision, getTcContextHolder());
        String objStr = proxyRev.getDisplayVal(BMIDE.PROP_OBJECT_STRING);

        String revIdAlt = getRevIdAlt(revision.getUid());
        if (StringUtils.isEmpty(revIdAlt)) {
            throw new RuntimeException(objStr + " 未成功获取基线ID");
        }

        // 打开版本对象的BOM window 获取 UID
        BOMWindow bomWindow = openBOMWindowWithTopLine(revision);

        Arg[] inputs = new Arg[9];
        inputs[0] = new Arg();
        inputs[0].val = "BOMWindow";

        inputs[1] = new Arg();
        inputs[1].val = "TYPE::BOMWindow::BOMWindow::RuntimeBusinessObject";
        inputs[2] = new Arg();
        // "BOM::47296"; bomline
        // TODO 无法识别的 bom model tag； BOM::47296不识别
        inputs[2].val = bomWindow.getUid();

        inputs[3] = new Arg();
        inputs[3].val = revIdAlt; // "A.002";//rev

        inputs[4] = new Arg();
        inputs[4].structure = new ICT.Structure[1];
        inputs[4].structure[0] = new ICT.Structure();
        inputs[4].structure[0].args = new Arg[2];
        inputs[4].structure[0].args[0] = new Arg();
        inputs[4].structure[0].args[0].val = "true";
        inputs[4].structure[0].args[1] = new Arg();
        inputs[4].structure[0].args[1].val = StringUtils.isEmpty(desc) ? objStr : desc;

        inputs[5] = new Arg();
        inputs[5].structure = new ICT.Structure[1];
        inputs[5].structure[0] = new ICT.Structure();
        inputs[5].structure[0].args = new Arg[2];
        inputs[5].structure[0].args[0] = new Arg();
        inputs[5].structure[0].args[0].val = "true";
        inputs[5].structure[0].args[1] = new Arg();
        inputs[5].structure[0].args[1].val = workflowTemplateName;

        //workflow
        inputs[6] = new Arg();
        inputs[6].structure = new ICT.Structure[1];
        inputs[6].structure[0] = new ICT.Structure();
        inputs[6].structure[0].args = new Arg[2];
        inputs[6].structure[0].args[0] = new Arg();
        inputs[6].structure[0].args[0].val = "true";
        inputs[6].structure[0].args[1] = new Arg();
        inputs[6].structure[0].args[1].val = StringUtils.isEmpty(workName) ? objStr : workName;

        //作业名
        inputs[7] = new Arg();
        inputs[7].structure = new ICT.Structure[1];
        inputs[7].structure[0] = new ICT.Structure();
        inputs[7].structure[0].args = new Arg[2];
        inputs[7].structure[0].args[0] = new Arg();
        inputs[7].structure[0].args[0].val = "true";
        inputs[7].structure[0].args[1] = new Arg();
        // 自动补充基线ID
        inputs[7].structure[0].args[1].val = baselineName + "_" + revIdAlt;

        inputs[8] = new Arg();
        inputs[8].structure = new ICT.Structure[1];
        inputs[8].structure[0] = new ICT.Structure();
        inputs[8].structure[0].args = new Arg[2];
        inputs[8].structure[0].args[0] = new Arg();
        inputs[8].structure[0].args[0].val = "true";
        inputs[8].structure[0].args[1] = new Arg();
        inputs[8].structure[0].args[1].val = StringUtils.isEmpty(workDesc) ? objStr : workDesc;

        try {
            InvokeICTMethodResponse response = getTcContextHolder().getICTService().invokeICTMethod("ICCTBOMWindow", "createBaseline", inputs);
            getTcContextHolder().printAndLogMessageFromServiceData(response.serviceData, true);
        } catch (ServiceException e) {
            e.printStackTrace();
        } finally {
            closeBOMWindow(bomWindow);
        }
    }
}

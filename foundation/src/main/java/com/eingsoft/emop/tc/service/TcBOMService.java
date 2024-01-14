package com.eingsoft.emop.tc.service;

import com.teamcenter.services.strong.bom._2008_06.StructureManagement.RemoveChildrenFromParentLineResponse;
import com.teamcenter.services.strong.cad._2007_01.StructureManagement.CreateBOMWindowsResponse;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.BOMLine;
import com.teamcenter.soa.client.model.strong.BOMWindow;
import com.teamcenter.soa.client.model.strong.ItemRevision;

import java.util.List;
import java.util.Map;

public interface TcBOMService extends AbstractTcBOMService<BOMLine, ItemRevision> {

    /**
     * 根据参数创建BOMWindow, 当前参数为TOP line对应的对象, 并且加载 top_line属性
     *
     * @param modelObject Item or ItemRevision 类型
     * @return
     */
    BOMWindow openBOMWindowWithTopLine(ModelObject modelObject);

    /**
     * 根据ruleName参数创建BOMWindow, 当前参数为TOP line对应的对象, 并且加载 top_line属性
     *
     * @param modelObject Item or ItemRevision 类型
     * @return
     */
    BOMWindow openBOMWindowWithTopLine(ModelObject modelObject, String ruleName);

    /**
     * 关闭本线程打开的BOMWindow
     */
    void closeAllBOMWindow();

    /**
     * 关闭本线程打开的BOMWindow，不抛出错误
     */
    void closeAllBOMWindowSiliently();

    void saveBOMWindow(BOMWindow bomWindow);

    /**
     * 打包BOM行
     *
     * @param lines
     * @return
     */
    List<BOMLine> pack(List<BOMLine> lines);

    void closeBOMWindow(BOMWindow bomWindow);

    /**
     * 在父结点中添加子BOM行
     *
     * @param parentItemRev
     * @param childrenItemRevs
     */
    List<BOMLine> addChildrenInBOMLine(BOMLine parentBOMLine, List<ItemRevision> childrenItemRevs);

    /**
     * 在父结点中添加子BOM行， 并且更新属性值, allUpdateAttrs可选。
     *
     * @param parentBOMLine
     * @param childrenItemRevs
     * @param allUpdateAttrs
     * @throws Exception
     */
    List<BOMLine> addChildrenInBOMLine(BOMLine parentBOMLine, List<ItemRevision> childrenItemRevs,
                                       List<Map<String, String>> allUpdateAttrs) throws Exception;

    /**
     * 删除当前行
     *
     * @param bomLines 删除当前行
     * @return
     */
    RemoveChildrenFromParentLineResponse removeBOMLines(List<BOMLine> bomLines);

    /**
     * 从父BOMLine列表中删除子BOMLines， 对象类型为BOMLine
     *
     * @param modelObjects 父BOM行
     * @return
     */
    RemoveChildrenFromParentLineResponse removeSubBOMLines(List<? extends ModelObject> modelObjects);

    CreateBOMWindowsResponse buildBomWinWithTopLineResponse(ModelObject modelObject, String ruleName);

    CreateBOMWindowsResponse buildBomWinWithTopLineResponses(List<ModelObject> modelObjects, String ruleName);

    /**
     * 获取基线ID
     *
     * @param revUid
     * @return A.001   A.002
     */
    String getRevIdAlt(String revUid);

    /**
     * 针对版本对象 打开BOM window, 再创建BOM基线, 如果BOM是单个物料，也可当成是针对物料版本 打基线
     *
     * @param revision             将要打基线的版本对象
     * @param desc                 非必填，为空则以 版本对象的 object_string 作为值
     * @param workflowTemplateName 默认流程名称 "TC Default Baseline Process“
     * @param workName             非必填，为空则以 版本对象的 object_string 作为值
     * @param baselineName         建议取对象的 Baseline_ + object_name 为名称, 代码自动补上基线ID， 如：Baseline_Veh0001-TemplateVeh_A.0
     * @param workDesc             非必填，为空则以 版本对象的 object_string 作为值
     */
    void createBaseline(ModelObject revision, String desc, String workflowTemplateName,
                        String workName, String baselineName, String workDesc);
}

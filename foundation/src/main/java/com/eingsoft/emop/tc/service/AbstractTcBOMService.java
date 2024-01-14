package com.eingsoft.emop.tc.service;

import java.util.List;
import java.util.Map;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.BOMLine;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.client.model.strong.MEProcessRevision;
import com.teamcenter.soa.client.model.strong.Mfg0BvrProcess;
import com.teamcenter.soa.client.model.strong.RevisionRule;

public interface AbstractTcBOMService<T extends BOMLine, R extends ItemRevision> extends TcService {

  /**
   * Preload the bom
   * 
   * 深度遍历 BOM 树，每一层会调用一次 batch API去取回当前层的所有BOM行
   * 
   */
  T preLoadBOM(R itemRevision, BOMPreloadConfig visitor);

  /**
   * Preload the bom
   * 
   * 深度遍历 BOM 树，每一层会调用一次 batch API去取回当前层的所有BOM行
   * 
   */
  T preLoadBOM(R itemRevision);

  /**
   * Preload the bom line
   * 
   * 深度遍历 BOM 树，每一层会调用一次 batch API去取回当前层的所有BOM行
   * 
   */
  void preLoadBOM(T bomLine, BOMPreloadConfig visitor);

  /**
   * Preload the bom line
   * 
   * 深度遍历 BOM 树，每一层会调用一次 batch API去取回当前层的所有BOM行
   * 
   */
  void preLoadBOM(T bomLine);

  /**
   * 获取item revision对应的bomline
   * 
   * @param itemRev
   * @return
   */
  T getTopLine(ModelObject itemRev);

  /**
   * 获取item revision对应的bomline，带上ruleName
   * 
   * @param itemRev
   * @return
   */
  T getTopLine(ModelObject itemRev, String ruleName);

  /**
   * Get the BOM Rule instance, it could be:
   * 
   * 1.Latest Working 2.Any Status; No Working 3.Any Status; Working 4.Working; Any Status
   * 5.Working; Any Status 6.Latest by Creation Date 7.Latest by Alpha Revision Order
   * 8.Working(Current User); Any Status 9.Working(Current Group); Any Status 10.Precise Only
   * 11.Precise; Working 12.Precise; Any Status
   * 
   * @param ruleName
   * @return
   */
  RevisionRule findRevisionRule(String ruleName) throws Exception;

  /**
   * get {@link BOMLine} or its sub type through item revision and the BOM rule name.
   * 
   * if the itemRev is an instance of {@link ItemRevision}, then the children will be
   * {@link BOMLine} instances.
   * 
   * if the itemRev is an instance of {@link MEProcessRevision}, then the children will be
   * {@link Mfg0BvrProcess} instances.
   * 
   * @param itemRev
   * @return
   */
  List<T> getChildBOMLineList(R itemRev);

  /**
   * get {@link BOMLine} or its sub type through item revision and the BOM rule name.
   * 
   * if the itemRev is an instance of {@link ItemRevision}, then the children will be
   * {@link BOMLine} instances.
   * 
   * if the itemRev is an instance of {@link MEProcessRevision}, then the children will be
   * {@link Mfg0BvrProcess} instances.
   * 
   * @param itemRev
   * @return
   */
  List<T> getChildBOMLineList(R itemRev, String ruleName);

  /**
   * 批量替换多个BOM行
   * 
   * @param bomline2ReplacedRevMap key: 被替换的BOM行对象，value: 用于替换的版本对象
   * @param replaceOption
   * @return 返回 被替换后的BOM行对象列表
   */
  List<com.eingsoft.emop.tc.model.ModelObject> batchReplaceBomLines(Map<BOMLine, ItemRevision> bomline2ReplacedRevMap, int replaceOption);

  /**
   * 替换单个BOM行
   * 
   * @param bomline 将要被替换的BOM行
   * @param replacedRev 用于替换的新 版本对象
   * @param replaceOption 0 替换当前行； 1 替换当前行和相同的兄弟节点BOM行； 2 替换当前节点及兄弟节点、以及子节点
   * @return 返回 被替换后的BOM行对象列表
   */
  List<com.eingsoft.emop.tc.model.ModelObject> replaceBomLine(BOMLine bomline, ItemRevision replacedRev, int replaceOption);
}

package com.eingsoft.emop.tc.service.impl;

import static com.eingsoft.emop.tc.util.ProxyUtil.proxy;
import static com.eingsoft.emop.tc.util.ProxyUtil.spy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.eingsoft.emop.tc.BMIDE;
import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.connection.EmopRequestListener.SOADiagnosticInfo;
import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.service.AbstractTcBOMService;
import com.eingsoft.emop.tc.service.BOMPreloadConfig;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.cache.ModelObjectCache;
import com.eingsoft.emop.tc.util.ProxyUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.internal.strong.structuremanagement.RestructureService;
import com.teamcenter.services.internal.strong.structuremanagement._2014_12.Restructure.ReplaceItemsParameter;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.services.strong.query._2006_03.SavedQuery.GetSavedQueriesResponse;
import com.teamcenter.services.strong.query._2007_09.SavedQuery.QueryResults;
import com.teamcenter.services.strong.query._2007_09.SavedQuery.SavedQueriesResponse;
import com.teamcenter.services.strong.query._2008_06.SavedQuery.QueryInput;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.BOMLine;
import com.teamcenter.soa.client.model.strong.ImanQuery;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.client.model.strong.RevisionRule;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
public abstract class AbstractTcBOMServiceImpl<T extends BOMLine, R extends ItemRevision> implements AbstractTcBOMService<T, R> {
  @Getter
  final TcContextHolder tcContextHolder;
  final static ExecutorService execute = new ThreadPoolExecutor(2, 10, 5, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(10));
  final static Cache<String, RevisionRule> revRules = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).maximumSize(100).build();

  AbstractTcBOMServiceImpl(TcContextHolder tcContextHolder) {
    this.tcContextHolder = tcContextHolder;
  }

  @Override
  public T preLoadBOM(R revision, BOMPreloadConfig config) {
    T topLine = getTopLine(revision, null);
    preLoadBOM(topLine, config);
    return (T) config.getCache().retrieve(topLine.getUid());
  }

  @Override
  public T preLoadBOM(R revision) {
    return preLoadBOM(revision, new BOMPreloadConfig() {});
  }

  @Override
  public void preLoadBOM(T bomLine, BOMPreloadConfig config) {
    loadBOMLines(new ArrayList<com.teamcenter.soa.client.model.ModelObject>(Arrays.asList(bomLine)), config, 0);
  }

  @Override
  public void preLoadBOM(T bomLine) {
    preLoadBOM(bomLine, new BOMPreloadConfig() {});
  }

  @Override
  public List<T> getChildBOMLineList(R itemRev) {
    return getChildBOMLineList(itemRev, null);
  }

  @Override
  public T getTopLine(com.teamcenter.soa.client.model.ModelObject itemRev) {
    return getTopLine(itemRev, null);
  }

  @Override
  public List<T> getChildBOMLineList(R itemRev, String ruleName) {
    T topLine = getTopLine(itemRev, ruleName);
    ModelObject topLineModelObject = spy(topLine, tcContextHolder);
    @SuppressWarnings("unchecked")
    List<ModelObject> lines = (List<ModelObject>) topLineModelObject.get(BMIDE.PROP_BL_ALL_CHILD_LINES);
    return lines.stream().map((a) -> (T) spy(a, tcContextHolder)).collect(Collectors.toList());
  }

  @SneakyThrows
  private void loadBOMLines(List<? extends com.teamcenter.soa.client.model.ModelObject> bomLines, BOMPreloadConfig visitor, int depth) {
    SOADiagnosticInfo diagnosticInfo = SOAExecutionContext.current().getDiagnosticInfo();
    if (log.isDebugEnabled()) {
      log.debug("start to load the " + depth + " depth of " + bomLines.size() + " bom lines");
      for (com.teamcenter.soa.client.model.ModelObject bomLine : bomLines) {
        log.debug(bomLine.getUid() + "," + spy(bomLines.get(0), tcContextHolder).get(BMIDE.PROP_BL_TITLE));
      }
    }
    ModelObjectCache cache = visitor.getCache();
    if (bomLines.size() > 0) {
      loadModelObjectsAndSyncToCache(bomLines, cache);
    }
    List<Future<?>> currentDepthTasks = new ArrayList<Future<?>>();
    List<ModelObject> spiedBomLines = spy(bomLines, tcContextHolder);
    if (visitor.isItemInBOMLineNeedInitializeProperties()) {
      // add Item property load task
      currentDepthTasks.add(execute.submit(new LoadItemPropertiesTask(spiedBomLines, SOAExecutionContext.current())));
    }
    if (visitor.isItemRevisionInBOMLineNeedInitializeProperties()) {
      // add Item revision property load task
      currentDepthTasks.add(execute.submit(new LoadRevisionPropertiesTask(spiedBomLines, SOAExecutionContext.current())));
    }
    // add all additional tasks from bomPreLoadStrategy
    currentDepthTasks.addAll(visitor.postBOMLinesLoadedTasks(spiedBomLines, tcContextHolder, diagnosticInfo).stream()
        .map(t -> execute.submit(t)).collect(Collectors.toList()));
    // reached the expected depth
    if (depth++ >= visitor.getMaxDepth()) {
      waitForFutureTaskComplete(currentDepthTasks);
      currentDepthTasks.addAll(visitor.postBOMLineItemsAndRevisionsLoadedTasks(spiedBomLines, tcContextHolder, diagnosticInfo).stream()
          .map(t -> execute.submit(t)).collect(Collectors.toList()));
      waitForFutureTaskComplete(currentDepthTasks);
      return;
    }
    /**
     * retrieve all current layer bomlines's children and flatten them into a single list, and then
     * load the next layer bomlines
     */
    List<ModelObject> nextLayerChildren =
        bomLines.stream().map(b -> ((List<ModelObject>) cache.retrieve(b.getUid()).get(BMIDE.PROP_BL_ALL_CHILD_LINES)).stream()
            .map(m -> spy(m, tcContextHolder)).collect(Collectors.toList())).flatMap(List::stream).collect(Collectors.toList());
    nextLayerChildren = visitor.getNextLayerBOMLines(nextLayerChildren);
    // wait for all tasks done
    waitForFutureTaskComplete(currentDepthTasks);
    // wait for above tasks done
    waitForFutureTaskComplete(currentDepthTasks);
    if (!nextLayerChildren.isEmpty()) {
      loadBOMLines(nextLayerChildren, visitor, depth++);
    }
  }

  private void waitForFutureTaskComplete(List<Future<?>> tasks) throws Exception {
    for (Future<?> task : tasks) {
      task.get(10, TimeUnit.MINUTES);
    }
  }

  @AllArgsConstructor
  private class LoadItemPropertiesTask implements Runnable {

    private final List<ModelObject> bomLines;
    private final SOAExecutionContext ctx;

    @Override
    public void run() {
      try {
        SOAExecutionContext.current().initWithSameCache(ctx);
        List<ModelObject> itemsToLoadProperties =
            bomLines.stream().map(b -> (ModelObject) spy(b, tcContextHolder).get(BMIDE.PROP_BL_ITEM)).collect(Collectors.toList());
        loadModelObjectsAndSyncToCache(
            SOAExecutionContext.current().getModelObjectCache().getNotExistsInCache(Collections.unmodifiableList(itemsToLoadProperties)),
            SOAExecutionContext.current().getModelObjectCache());
      } finally {
        SOAExecutionContext.current().cleanupSiliently(false);
      }
    }
  }

  @AllArgsConstructor
  private class LoadRevisionPropertiesTask implements Runnable {

    private final List<ModelObject> bomLines;
    private final SOAExecutionContext ctx;

    @Override
    public void run() {
      try {
        SOAExecutionContext.current().initWithSameCache(ctx);
        List<ModelObject> itemsToLoadProperties =
            bomLines.stream().map(b -> (ModelObject) spy(b, tcContextHolder).get(BMIDE.PROP_BL_REVISION)).collect(Collectors.toList());
        loadModelObjectsAndSyncToCache(
            SOAExecutionContext.current().getModelObjectCache().getNotExistsInCache(Collections.unmodifiableList(itemsToLoadProperties)),
            SOAExecutionContext.current().getModelObjectCache());
      } finally {
        SOAExecutionContext.current().cleanupSiliently(false);
      }
    }
  }

  private void loadModelObjectsAndSyncToCache(List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjects,
      ModelObjectCache cache) {
    List<ModelObject> loadedModelObjects = tcContextHolder.getTcLoadService().loadProperties(modelObjects);
    loadedModelObjects.stream().forEach(obj -> {
      if (obj != null) {
        cache.put(obj.getUid(), spy(obj, tcContextHolder));
      } else {
        log.warn("encountered not expected null");
      }
    });
  }

  @Override
  public RevisionRule findRevisionRule(String ruleName) throws Exception {
    ImanQuery query = null;
    RevisionRule retObj = null;
    try {
      GetSavedQueriesResponse savedQueries = SavedQueryService.getService(tcContextHolder.getConnection()).getSavedQueries();
      if (savedQueries.queries.length == 0) {
        log.error("There are no saved queries in the system.");
        throw new Exception("There are no saved queries in the system.");
      }

      // Find one called 'General...'
      for (int i = 0; i < savedQueries.queries.length; i++) {
        if (savedQueries.queries[i].name.equals("__WEB_rev_rules")) {
          query = savedQueries.queries[i].query;
          break;
        }
      }
    } catch (ServiceException e) {
      log.error("GetSavedQueries service request failed.");
      log.error(e.getMessage());
      throw new Exception(e);
    }

    if (query == null) {
      log.error("There is not an '__WEB_rev_rules' query.");
      throw new Exception("There is not an '__WEB_rev_rules' query.");
    }

    // 通过查询构建器查询
    QueryInput savedQueryInput[] = new QueryInput[1];
    savedQueryInput[0] = new QueryInput();
    savedQueryInput[0].query = query;
    savedQueryInput[0].maxNumToReturn = 0;
    savedQueryInput[0].limitList = new ModelObject[0];

    SavedQueriesResponse savedQueryResult =
        SavedQueryService.getService(tcContextHolder.getConnection()).executeSavedQueries(savedQueryInput);
    QueryResults found = savedQueryResult.arrayOfResults[0];
    if (found.objectUIDS.length > 0) {
      ServiceData sd = tcContextHolder.getDataManagementService().loadObjects(found.objectUIDS);
      for (int i = 0; i < found.objectUIDS.length; i++) {
        RevisionRule tmpRetObj = (RevisionRule) sd.getPlainObject(i);
        tcContextHolder.getDataManagementService().refreshObjects(new com.teamcenter.soa.client.model.ModelObject[] {tmpRetObj});
        if (ruleName.equals(tmpRetObj.getPropertyDisplayableValue("object_name"))) {
          return proxy(tmpRetObj, tcContextHolder);
        }
      }
    }
    return retObj;
  }

  /**
   * 替换单个BOM行
   * 
   * @param bomline 将要被替换的BOM行
   * @param replacedRev 用于替换的新 版本对象
   * @param replaceOption 0 替换当前行； 1 替换当前行和相同的兄弟节点BOM行； 2 替换当前节点及兄弟节点、以及子节点
   * @return 返回 被替换后的BOM行对象列表
   */
  @Override
  public List<ModelObject> replaceBomLine(BOMLine bomline, ItemRevision replacedRev, int replaceOption) {
    Map<BOMLine, ItemRevision> bomline2ReplacedRevMap = Maps.newHashMap();
    bomline2ReplacedRevMap.put(bomline, replacedRev);
    return batchReplaceBomLines(bomline2ReplacedRevMap, replaceOption);
  }

  /**
   * 批量替换多个BOM行
   * 
   * @param bomline2ReplacedRevMap key: 被替换的BOM行对象，value: 用于替换的版本对象
   * @param replaceOption
   * @return 返回 被替换后的BOM行对象列表
   */
  @Override
  public List<ModelObject> batchReplaceBomLines(Map<BOMLine, ItemRevision> bomline2ReplacedRevMap, int replaceOption) {
    List<ReplaceItemsParameter> replaceItemsParameters = Lists.newArrayList();
    for (Entry<BOMLine, ItemRevision> entry : bomline2ReplacedRevMap.entrySet()) {
      ReplaceItemsParameter replaceItemsParameter = new ReplaceItemsParameter();
      replaceItemsParameter.bomLine = entry.getKey();
      replaceItemsParameter.itemRevision = entry.getValue();
      replaceItemsParameter.item = null;
      replaceItemsParameter.viewType = null;
      replaceItemsParameter.replaceOption = replaceOption;
      replaceItemsParameters.add(replaceItemsParameter);
    }

    RestructureService service = tcContextHolder.getRestructureService();
    ServiceData serviceData =
        service.replaceItems(replaceItemsParameters.toArray(new ReplaceItemsParameter[replaceItemsParameters.size()]));
    tcContextHolder.printAndLogMessageFromServiceData(serviceData);

    List<ModelObject> afterReplacedBomline = Lists.newArrayList();
    for (int i = 0; i < serviceData.sizeOfUpdatedObjects(); i++) {
      ModelObject replacedBomline = ProxyUtil.spy(serviceData.getUpdatedObject(i), getTcContextHolder());
      System.out.println(i + " = " + replacedBomline.getDisplayVal(BMIDE.PROP_BL_TITLE));
      afterReplacedBomline.add(replacedBomline);
    }
    return afterReplacedBomline;
  }
}

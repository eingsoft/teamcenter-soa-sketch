package com.eingsoft.emop.tc.service;

import java.util.Collections;
import java.util.List;

import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.connection.EmopRequestListener.SOADiagnosticInfo;
import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.service.cache.ModelObjectCache;

public interface BOMPreloadConfig {

    /**
     * visiting max depth
     */
    default int getMaxDepth() {
        return Integer.MAX_VALUE;
    }

    default ModelObjectCache getCache() {
        return SOAExecutionContext.current().getModelObjectCache();
    }

    /**
     * retrieve next layer bomlines, it is necessary when need skip some bomlines
     */
    default List<ModelObject> getNextLayerBOMLines(List<ModelObject> bomlines) {
        return bomlines;
    }

    /**
     * whether need load properties for the bomline mapped Item
     */
    default boolean isItemInBOMLineNeedInitializeProperties() {
        return true;
    }

    /**
     * whether need load properties for the bomline mapped ItemRevision
     */
    default boolean isItemRevisionInBOMLineNeedInitializeProperties() {
        return true;
    }

    /**
     * called when one depth of BOM lines are loaded, in addtional, the BOMLine's item and item revision properties are
     * loaded.
     */
    default List<Runnable> postBOMLineItemsAndRevisionsLoadedTasks(List<ModelObject> bomLines,
        TcContextHolder tcContextHolder, SOADiagnosticInfo diagnosticInfo) {
        return Collections.emptyList();
    }

    /**
     * called when one depth of BOM lines are loaded
     */
    default List<Runnable> postBOMLinesLoadedTasks(List<ModelObject> bomLines, TcContextHolder tcContextHolder,
        SOADiagnosticInfo diagnosticInfo) {
        return Collections.emptyList();
    }

}

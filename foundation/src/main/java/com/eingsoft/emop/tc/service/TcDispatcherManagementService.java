package com.eingsoft.emop.tc.service;

import java.util.List;

import lombok.AllArgsConstructor;

import com.teamcenter.soa.client.model.ModelObject;

/**
 * 
 * @author beam
 *
 *         submit model conversion request to dispatcher server
 */
public interface TcDispatcherManagementService extends TcService {

    /**
     * send a creo to jt model conversion request
     * 
     */
    void sendCreoToJTConversionRequestByUids(List<DatasetInfoUids> datasetInfos);

    /**
     * send a creo to jt model conversion request
     * 
     */
    void sendCreoToJTConversionRequestByModelObjects(List<DatasetInfoModelObjects> datasetInfos);

    /**
     * model file Id and the embedded dataset Id
     *
     */
    @AllArgsConstructor
    public static class DatasetInfoUids {
        public String itemRevUid;
        public String datasetUid;
    }

    /**
     * model file and the embedded dataset
     *
     */
    @AllArgsConstructor
    public static class DatasetInfoModelObjects {
        public ModelObject itemRev;
        public ModelObject dataset;
    }
}

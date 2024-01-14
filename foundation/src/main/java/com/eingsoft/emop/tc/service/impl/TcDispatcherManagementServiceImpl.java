package com.eingsoft.emop.tc.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;
import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcDispatcherManagementService;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.strong.core.DispatcherManagementService;
import com.teamcenter.services.strong.core._2008_06.DispatcherManagement.CreateDispatcherRequestArgs;
import com.teamcenter.services.strong.core._2008_06.DispatcherManagement.CreateDispatcherRequestResponse;

@Log4j2
@ScopeDesc(Scope.TcContextHolder)
public class TcDispatcherManagementServiceImpl implements TcDispatcherManagementService {

    @Getter
    private final TcContextHolder tcContextHolder;

    public TcDispatcherManagementServiceImpl(TcContextHolder tcContextHolder) {
        this.tcContextHolder = tcContextHolder;
    }

    @Override
    public void sendCreoToJTConversionRequestByUids(List<DatasetInfoUids> datasetInfos) {
        if (datasetInfos.isEmpty()) {
            return;
        }
        List<String> datasetUids = datasetInfos.stream().map(o -> o.itemRevUid).collect(Collectors.toList());
        List<String> fileUids = datasetInfos.stream().map(o -> o.datasetUid).collect(Collectors.toList());
        datasetUids.addAll(fileUids);
        List<ModelObject> objs = tcContextHolder.getTcLoadService().loadObjects(datasetUids);
        List<DatasetInfoModelObjects> conversions = new ArrayList<DatasetInfoModelObjects>(datasetInfos.size());
        for (int i = 0; i < datasetInfos.size(); i++) {
            conversions.add(new DatasetInfoModelObjects(objs.get(i), objs.get(datasetInfos.size() + i)));
        }
        sendCreoToJTConversionRequestByModelObjects(conversions);
    }

    @Override
    public void sendCreoToJTConversionRequestByModelObjects(List<DatasetInfoModelObjects> datasetInfos) {
        if (datasetInfos.isEmpty()) {
            return;
        }
        DispatcherManagementService service = DispatcherManagementService.getService(tcContextHolder.getConnection());
        CreateDispatcherRequestResponse response;
        try {
            response =
                service.createDispatcherRequest(new CreateDispatcherRequestArgs[] {createRequest("creotojt",
                    datasetInfos.stream().map(o -> o.itemRev).toArray(ModelObject[]::new),
                    datasetInfos.stream().map(o -> o.dataset).toArray(ModelObject[]::new))});
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
        if (response.requestsCreated.length < datasetInfos.size()) {
            log.warn("some of the requests are filed to submit " + datasetInfos);
        } else {
            log.info("successfully submitted " + datasetInfos.size() + " requests.");
        }
    }

    private CreateDispatcherRequestArgs createRequest(String serviceName, ModelObject[] datasets,
        ModelObject[] partFiles) {
        CreateDispatcherRequestArgs request = new CreateDispatcherRequestArgs();
        request.providerName = "SIEMENS";
        // high priority
        request.priority = 3;
        request.type = "ONDEMAND-PRIORITY";
        request.serviceName = serviceName;
        request.primaryObjects = datasets;
        request.secondaryObjects = partFiles;
        return request;
    }

}

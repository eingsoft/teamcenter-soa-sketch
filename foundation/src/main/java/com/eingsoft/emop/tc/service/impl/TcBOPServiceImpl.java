package com.eingsoft.emop.tc.service.impl;

import java.util.Set;

import lombok.extern.log4j.Log4j2;

import com.eingsoft.emop.tc.SOAExecutionContext;
import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;
import com.eingsoft.emop.tc.service.TcBOPService;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.teamcenter.services.strong.manufacturing.DataManagementService;
import com.teamcenter.services.strong.manufacturing._2011_06.DataManagement.OpenContextInput;
import com.teamcenter.services.strong.manufacturing._2011_06.DataManagement.OpenContextsResponse;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.MEProcessRevision;
import com.teamcenter.soa.client.model.strong.Mfg0BvrProcess;

@Log4j2
@ScopeDesc(Scope.TcContextHolder)
public class TcBOPServiceImpl extends AbstractTcBOMServiceImpl<Mfg0BvrProcess, MEProcessRevision> implements
		TcBOPService {

	public TcBOPServiceImpl(TcContextHolder tcContextHolder) {
		super(tcContextHolder);
	}

	@Override
	public Mfg0BvrProcess getTopLine(ModelObject itemRev, String ruleName) {
		DataManagementService service = getTcContextHolder().getMfgDataManagementService();
		OpenContextInput input = new OpenContextInput();
		input.object = itemRev;
		OpenContextsResponse response = service.openContexts(new OpenContextInput[] { input });
		getTcContextHolder().printAndLogMessageFromServiceData(response.serviceData, true);
		return (Mfg0BvrProcess) response.serviceData.getCreatedObject(0);
	}

	@Override
	public void closeAllBOPLinesSiliently() {
		Set<ModelObject> bopLines = SOAExecutionContext.current().getOpenedBOPLines();
		try {
			if (!bopLines.isEmpty()) {
				ModelObject[] lines = bopLines.toArray(new ModelObject[bopLines.size()]);
				getTcContextHolder().getMfgDataManagementService().closeContexts(lines);
				bopLines.clear();
			}
		} catch (Exception e) {
			log.error("error while closing BOP line.", e);
		}
	}
}

package com.eingsoft.emop.tc.service;

import com.teamcenter.soa.client.model.strong.MEProcessRevision;
import com.teamcenter.soa.client.model.strong.Mfg0BvrProcess;

public interface TcBOPService extends AbstractTcBOMService<Mfg0BvrProcess, MEProcessRevision> {

	/**
	 * close all opened bop lines without exception thrown
	 */
	void closeAllBOPLinesSiliently();

}

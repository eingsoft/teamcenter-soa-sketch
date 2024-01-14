package com.eingsoft.emop.tc.service;

import java.util.List;

import com.eingsoft.emop.tc.model.ModelObject;

/**
 * 
 * @author beam
 * 
 *         Mock server that doesn't meet the service standard
 *
 */
public interface ViolateMockService extends TcService {

    List<ModelObject> mock(List<ModelObject> modelObjects);
}

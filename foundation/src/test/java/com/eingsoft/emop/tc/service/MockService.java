package com.eingsoft.emop.tc.service;

import java.util.List;

import com.eingsoft.emop.tc.model.ModelObject;

public interface MockService extends TcService {

    ModelObject getModelObject();

    List<ModelObject> getModelObjects();

    ModelObject[] getModelObjectsArray();

    Object getObject();

    int getPrimitiveType();

    List<String> getStrs();

    List<Object> getObjects();

    int[] getPrimitiveArray();
}

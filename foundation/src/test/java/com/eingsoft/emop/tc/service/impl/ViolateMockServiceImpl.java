package com.eingsoft.emop.tc.service.impl;

import java.util.List;

import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;
import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.ViolateMockService;

@ScopeDesc(Scope.TcContextHolder)
public class ViolateMockServiceImpl implements ViolateMockService {

    private final TcContextHolder tcContextHolder;

    public ViolateMockServiceImpl(TcContextHolder tcContextHolder) {
        this.tcContextHolder = tcContextHolder;
    }

    @Override
    public List<ModelObject> mock(List<ModelObject> modelObjects) {
        return null;
    }

    @Override
    public TcContextHolder getTcContextHolder() {
        return tcContextHolder;
    }

}

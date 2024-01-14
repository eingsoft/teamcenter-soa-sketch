package com.eingsoft.emop.tc.service.impl;

import static com.eingsoft.emop.tc.util.MockDataUtil.createModelObject;
import static com.eingsoft.emop.tc.util.ProxyUtil.spy;

import java.util.Arrays;
import java.util.List;

import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;
import com.eingsoft.emop.tc.model.ModelObject;
import com.eingsoft.emop.tc.service.MockService;
import com.eingsoft.emop.tc.service.TcContextHolder;

@ScopeDesc(Scope.TcContextHolder)
public class MockServiceImpl implements MockService {

    private final TcContextHolder holder;

    public MockServiceImpl(TcContextHolder holder) {
        this.holder = holder;
    }

    @Override
    public TcContextHolder getTcContextHolder() {
        return holder;
    }

    @Override
    public ModelObject getModelObject() {
        return spy(createModelObject("Item", "uidA"), holder);
    }

    @Override
    public List<ModelObject> getModelObjects() {
        return spy(Arrays.asList(createModelObject("Item", "uidA"), createModelObject("Item", "uidB")), holder);
    }

    @Override
    public ModelObject[] getModelObjectsArray() {
        return new ModelObject[] {spy(createModelObject("Item", "uidA"), holder),
            spy(createModelObject("Item", "uidB"), holder)};
    }

    @Override
    public Object getObject() {
        return new Object();
    }

    @Override
    public int getPrimitiveType() {
        return 0;
    }

    @Override
    public List<String> getStrs() {
        return Arrays.asList("str");
    }

    @Override
    public List<Object> getObjects() {
        return Arrays.asList("str");
    }

    @Override
    public int[] getPrimitiveArray() {
        return new int[] {1};
    }
}

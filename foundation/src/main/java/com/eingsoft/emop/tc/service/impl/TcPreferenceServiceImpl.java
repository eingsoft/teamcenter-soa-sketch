package com.eingsoft.emop.tc.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcPreferenceService;
import com.teamcenter.services.strong.administration.PreferenceManagementService;
import com.teamcenter.services.strong.administration._2012_09.PreferenceManagement.GetPreferencesResponse;
import com.teamcenter.services.strong.administration._2012_09.PreferenceManagement.PreferenceValue;

import lombok.Getter;

@ScopeDesc(Scope.TcContextHolder)
public class TcPreferenceServiceImpl implements TcPreferenceService {

    @Getter
    private final TcContextHolder tcContextHolder;

    public TcPreferenceServiceImpl(TcContextHolder tcContextHolder) {
        this.tcContextHolder = tcContextHolder;
    }

    @Override
    public String getStringValue(String prefName) {
        PreferenceManagementService prefSvc = getTcContextHolder().getPreferenceManagementService();
        GetPreferencesResponse response = prefSvc.getPreferences(new String[] {prefName}, false);
        if (response.data.sizeOfPartialErrors() > 0 || response.response.length <= 0) {
            getTcContextHolder().printAndLogMessageFromServiceData(response.data, false);
            return null;
        } else {
            PreferenceValue prefValue = response.response[0].values;
            if (prefValue.values.length > 0) {
                return prefValue.values[0];
            } else {
                return null;
            }
        }
    }

    @Override
    public List<String> getStringValues(String prefName) {
        PreferenceManagementService prefSvc = getTcContextHolder().getPreferenceManagementService();
        GetPreferencesResponse response = prefSvc.getPreferences(new String[] {prefName}, false);
        if (response.data.sizeOfPartialErrors() > 0 || response.response.length <= 0) {
            getTcContextHolder().printAndLogMessageFromServiceData(response.data, false);
            return new ArrayList<String>();
        } else {
            PreferenceValue prefValue = response.response[0].values;
            if (prefValue.values.length > 0) {
                return Arrays.asList(prefValue.values);
            } else {
                return new ArrayList<String>();
            }
        }
    }
}
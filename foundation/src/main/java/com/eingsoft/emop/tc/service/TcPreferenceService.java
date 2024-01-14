package com.eingsoft.emop.tc.service;

import java.util.List;

public interface TcPreferenceService extends TcService {
    String getStringValue(String prefName);

    List<String> getStringValues(String prefName);
}

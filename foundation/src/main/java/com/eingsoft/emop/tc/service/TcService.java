package com.eingsoft.emop.tc.service;

import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;

/**
 * 
 * @author beam
 * 
 *         All the Teamcenter related service should extend this interface
 */
@ScopeDesc(Scope.TcContextHolder)
public interface TcService {

	TcContextHolder getTcContextHolder();
}

package com.eingsoft.emop.tc.service;

import java.util.List;
import java.util.Map;

import com.teamcenter.services.strong.core._2013_05.LOV.LOVValueRow;
import com.teamcenter.soa.client.model.ModelObject;

public interface TcLOVService extends TcService {
    /**
     * 初始化LOV DATA
     * 
     * @param owningObject
     * @param propertyName
     * @param boName 业务逻辑对象的名字 如 D9_MaterialRevision
     * @return
     */
    List<LOVValueRow> getInitialLOVValues(ModelObject owningObject, String propertyName, String boName);

    /**
     * 
     * 已知对象的情况下，获取所有UnitOfMeasure; 以symbol为键，uid为值
     * 
     * @param obj
     * @param propName
     * @return
     */
    Map<String, String> getUOMLOVMap(ModelObject refObj, String propName);

    /**
     * Get LOV Values
     */
    List<LOVValueRow> getInitialLOVValues(String propertyName, String boName);

    /**
     * 未知道TC对象，根据TC 对象类型，获取 uom_tag 引用属性类型的 LOV 对象MAP， 如 PCS -> 个 <br>
     * 值为 UnitOfMeasure
     * 
     * @param objectType JM8_Production, D9_Material
     * @param propName 默认uom_tag, 也有可能是继承自此属性的值，如 d9uom_tag
     * @return
     */
    Map<String, com.eingsoft.emop.tc.model.ModelObject> symbol2UOMTagMap(String objectType, String propName);

    /**
     * 根据 TC 对象类型 和 基本计量单位的 symbol(display value), 找到 uom_tag 对应的 引用对象 UnitOfMeasure
     * 
     * @param itemType Item
     * @param prop uom_tag
     * @param val PCS, TAG, BOX ...
     * @return
     */
    com.eingsoft.emop.tc.model.ModelObject findUnitOfMeasureWithSymbol(String itemType, String prop, String val);

}

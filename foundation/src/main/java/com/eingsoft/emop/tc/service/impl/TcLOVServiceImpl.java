package com.eingsoft.emop.tc.service.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import com.eingsoft.emop.tc.BMIDE;
import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcLOVService;
import com.eingsoft.emop.tc.util.ProxyUtil;
import com.teamcenter.services.strong.core._2013_05.LOV;
import com.teamcenter.services.strong.core._2013_05.LOV.InitialLovData;
import com.teamcenter.services.strong.core._2013_05.LOV.LOVSearchResults;
import com.teamcenter.services.strong.core._2013_05.LOV.LOVValueRow;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.exceptions.NotLoadedException;

/**
 * 对于LOV类型必填属性，如基本计量单位，引用类型，必需先找到TC中对应的对象
 * 
 * @author king
 */
@Log4j2
@ScopeDesc(Scope.TcContextHolder)
public class TcLOVServiceImpl implements TcLOVService {
    @Getter
    private TcContextHolder tcContextHolder;

    public TcLOVServiceImpl(TcContextHolder tcContextHolder) {
        this.tcContextHolder = tcContextHolder;
    }

    /**
     * 初始化LOV DATA
     * 
     * @param owningObject
     * @param propertyName
     * @param boName 业务逻辑对象的名字 如 D9_MaterialRevision
     * @return
     */
    private InitialLovData buildInitLovData(ModelObject owningObject, String propertyName, String boName) {
        LOV.LovFilterData filter = new LOV.LovFilterData();
        filter.maxResults = 2000;
        filter.numberToReturn = filter.maxResults;

        LOV.LOVInput input = new LOV.LOVInput();
        input.boName = boName;
        if (owningObject == null) {
            input.operationName = "Create";
        } else {
            input.operationName = "Edit";
            input.owningObject = owningObject;
        }
        Map<String, String[]> map = new HashMap<String, String[]>();
        map.put("object_type", new String[] {propertyName});
        // map.put("fnd0LOVContextObject", new Object[]{});
        // map.put("ip_classification", new Object[]{});
        input.propertyValues = map;

        LOV.InitialLovData initialLovData = new LOV.InitialLovData();
        initialLovData.filterData = filter;
        initialLovData.propertyName = propertyName;
        initialLovData.lovInput = input;

        return initialLovData;
    }

    private List<LOVValueRow> getInitialLOVValues(InitialLovData initialData) {
        LOVSearchResults results = tcContextHolder.getLOVService().getInitialLOVValues(initialData);
        tcContextHolder.printAndLogMessageFromServiceData(results.serviceData);

        for (LOVValueRow row : results.lovValues) {
            @SuppressWarnings("unchecked")
            Map<String, String[]> map = row.propDisplayValues;
            for (String key : map.keySet()) {
                log.debug("UID: " + row.uid + ", key: " + key + ", value: " + map.get(key)[0]);
            }

        }
        return Arrays.asList(results.lovValues);
    }

    /**
     * 初始化LOV DATA
     * 
     * @param owningObject
     * @param propertyName
     * @param boName 业务逻辑对象的名字 如 D9_MaterialRevision
     * @return
     */
    @Override
    public List<LOVValueRow> getInitialLOVValues(ModelObject owningObject, @NonNull String propertyName,
        @NonNull String boName) {
        LOV.InitialLovData initialData = buildInitLovData(owningObject, propertyName, boName);
        return getInitialLOVValues(initialData);
    }

    @Override
    public List<LOVValueRow> getInitialLOVValues(@NonNull String propertyName, @NonNull String boName) {
        return getInitialLOVValues(null, propertyName, boName);
    }

    /**
     * 
     * 已知对象的情况下，获取所有UnitOfMeasure; 以symbol为键，uid为值
     * 
     * @param obj
     * @param propName
     * @return
     */
    @Override
    public Map<String, String> getUOMLOVMap(ModelObject refObj, String propName) {
        Map<String, String> map = new HashMap<String, String>();
        LOV.InitialLovData initialData = buildInitLovData(refObj, propName, refObj.getTypeObject().getName());
        LOVSearchResults results = tcContextHolder.getLOVService().getInitialLOVValues(initialData);

        // 如果要取symbol属性，太慢了，就直接用DisplayValue，但是要保证DisplayValue就是symbol的值
        if (BMIDE.UNIT_OF_MEASURE_FAST_MODE) {
            for (LOVValueRow row : results.lovValues) {
                @SuppressWarnings("unchecked")
                Map<String, String[]> dvMap = row.propDisplayValues;
                @SuppressWarnings("unchecked")
                Map<String, String[]> ivMap = row.propInternalValues;
                String key = dvMap.get("lov_values")[0];
                String val = ivMap.get("lov_values")[0];
                map.put(key, val);
            }
        } else {
            for (LOVValueRow lovValueRow : results.lovValues) {
                @SuppressWarnings("unchecked")
                Map<String, String[]> vals = lovValueRow.propInternalValues;
                for (Entry<String, String[]> entry : vals.entrySet()) {
                    String uid = entry.getValue()[0];
                    ModelObject obj = tcContextHolder.getTcLoadService().loadObject(uid);
                    obj = tcContextHolder.getTcLoadService().loadProperty(obj, BMIDE.PROP_SYMBOL);
                    String symbol;
                    try {
                        symbol = obj.getPropertyObject(BMIDE.PROP_SYMBOL).getStringValue();
                        map.put(symbol, uid);
                        log.debug("key: " + symbol + ", val: " + uid);
                    } catch (NotLoadedException e) {
                        log.error(e);
                    }
                }
            }
        }
        return map;
    }

    /**
     * 未知道TC对象，根据TC 对象类型，获取 uom_tag 引用属性类型的 LOV 对象MAP， 如 PCS -> 个 <br>
     * 值为 UnitOfMeasure
     * 
     * @param objectType JM8_Production, D9_Material
     * @param propName 默认uom_tag, 也有可能是继承自此属性的值，如 d9uom_tag
     * @return
     */
    @Override
    public Map<String, com.eingsoft.emop.tc.model.ModelObject> symbol2UOMTagMap(String objectType, String propName) {
        Map<String, com.eingsoft.emop.tc.model.ModelObject> map =
            new HashMap<String, com.eingsoft.emop.tc.model.ModelObject>();
        LOV.InitialLovData initialData = buildInitLovData(null, propName, objectType);
        LOVSearchResults results = tcContextHolder.getLOVService().getInitialLOVValues(initialData);
        for (LOVValueRow row : results.lovValues) {
            @SuppressWarnings("unchecked")
            Map<String, String[]> dvMap = row.propDisplayValues;
            @SuppressWarnings("unchecked")
            Map<String, String[]> ivMap = row.propInternalValues;
            String key = dvMap.get("lov_values")[0];
            String uid = ivMap.get("lov_values")[0];
            ModelObject obj = tcContextHolder.getTcLoadService().loadObject(uid);
            map.put(key, ProxyUtil.spy(obj, tcContextHolder));
        }

        return map;
    }

    /**
     * 根据 TC 对象类型 和 基本计量单位的 symbol(display value), 找到 uom_tag 对应的 引用对象 UnitOfMeasure
     * 
     * @param itemType Item
     * @param prop uom_tag
     * @param val PCS, TAG, BOX ...
     * @return
     */
    @Override
    public com.eingsoft.emop.tc.model.ModelObject findUnitOfMeasureWithSymbol(String itemType, String prop,
        String val) {
        Map<String, com.eingsoft.emop.tc.model.ModelObject> symbol2UOMtagMap = symbol2UOMTagMap(itemType, prop);
        com.eingsoft.emop.tc.model.ModelObject unitOfMeasure = symbol2UOMtagMap.get(val);
        if (unitOfMeasure == null) {
            throw new RuntimeException("Cannot find the UnitOfMeasure value with params(Type:" + itemType + ", Property:"
                + prop + ", Symbol:" + val + ")");
        }
        return unitOfMeasure;
    }
}

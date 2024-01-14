package com.eingsoft.emop.tc.service.impl;

import static com.eingsoft.emop.tc.util.ProxyUtil.proxy;
import static com.eingsoft.emop.tc.util.ProxyUtil.spy;
import static java.util.Collections.emptyList;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcQueryService;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.internal.strong.core._2011_06.ICT;
import com.teamcenter.services.internal.strong.core._2011_06.ICT.InvokeICTMethodResponse;
import com.teamcenter.services.strong.query._2006_03.SavedQuery.GetSavedQueriesResponse;
import com.teamcenter.services.strong.query._2006_03.SavedQuery.SavedQueryObject;
import com.teamcenter.services.strong.query._2007_06.SavedQuery.ExecuteSavedQueriesResponse;
import com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryInput;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.ImanQuery;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.Person;
import com.teamcenter.soa.client.model.strong.RevisionRule;

@Log4j2
@ScopeDesc(Scope.TcContextHolder)
public class TcQueryServiceImpl implements TcQueryService {
    @Getter
    private TcContextHolder tcContextHolder;

    public TcQueryServiceImpl(TcContextHolder tcContextHolder) {
        this.tcContextHolder = tcContextHolder;
    }

    /**
     * 获取查询构建器中定义的所有查询对象
     * 
     * @return
     */
    @Override
    public List<SavedQueryObject> getQueries() {
        SavedQueryObject[] queries = new SavedQueryObject[0];
        try {
            GetSavedQueriesResponse savedQueries = tcContextHolder.getSavedQueryService().getSavedQueries();
            queries = savedQueries.queries;
        } catch (ServiceException e) {
            log.error(e.getMessage(), e);
        }
        return Arrays.asList(queries);
    }

    /**
     * 获取查询构建器中定义的所有查询对象，并构造Map返回， key:queryName, value: ImanQuery.
     * 
     * @return
     */
    @Override
    public Map<String, ImanQuery> getQueriesMap() {
        List<SavedQueryObject> queries = getQueries();
        Map<String, ImanQuery> queryMap = new HashMap<>();
        for (SavedQueryObject query : queries) {
            queryMap.put(query.name, query.query);
        }
        return queryMap;
    }

    /**
     * 根据queryName, 取回ImanQuery对象。
     * 
     * @param queryName
     * @return
     */
    @Override
    public ImanQuery getQueryByName(String queryName) {
        if (Strings.isNullOrEmpty(queryName)) {
            log.info("the query name must not be null.");
        }
        Map<String, ImanQuery> queryMap = getQueriesMap();
        ImanQuery query = queryMap.get(queryName);
        return query == null ? null : proxy(query, tcContextHolder);
    }

    /**
     * 根据item ID 查找Item对象，参数支持通佩符。 查询最大返回结果100
     * 
     * @param itemId
     * @return
     */
    @Override
    public List<Item> queryItemsById(String itemId) {
        ImanQuery itemIdQuery = getQueryByName("Item ID");
        return executeQuery(itemIdQuery, Arrays.asList("Item ID"), Arrays.asList(itemId), Item.class, 100);
    }

    @Override
    public <T extends ModelObject> List<T> executeQuery(ImanQuery query, List<String> entries, List<String> values,
        Class<T> clz) {
        // 不限数量
        return executeQuery(query, entries, values, clz, 0);
    }

    private RevisionRule[] queryRevRules() {
        ImanQuery query = getQueryByName("__WEB_rev_rules");
        SavedQueryInput savedQueryInput[] = new SavedQueryInput[1];
        savedQueryInput[0] = buildQueryInput(query, 100, new String[] {}, new String[] {});

        ExecuteSavedQueriesResponse savedQueryResponse =
            this.tcContextHolder.getSavedQueryService().executeSavedQueries(savedQueryInput);
        tcContextHolder.printAndLogMessageFromServiceData(savedQueryResponse.serviceData);
        ModelObject[] objs = savedQueryResponse.arrayOfResults[0].objects;
        if (objs == null || objs.length == 0) {
            return new RevisionRule[0];
        }
        return Lists.newArrayList(objs).toArray(new RevisionRule[objs.length]);
    }

    @Override
    public RevisionRule queryLatestWorkRevRule() {
        RevisionRule[] rules = queryRevRules();
        RevisionRule latestRevRule = null;
        for (RevisionRule revisionRule : rules) {
            try {
                if ("Latest Working".equals(revisionRule.get_object_name())) {
                    latestRevRule = revisionRule;
                    break;
                }
            } catch (Exception e) {
                log.error(e.getLocalizedMessage());
            }
        }
        return latestRevRule;
    }

    /**
     * 根据item ID查找Item, 此方法精准查找，默认取查询结果的第一个对象。
     * 
     * @param itemId
     * @return
     */
    @Override
    public Item queryItemById(String itemId) {
        List<Item> items = queryItemsById(itemId);
        return items.size() != 0 ? items.get(0) : null;
    }

    /**
     * 查询所有Person对象， 返回结果最大为50万
     * 
     * @param queryAllPersonsName 定义查询的名称
     * @param entry 查询构建器中定义的条目关键字和名称, 定义为："per"
     * @return
     */
    @Override
    public List<Person> queryAllPersons(String queryAllPersonsName, String entry) {
        ImanQuery query = getQueryByName(queryAllPersonsName);
        SavedQueryInput savedQueryInput[] = new SavedQueryInput[1];
        savedQueryInput[0] = buildQueryInput(query, 500000, new String[] {entry}, new String[] {"*"});

        ExecuteSavedQueriesResponse savedQueryResponse =
            this.tcContextHolder.getSavedQueryService().executeSavedQueries(savedQueryInput);

        tcContextHolder.printAndLogMessageFromServiceData(savedQueryResponse.serviceData);
        ModelObject[] objs = savedQueryResponse.arrayOfResults[0].objects;
        if (objs == null || objs.length == 0) {
            return emptyList();
        }
        return Arrays.stream(objs).map(o -> proxy((Person)o, tcContextHolder)).collect(Collectors.toList());

    }

    /**
     * 构建QueryInput参数
     * 
     * @param query
     * @param maxNumToReturn
     * @param entries
     * @param values
     * @return
     */
    private SavedQueryInput buildQueryInput(ImanQuery query, int maxNumToReturn, String[] entries, String[] values) {
        SavedQueryInput input = new SavedQueryInput();
        input.query = query;
        input.maxNumToReturn = maxNumToReturn;
        input.limitListCount = 0;
        input.limitList = new ModelObject[0];
        input.entries = entries;
        input.values = values;
        input.maxNumToInflate = maxNumToReturn;
        return input;
    }

    /**
     * maxCount为0表示不限数量
     */
    @Override
    public <T extends ModelObject> List<T> executeQuery(ImanQuery query, List<String> entries, List<String> values,
        Class<T> clz, int maxCount) {
        SavedQueryInput savedQueryInput[] = new SavedQueryInput[1];
        savedQueryInput[0] = buildQueryInput(query, maxCount, entries.toArray(new String[entries.size()]),
            values.toArray(new String[values.size()]));

        ExecuteSavedQueriesResponse savedQueryResponse =
            this.tcContextHolder.getSavedQueryService().executeSavedQueries(savedQueryInput);
        tcContextHolder.printAndLogMessageFromServiceData(savedQueryResponse.serviceData);
        ModelObject[] objs = savedQueryResponse.arrayOfResults[0].objects;
        if (objs == null || objs.length == 0) {
            return emptyList();
        }
        return Arrays.stream(objs).map(o -> proxy((T)o, tcContextHolder)).collect(Collectors.toList());
    }

    @Override
    public ImanQuery createSavedQuery(String queryName, String queryDescription, String returnTypeName, String clause) {
        ICT.Arg[] args = new ICT.Arg[7];
        for (int i = 0; i < args.length; i++) {
            args[i] = new ICT.Arg();
        }
        args[0].val = "ImanQuery";
        args[1].val = "TYPE::ImanQuery::ImanQuery::POM_application_object";
        args[2].val = queryName;
        ICT.Structure structure = new ICT.Structure();
        ICT.Arg[] struArgs = new ICT.Arg[2];
        for (int i = 0; i < struArgs.length; i++) {
            struArgs[i] = new ICT.Arg();
        }
        struArgs[0].val = "true";
        struArgs[1].val = queryDescription;
        structure.args = struArgs;
        args[3].val = "";
        args[3].structure = new ICT.Structure[] {structure};
        args[4].val = returnTypeName;
        args[5].val = clause;
        args[6].val = "0";

        InvokeICTMethodResponse response;
        try {
            response = tcContextHolder.getICTService().invokeICTMethod("ICCTQuery", "create", args);
        } catch (ServiceException e) {
            throw new RuntimeException("fail to create SavedQuery " + queryName, e);
        }
        ServiceData data = response.serviceData;
        tcContextHolder.printAndLogMessageFromServiceData(data);
        if (data.sizeOfPlainObjects() > 0) {
            return (ImanQuery)spy(data.getPlainObject(0), tcContextHolder);
        } else {
            return null;
        }
    }
}

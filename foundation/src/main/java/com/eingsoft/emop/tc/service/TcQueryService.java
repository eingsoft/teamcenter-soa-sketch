package com.eingsoft.emop.tc.service;

import java.util.List;
import java.util.Map;

import com.teamcenter.services.strong.query._2006_03.SavedQuery.SavedQueryObject;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.ImanQuery;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.Person;
import com.teamcenter.soa.client.model.strong.RevisionRule;

import lombok.NonNull;

public interface TcQueryService extends TcService {

    /**
     * 获取查询构建器中定义的所有查询对象
     * 
     * @return
     */
    List<SavedQueryObject> getQueries();

    /**
     * 获取查询构建器中定义的所有查询对象，并构造Map返回， key:queryName, value: ImanQuery.
     * 
     * @return
     */
    Map<String, ImanQuery> getQueriesMap();

    /**
     * 根据queryName, 取回ImanQuery对象。
     * 
     * @param queryName
     * @return
     */
    ImanQuery getQueryByName(String queryName);

    /**
     * 根据item ID 查找Item对象，参数支持通佩符。 查询最大返回结果100
     * 
     * @param itemId
     * @return
     */
    List<Item> queryItemsById(String itemId);

    RevisionRule queryLatestWorkRevRule();

    /**
     * 根据item ID查找Item, 此方法精准查找，默认取查询结果的第一个对象。
     * 
     * @param itemId
     * @return
     */
    Item queryItemById(String itemId);

    /**
     * 查询所有Person对象， 返回结果最大为50万
     * 
     * @param queryAllPersonsName 定义查询的名称
     * @param entry 查询构建器中定义的条目关键字和名称, 定义为："per"
     * @return
     */
    List<Person> queryAllPersons(String queryAllPersonsName, String entry);

    <T extends ModelObject> List<T> executeQuery(@NonNull ImanQuery query, List<String> entries, List<String> values, @NonNull Class<T> clz);

    <T extends ModelObject> List<T> executeQuery(@NonNull ImanQuery query, List<String> entries, List<String> values, @NonNull Class<T> clz, int maxCount);

    ImanQuery createSavedQuery(String queryName, String queryDescription, String returnTypeName, String clause);
}

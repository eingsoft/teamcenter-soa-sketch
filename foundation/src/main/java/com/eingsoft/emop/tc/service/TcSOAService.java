package com.eingsoft.emop.tc.service;

import java.util.List;
import java.util.Map;
import com.eingsoft.emop.tc.model.ModelObject;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.internal.strong.core._2011_06.ICT;
import com.teamcenter.services.internal.strong.core._2011_06.ICT.InvokeICTMethodResponse;
import com.teamcenter.services.strong.core._2006_03.DataManagement.CreateItemsOutput;
import com.teamcenter.services.strong.core._2006_03.DataManagement.ItemProperties;
import com.teamcenter.services.strong.core._2007_01.DataManagement.VecStruct;
import com.teamcenter.services.strong.core._2008_06.DataManagement.CreateIn;
import com.teamcenter.services.strong.core._2008_06.DataManagement.CreateOut;
import com.teamcenter.services.strong.core._2010_09.DataManagement.PropInfo;
import com.teamcenter.soa.client.model.strong.Folder;
import com.teamcenter.soa.client.model.strong.GroupMember;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.User;

public interface TcSOAService extends TcService {

  /**
   * 获得当前登入用户信息
   * 
   * @return
   */
  User getUser();

  /**
   * 获取当前用户的成员
   *
   * @return
   */
  GroupMember getGroupMember();

  /**
   * 当前用户的Home文件夹
   * 
   * @return
   */
  Folder getHomeFolder();

  /**
   * 当前用户的Home文件夹
   * 
   * @return
   */
  Folder getNewstuffFolder();

  /**
   * 创建单个Item对象
   * 
   * @param itemProperty
   * @param container
   * @param relationProp
   */
  Item createItem(ItemProperties itemProperty, com.teamcenter.soa.client.model.ModelObject container, String relationProp);

  /**
   * 创建多个Item对象
   * 
   * @param itemProperties
   * @param container
   * @param relationProp
   */
  List<Item> createItems(List<ItemProperties> itemProperties, com.teamcenter.soa.client.model.ModelObject container, String relationProp);

  /**
   * 创建一个Item对象，带revision属性、container、relationType
   */
  Item createItems(String itemType, String itemId, String itemName, String clientId, Map<String, Object> itemPropMap,
      Map<String, Object> itemRevPropMap, com.teamcenter.soa.client.model.ModelObject container, String relationType);

  /**
   * 批量创建Items, 并以contents关系放到Newstuff 文件夹下; 如需要连带赋值属性创建，请调用batchCreateItems(CreateIn[] createIns)
   * 
   * @param itemProperties
   * @return
   */
  List<CreateItemsOutput> batchCreateItems(List<ItemProperties> itemProperties);

  /**
   * 批量创建Items; 如需要连带赋值属性创建，请调用batchCreateItems(CreateIn[] createIns)
   * 
   * @param itemProperties
   * @param container
   * @param relationProp
   * @return
   */
  List<CreateItemsOutput> batchCreateItems(List<ItemProperties> itemProperties, com.teamcenter.soa.client.model.ModelObject container,
      String relationProp);

  /**
   * 根据指定的Item Type创建Item对象， 若itemId为空， 则自动Assign； 对象以contents关系绑定在Newstuff文件夹下。
   * 
   * @param itemId
   * @param itemName
   * @param itemType
   */
  Item createItem(String itemId, String itemName, String itemType);

  /**
   * 创建的文件夹放在Newstuff下
   * 
   * @param folderName
   * @return
   */
  Folder createFolder(String folderName);

  /**
   * 创建的文件夹以relationType关系放在container中
   * 
   * @param folderName
   * @param container
   * @param relationType
   * @return
   */
  Folder createFolder(String folderName, com.teamcenter.soa.client.model.ModelObject container, String relationType);

  /**
   * 更新普通属性的值，BMIDE中定义的属性类型为字符串
   * 
   * @param modelObject
   * @param prop
   * @param value
   */
  void setProperties(com.teamcenter.soa.client.model.ModelObject modelObject, String prop, String value);

  /**
   * 更新数组属性的值， BMIDE中定义的属性类型为字符串数组
   * 
   * @param modelObject
   * @param prop
   * @param values
   */
  void setProperties(com.teamcenter.soa.client.model.ModelObject modelObject, String prop, List<String> values);

  /**
   * 修改对象属性值， value对应值的类型为: String, String[].
   * 
   * @param modelObject
   * @param map
   */
  void setProperties(com.teamcenter.soa.client.model.ModelObject modelObject, Map<String, Object> map);

  /**
   * 设置可修改属性的值
   * 
   * @param modelObjects
   * @param map
   */
  void setProperties(List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjects, Map<String, VecStruct> map);

  /**
   * 构造对象多个属性的值的信息， 用于批量设置多个属性
   * 
   * @param object business object
   * @param nameValuesMap prop-values map
   * @return
   */
  PropInfo buildPropInfo(com.teamcenter.soa.client.model.ModelObject object, Map<String, List<String>> nameValuesMap);

  /**
   * 支持同时更新多个对象，多个属性值的API
   * 
   * @param propInfos
   */
  void setProperties(PropInfo[] propInfos);

  /**
   * 支持同时更新多个对象，多个属性值的API
   *
   * @param propInfos
   */
  void setProperties(List<PropInfo> propInfos);

  /**
   * 支持同时更新多个对象，多个属性值的API
   * 
   * @param propInfo
   * @param options 可为空
   */
  void setProperties(List<PropInfo> propInfo, List<String> options);

  /**
   * 批量创建对象，以relationType保存至container下; 如果Container or relation type任意为空，创建的对象不以任何关系绑定。 <br>
   * 
   * @param createIns
   * @return
   */
  List<CreateOut> batchCreateObjects(List<CreateIn> createIns, com.teamcenter.soa.client.model.ModelObject container, String relationType);

  /**
   * 创建可对itemRev属性赋值的Item， 并保存至Newstuff文件夹下，返回值为Item, ItemRevision, Master对象数组
   * 
   * @param itemType
   * @param itemId
   * @param itemName
   * @param itemRevPropMap
   * @return
   */
  List<ModelObject> createItem(String itemType, String itemId, String itemName, Map<String, Object> itemRevPropMap);

  /**
   * 建立创建Item所需要的输入信息， itemPropMap用于保存非item_id, object_name属性, itemRevPropMap可选
   * 
   * @param itemType 为空则默认使用Item类型
   * @param itemId 为空，则自动AssignID
   * @param itemName 不建议为空
   * @param clientId 批量创建Item时有用， 用以区分返回的结果, 可选
   * @param itemPropMap item的属性与属性值对（为null则默认带出rev，不允许有必填属性使用null）
   * @param itemRevPropMap 不应包含item_id等非Revision属性和由item创建时附加赋值的属性
   *        itemRev的属性与属性值对（为null则默认带出rev，不允许有必填属性使用null）
   * @return
   */
  CreateIn buildItemInput(String itemType, String itemId, String itemName, String clientId, Map<String, Object> itemPropMap,
      Map<String, Object> itemRevPropMap);

  /**
   * 创建Model Object对象并以relationType保存至Container中
   * 
   * @param objectType 为空则默认创建Item对象
   * @param clientId
   * @param propMap
   * @param container
   * @param relationType
   * @return
   */
  List<ModelObject> createObject(String objectType, String clientId, Map<String, String> propMap,
      com.teamcenter.soa.client.model.ModelObject container, String relationType);

  /**
   * 根据对象类型构建创建对象的输入条件， 必填属性不能为null or empty
   * 
   * @param objectType
   * @param clientId
   * @param propMap 属性名， 创建的对象不允许为空的属性必填在此处
   * @return
   */
  CreateIn buildCreateIn(String objectType, String clientId, Map<String, String> propMap);

  /**
   * 查找指定ItemId的对象
   * 
   * @param modelObjects
   * @param itemId
   * @return
   */
  ModelObject filterObjsByItemId(List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjects, String itemId);

  /**
   * 查找引用某个对象的对象们
   * 
   * @param obj
   * @param relType
   * @param objType 对象类型，可以用英文逗号分割
   * @return
   */
  List<ModelObject> findWhereReferenced(com.teamcenter.soa.client.model.ModelObject obj, String relType, String objType, int level);

  /**
   * 刷新对象
   */
  void refreshObj(List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjs);

  /**
   * 刷新对象
   */
  void refreshObjs(com.teamcenter.soa.client.model.ModelObject modelObjs);

  /**
   * 当前对象是否开户旁路权限
   * 
   * @return
   */
  boolean hasByPass();

  /**
   * 开启旁路权限
   */
  void openByPass();

  /**
   * 关闭旁路权限
   */
  void closeByPass();

  /**
   * 批量创建Items, 并以contents关系放到Newstuff 文件夹下; 如需要连带赋值属性创建，请调用batchCreateItems(CreateIn[] createIns)
   * 
   * @param itemProperties
   * @return
   */
  List<CreateItemsOutput> batchCreateItemsAndPutToNewstuff(List<ItemProperties> itemProperties);

  /**
   * 执行ICT函数，主要用以执行没有暴露SOA Java接口的调用
   * 
   * @param className
   * @param methodName
   * @param argList
   * @return
   */
  InvokeICTMethodResponse invokeICTMethod(String className, String methodName, List<String> argList);

  InvokeICTMethodResponse invokeICTMethod(String className, String methodName, ICT.Arg[] args);

  /**
   * Get the user's groups, please use the API in {@link TcOrgnizationService}
   * 
   * @param user
   * @return
   */
  @Deprecated
  List<ModelObject> getUserGroups(User user);

  /**
   * please use the API in {@link TcOrgnizationService}
   */
  @Deprecated
  User findUserByUserId(String userId);

  CreateIn buildItemInput(String itemType, String revisionType, String itemId, String itemName, String clientId,
      Map<String, Object> itemPropMap, Map<String, Object> itemRevPropMap);

  /**
   * 在TC对象中，给表类型的属性，添加多行数据
   * 
   * @param targetObj 如版本对象
   * @param tableType 表定义的类型， 表与版本对象是引用关系
   * @param tableProp 表类型在目标对象中的属性名
   * @param columnName2ValueMaps 一行数据对应一个map 对象， map中对应每一列的列名 和属性名
   */
  void addRowsToTable(com.teamcenter.soa.client.model.ModelObject targetObj, String tableType, String tableProp,
      List<Map<String, String[]>> columnName2ValueMaps);

  /**
   * 在TC对象中，给表类型的属性，添加一行数据
   * 
   * @param targetObj 如版本对象
   * @param tableType 表定义的类型， 表与版本对象是引用关系
   * @param tableProp 表类型在目标对象中的属性名
   * @param columnName2ValueMap map中对应每一列的列名 和属性名
   */
  void addRowToTable(com.teamcenter.soa.client.model.ModelObject targetObj, String tableType, String tableProp,
      Map<String, String[]> columnName2ValueMap);

  /**
   * 为对象的引用类型的属性赋值
   * 
   * @param primary 主对象
   * @param secondaryUid 次对象UID
   * @param property 引用类型的属性
   * @throws ServiceException
   */
  void setRefProperty(com.teamcenter.soa.client.model.ModelObject primary, String secondaryUid, String property);
}

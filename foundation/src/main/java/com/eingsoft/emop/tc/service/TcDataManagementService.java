package com.eingsoft.emop.tc.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import com.eingsoft.emop.tc.service.impl.TcDataManagementServiceImpl.ItemRevInfo;
import com.eingsoft.emop.tc.service.impl.TcDataManagementServiceImpl.ParentRelationInfo;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.exceptions.NotLoadedException;

public interface TcDataManagementService extends TcLoadService, TcService {

  /**
   * 根据ItemID获取具体的Item， 请使用 findItems()
   * 
   * @param itemId
   * @return
   */
  @Deprecated
  public Item getItemByItemId(String itemId, String revId);

  /**
   * 根据ItemIds获取Items， 请使用 findSpecificItemRevision()
   * 
   * @param itemIds
   * @return
   */
  @Deprecated
  public List<Item> getItemsByItemIds(List<String> itemIds, String revId);

  /**
   * 根据item id(可含通配符*)查找所有相关item 和 revision 信息， 返回一个MAP Map.key item_id <br>
   * MAP.value ItemRevInfo 包含 item, latestItemRevision 和 多个revision构成的 map(版本ID：revision) <br>
   * 
   * @param itemId
   * @return 返回的对象已经spy() 处理
   */
  Map<String, ItemRevInfo> findItemRevInfoMapByItemId(String itemId);

  ItemRevInfo findItemRevInfoByItemId(String itemId);

  /**
   * 根据item id(可含通配符*)查找所有相关item 和 revision 信息， 返回一个MAP Map.key item_id <br>
   * MAP.value ItemRevInfo 包含 item, latestItemRevision 和 多个revision构成的 map(版本ID：revision) <br>
   * 
   * @param itemId
   * @param itemType 若为空，则不进行类型过滤
   * @return
   */
  Map<String, ItemRevInfo> findItemRevInfoMapByItemIdAndObjectType(String itemId, String itemType);

  /**
   * 根据item id(可含通配符*)查找所有相关item 和 revision 信息， 返回一个MAP Map.key item_id <br>
   * MAP.value ItemRevInfo 包含 item, latestItemRevision 和 多个revision构成的 map(版本ID：revision) <br>
   * 
   * @param itemId
   * @param itemTypes 多个对象类型的item type， 若为空，则不进行类型过滤
   * @return
   */
  Map<String, ItemRevInfo> findItemRevInfoMapByItemIdAndObjectTypes(String itemId, List<String> itemTypes);

  /**
   * 根据item id(可含通配符*)查找所有相关item 和 revision 信息， 返回一个MAP Map.key item_id <br>
   * MAP.value ItemRevInfo 包含 item, latestItemRevision 和 多个revision构成的 map(版本ID：revision) <br>
   * 
   * @param prop2valConditionMap item_id的参数必填且非空值， 否则返回 empty map
   * @return
   */
  Map<String, ItemRevInfo> findItemRevInfoMapByCondition(Map<String, String> prop2valConditionMap);

  /**
   * 根据指定的item id 查找 item列表
   * 
   * @param itemId 如果需要找到多个item, itemId 需要加 * 通配符，如00168*
   * @return
   */
  List<Item> findItems(String itemId);

  /**
   * 根据指定的item id 查找 item
   * 
   * @param itemId itemId 不包括 * 通配符， 否则返回的对象为 NULL
   * @return
   */
  Item findItem(String itemId);

  /**
   * 根据指定的 item id 找到该item下的最新版本对象
   * 
   * @param itemId 不要包括 * 通配符，否则返回的对象为 NULL
   * @return
   */
  ItemRevision findLatestItemRevision(String itemId);

  /**
   * 根据指定的 item id 找到该item下的指定的版本对象
   * 
   * @param itemId 不要包括 * 通配符，否则返回的对象为 NULL
   * @param revId 如A, B, C ...
   * @return
   */
  ItemRevision findSpecificItemRevision(String itemId, String revId);

  /**
   * 根据指定版本对象，查找其上一个版本对象，如果当前版本对象为初始A版本，则返回其本身；否则找到上一个版本对象
   * 
   * @param itemRev
   * @return
   */
  ItemRevision findPreviousItemRevision(ModelObject itemRev);

  /**
   * 根据指定版本对象，查找其下一个版本对象，如果当前版本对象为最新版本对象，下一个版本对象为 null
   * 
   * @param itemRev
   * @return
   */
  ItemRevision findNextItemRevision(ModelObject itemRev);

  /**
   * 根据UID 返回 信息
   * @param uid 可以是 item 或 Revision 版本的 UID
   * @return
   */
  ItemRevInfo findItemRevInfoByUid(String uid);

  Map<String, ItemRevInfo> findItemRevInfoMapByUids(List<String> uids);

  /**
   * 找到任意状态的版本对象，如未找到，则往上找一个版本，上一个版本还未找到，则返回 null
   * 
   * @param itemId
   * @return
   */
  com.eingsoft.emop.tc.model.ModelObject findLatestAnyStatusItemRev(String itemId);

  /**
   * 找到发布状态的版本对象，如未找到，则往上找一个版本，上一个版本还未找到，则返回 null
   * 
   * @param itemId
   * @return
   */
  com.eingsoft.emop.tc.model.ModelObject findLatestApprovedItemRevision(String itemId);

  /**
   * 指定状态名称相同的，如未找到，则往上找一个版本，上一个版本还未找到，则返回 null
   * 
   * @param itemId
   * @param statusName null 则支持任何状态； 多个状态 可以逗号分开； 单个状态则直接填写状态名； eg: Approved
   * @return
   */
  com.eingsoft.emop.tc.model.ModelObject findLatestSpecificStatusItemRev(String itemId, String statusName);

  /**
   * 判断对象的 最近 状态 是否为指定状态
   * 
   * @param tcObj
   * @param statusName null 则支持任何状态； 多个状态 可以逗号分开； 单个状态则直接填写状态名； eg: Approved
   * @return
   */
  boolean isSpecificStatus(ModelObject tcObj, String statusName);

  /**
   * 判断对象的最近状态是否为 发布状态 Approved
   * 
   * @param tcObj
   * @return
   */
  boolean isApprovedStatus(ModelObject tcObj);

  /**
   * 判断对象 是否有状态（最近状态 是否存在）
   * 
   * @param tcObj
   * @return
   */
  boolean isAnyStatus(ModelObject tcObj);

  /**
   * refresh the objects, expire the tc server cache, pay attention that the refresh object will not
   * fast enough, if you have a large number of objects to be refreshed, it will lead to a
   * performance issue, you are strongly recommended to use {@link EphemeralCredentialContextHolder}
   * to login each time to avoid a large number of objects refreshing.
   * 
   * @param objs
   */
  public void refreshObjects(List<? extends ModelObject> objs);

  /**
   * delete the objects
   * 
   * @param objs
   */
  void deleteModelObjects(Collection<? extends ModelObject> objs);

  /**
   * delete the objects with deleting relation first
   * 
   * @param objs
   */
  void deleteModelObjects(String relationType, ModelObject primaryObj, Collection<? extends ModelObject> secondaryObjs);

  /**
   * delete the secondaryObjs with deleting relation first, the primaryObjs size should be the same
   * as secondaryObjs, primaryObjs[i] is the parent, and secondaryObjs[i] is the child object
   * primaryObjs[i] and secondaryObjs[i] should have the relationType relationship
   * 
   */
  void deleteModelObjects(String relationType, List<? extends ModelObject> primaryObjs, Collection<? extends ModelObject> secondaryObjs);

  /**
   * change object ownership
   */
  void changeOwnership(List<ModelObject> modelObjects, ModelObject user, ModelObject group);

  /**
   * change object ownership,have no group parameter,load user's default group
   * 
   * @throws NotLoadedException
   */
  void changeOwnership(List<ModelObject> modelObjects, ModelObject user);

  /**
   * 基于版本对象根据指定的 版本ID 进行修订升版， 注意:如果newRevId已经存在, 返回null
   * 
   * @param itemRev
   * @param newRevId
   * @return 如果升版失败将返回空
   */
  ItemRevision revise(ItemRevision itemRev, String newRevId);

  /**
   * 将版本对象进行升版， 新的版本ID自动获取; 若升版失败，返回 null
   * 
   * @param itemRev
   * @return
   */
  ItemRevision revise(ItemRevision itemRev);

  /**
   * 升版多个 版本对象， 新的版本ID 自动获取; 若升版失败，则返回空的MAP； 若部分失败，则对应的key(item revision) 不在map中
   * 
   * @param itemRevs
   * @return map key:原始版本 value: 升版后的版本
   */
  Map<ItemRevision, ItemRevision> revises(List<ItemRevision> itemRevs);

  /**
   * 根据UID 返回升版(新版本ID自动获取)后的对象, 若升版失败，则返回空的MAP； 若部分失败，则对应的key(UID) 不在map中
   * 
   * @param revUids 会过滤，仅升版版本对象的类型
   * @return
   */
  Map<String, ItemRevision> reviseWithUids(List<String> revUids);

  /**
   * 根据UID 返回升版(新版本ID自动获取)后的对象, 若升版失败，则返回空的MAP； 若部分失败，则对应的key(UID) 不在map中
   * 
   * @param revUids 会过滤，仅升版版本对象的类型
   * @return
   */
  Map<ItemRevision, ItemRevision> reviseWithRevs(List<String> revUids);

  /**
   * 基于版本对象另存新的对象
   * 
   * @param itemRev 为空，则跳过处理，返回null
   * @param newItemId 为空，则自动分配ID
   * @param newName 为空，将使用系统默认值
   * @return
   */
  ItemRevision saveAs(ItemRevision itemRev, String newItemId, String newName);

  /**
   * 根据对象类型，找到其下的 所有子结点对象[伪文件（关系）对象除外], 子节点为空，则返回空list
   * 
   * @param modelObject
   * @return
   */
  public List<com.eingsoft.emop.tc.model.ModelObject> findChildren(ModelObject modelObject);

  /**
   * 根据当前 对象，找到其子类下的对象[ 伪文件（关系）对象除外]，并根据对象类型进行过滤，包括其子类。 子节点为空，则返回空list
   * 
   * @param modelObject
   * @param objectType 如： Part_0_Revision_alt(Part Revision类型的存储类) or ItemRevision
   * @return
   */
  public List<com.eingsoft.emop.tc.model.ModelObject> findChildren(ModelObject modelObject, String objectType);

  /**
   * 根据多个对象，查找其所有的子节点对象， 最终构造对应的MAP返回，子节点为空，则返回空list
   * 
   * @param modelObjects
   * @return
   */
  public Map<com.eingsoft.emop.tc.model.ModelObject, List<com.eingsoft.emop.tc.model.ModelObject>> findChildren(
      List<? extends com.teamcenter.soa.client.model.ModelObject> modelObjects);

  /**
   * 查找多个对象下的 子节点
   * 
   * @param modelObjects
   * @param filterObjectTypes 可为空, 支持多个 以英文逗号隔开
   * @return
   */
  Map<com.eingsoft.emop.tc.model.ModelObject, List<com.eingsoft.emop.tc.model.ModelObject>> findChildren(
      List<? extends ModelObject> modelObjects, String filterObjectTypes);

  /**
   * 返回此对象中，关系下的所有对象 [ 仅伪文件（关系）对象] 与 TcRelationshipService 中的 方法配合使用，此方法更灵活
   * 
   * @param modelObject
   * @param relationName 不为空
   * @return
   */
  List<com.eingsoft.emop.tc.model.ModelObject> findChildrenWithRel(ModelObject modelObject, String relationName);

  /**
   * 返回此对象下的，某个关系中，指定的对象类型 [ 仅伪文件（关系）对象] 与 TcRelationshipService 中的 方法配合使用，此方法更灵活
   * 
   * @param modelObject
   * @param relationName 不为空
   * @param objectType
   * @return
   */
  List<com.eingsoft.emop.tc.model.ModelObject> findChildrenWithRel(ModelObject modelObject, String relationName, String objectType);

  /**
   * key 为关系名 [ 仅伪文件（关系）对象] 与 TcRelationshipService 中的 方法配合使用，此方法更灵活
   * 
   * @param modelObj
   * @return 返回参数对象中关系下所对应的所有值
   */
  Map<String, List<com.eingsoft.emop.tc.model.ModelObject>> findChildrenWithRel(ModelObject modelObj);

  /**
   * 一次性批量加载多个对象下的指定个关系 返回 父的和子列表
   * 
   * @param modelObjs
   * @param refRelation 不为空
   * @param filterObjectTypes 可为空
   * @return
   */
  Map<com.eingsoft.emop.tc.model.ModelObject, List<com.eingsoft.emop.tc.model.ModelObject>> findChildrenWithRel(List<ModelObject> modelObjs,
      String refRelation, String filterObjectTypes);

  /**
   * 一次性批量加载多个对象下的多个关系属性
   * 
   * @param modelObjs
   * @param refRelation 可为空
   * @param filterObjectTypes 可为空
   * @return
   */
  List<ParentRelationInfo> findParentAndChildrenWithRel(List<ModelObject> modelObjs, String refRelation, String filterObjectTypes);

  /**
   * 根据版本对象 获取 ps_parent active 的对象， 排除小版本 123 无效对象 和 ABC大版本中的小版本， 取 C3 版本对象 <br>
   * 此方法 默认只取第一个，对于确定只有一个父的对象，方可使用此方法; 如果不存在，则返回null
   * 
   * @param itemRev
   * @return
   */
  com.eingsoft.emop.tc.model.ModelObject findLatestActivePSParent(ModelObject itemRev);

  /**
   * 根据版本对象 获取 ps_parent active 的对象， 排除小版本 123 无效对象 和 ABC大版本中的小版本， 取 C3 版本对象 <br>
   * 如果不存在，则返回空list
   * 
   * @param itemRev
   * @return
   */
  List<com.eingsoft.emop.tc.model.ModelObject> findLatestActivePSParents(ModelObject itemRev);

  /**
   * 根据版本对象 获取 ps_parent active 的对象，对于多个对象，则以itemId 为key, 最新对象 为value, 返回map <br>
   * 如果不存在，则返回空的map
   * 
   * @param itemRev
   * @return
   */
  Map<String, com.eingsoft.emop.tc.model.ModelObject> findLatestActivePSParentMap(ModelObject itemRev);

  /**
   * 传入 item的对象类型名称，如果输入错误或者TC中未定义此类型， 返回 空字符串 <br>
   * 参照TC中指派流水号生成item_id： 一旦生成此流水号之后 ，后续TC其它操作 都不会占用已经生成的 流水号
   * 
   * @param itemType
   * @return
   */
  String generateItemId(String itemType);

  /**
   * 同时查询多个item ID, 支持通佩符*， 版本中会带有数据集
   * 
   * @param itemIds
   * @return
   */
  Map<String, ItemRevInfo> findItemRevInfoMapByItemIds(List<String> itemIds);

  /**
   * 同时查询多个item ID, 支持通佩符*， 版本中会带有数据集
   * 
   * @param prop2valConditionMaps 支持其它类型参数过滤条件
   * @return
   */
  Map<String, ItemRevInfo> findItemRevInfoMapByCondition(List<Map<String, String>> prop2valConditionMaps);

}

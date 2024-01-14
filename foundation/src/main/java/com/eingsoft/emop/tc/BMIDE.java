package com.eingsoft.emop.tc;

/**
 * BMIDE中原生定义的属性、类型、关系等其它变量值。
 * 
 * @author king
 */
public class BMIDE {
  /**
   * 回车换行符号: "\n"
   */
  public static String ENTER = "\n";

  /**
   * 星号 通佩符: "*"
   */
  public static String ASTERISK = "*";

  /**
   * 分号: ";"
   */
  public static String SEMICOLON = ";";

  /**
   * comma 英文逗号: ","
   */
  public static String COMMA = ",";

  /**
   * colon 英文冒号 ":"
   */
  public static String COLON = ":";

  /**
   * equals: "="
   */
  public static String EQ = "=";

  /**
   * more than: ">"
   */
  public static String MORE = ">";

  /**
   * less than: "<"
   */
  public static String LESS = "<";

  /**
   * 点位符 : "."
   */
  public static final String DOT = ".";

  /**
   * 一个空格占位符 : " "
   */
  public static final String EMPTY_SPACE = " ";

  /**
   * 下划线 : _
   */
  public static final String UNDERLINE = "_";

  /**
   * 横杠连接符 : -
   */
  public static final String HYPHEN = "-";

  /**
   * 斜线: /
   */
  public static final String SLASH = "/";

  /**
   * 反斜线 : \
   */
  public static final String BACKSLASH = "\\";

  /**
   * 左圆括号 : (
   */
  public static final String LEFT_PARENTHESE = "(";

  /**
   * 右圆括号 : )
   */
  public static final String RIGHT_PARENTHESE = ")";

  // -------------Business object/type start ----------------------
  /**
   * TCComponentItem type: Item
   */
  public static final String TYPE_ITEM = "Item";

  /**
   * TCComponentForm type: Form
   */
  public static final String TYPE_FORM = "Form";

  /**
   * TCComponent Query type: ImanQuery
   */
  public static final String TYPE_QUERY = "ImanQuery";

  /**
   * TCComponent DirectModel type: DirectModel
   */
  public static final String TYPE_DIRECTMODEL = "DirectModel";

  /**
   * TCComponent BOMWindow type: BOMWindow
   */
  public static final String TYPE_BOM_WINDOW = "BOMWindow";

  /**
   * TCComponent BOMLine type: BOMLine
   */
  public static final String TYPE_BOM_LINE = "BOMLine";

  /**
   * TCComponent RevisionRule type: "RevisionRule"
   */
  public static final String TYPE_REVISION_RULE = "RevisionRule";

  /**
   * TCComponentFolder type: Folder
   */
  public static final String TYPE_FOLDER = "Folder";

  /**
   * TCComponentDataset type: Dataset
   */
  public static final String TYPE_DATASET = "Dataset";

  /**
   * JT 数据集的对象类型 DirectModel
   */
  public static final String TYPE_DirectModel = "DirectModel";

  /**
   * TCComponentTaskTemplate type: EPMTaskTemplate
   */
  public static final String TYPE_EPM_TASK_TEMPLATE = "EPMTaskTemplate";

  /**
   * 审核任务 EPMReviewTask
   */
  public static final String TYPE_REVIEW_TASK = "EPMReviewTask";

  /**
   * EPMReviewTask sub task: EPMSelectSignoffTask 选择签审人
   */
  public static final String TYPE_SELECT_SIGN_OFF_TASK = "EPMSelectSignoffTask";

  /**
   * EPMReviewTask sub task: EPMPerformSignoffTask 执行签审
   */
  public static final String TYPE_PERFORM_SIGN_OFF_TASK = "EPMPerformSignoffTask";

  /**
   * type: MEOPRevision
   */
  public static final String TYPE_ME_OP_REVISION = "MEOPRevision";

  /**
   * type: Mfg0MECompOPRevision
   */
  public static final String TYPE_ME_COMP_OP_REVISION = "Mfg0MECompOPRevision";

  /**
   * DataSet Type: MSExcelX
   */
  public static final String DATASET_EXCELX_TYPE = "MSExcelX";
  // -------------Business object/type end ----------------------

  // -------------------------------
  /**
   * DataSet EXCEL reference name in tab table: excel
   */
  public static final String DATASET_REF_EXCELX_NAME = "excel";

  // ----------------------------------

  // -------------property name start ----------------------
  /**
   * Home folder property: Home
   */
  public static final String PROP_HOME_FOLDER = "Home";

  /**
   * 工艺与产品关联关系: IMAN_METarget
   */
  public static final String REL_IMAN_METarget = "IMAN_METarget";

  /**
   * 工艺与工作区域关联关系: IMAN_MEWorkArea
   */
  public static final String REL_IMAN_MEWorkArea = "IMAN_MEWorkArea";

  /**
   * Folder property: contents
   */
  public static final String REL_CONTENTS = "contents";

  /**
   * Revision property: TC_Attaches
   */
  public static final String REL_TC_ATTACHES = "TC_Attaches";

  /**
   * Item relation with form property: IMAN_master_form_rev
   */
  public static final String REL_MASTER_FORM = "IMAN_master_form_rev";

  /**
   * Item relation with data property: IMAN_specification
   */
  public static final String REL_SPECIFICATION = "IMAN_specification";

  /**
   * Item relation with data property: IMAN_Rendering
   */
  public static final String REL_RENDERING = "IMAN_Rendering";

  /**
   * ItemRevision relation with data property: IMAN_classification
   */
  public static final String REL_CLASSIFICATION = "IMAN_classification";

  /**
   * ItemRevision property: item_revision_id
   */
  public static final String PROP_ITEM_REV_ID = "item_revision_id";

  /**
   * TC object property: object_name
   */
  public static final String PROP_OBJECT_NAME = "object_name";

  /**
   * TC object property: name
   */
  public static final String PROP_NAME = "name";

  /**
   * TC object property: object_type
   */
  public static final String PROP_OBJECT_TYPE = "object_type";

  /**
   * TC object property: object_string
   */
  public static final String PROP_OBJECT_STRING = "object_string";

  /**
   * TC object property: user_name
   */
  public static final String PROP_USER_NAME = "user_name";

  /**
   * TC object property: user_id
   */
  public static final String PROP_USER_ID = "user_id";

  /**
   * 责任人: responsible_party
   */
  public static final String PROP_RESP_USER = "responsible_party";

  /**
   * 流程任务的状态: task_state， 如待开始，已开始，已完成等
   */
  public static final String PROP_TASK_STATE = "task_state";

  /**
   * Group property: full_name
   */
  public static final String PROP_FULL_NAME = "full_name";

  /**
   * TC object property: object_desc
   */
  public static final String PROP_OBJECT_DESC = "object_desc";

  /**
   * TC object property: creation_date
   */
  public static final String PROP_CREATION_DATE = "creation_date";

  /**
   * TC object property: last_mod_date
   */
  public static final String PROP_LAST_MOD_DATE = "last_mod_date";

  /**
   * TC object property: item_id
   */
  public static final String PROP_ITEM_ID = "item_id";

  /**
   * 流程中的子任务: child_tasks
   */
  public static final String PROP_CHILD_TASKS = "child_tasks";

  /**
   * 流程中的任务的前置任务的名称: predecessors
   */
  public static final String PROP_PREDECESSORS = "predecessors";

  /**
   * Runtime Prop 获取版本对象下的零组件子对象(取出的值是乱序): ps_children
   */
  public static final String PROP_PS_CHILDREN = "ps_children";

  /**
   * Runtime Prop 获取版本对象下的零组件父对象: ps_parents
   */
  public static final String PROP_PS_PARENTS = "ps_parents";

  /**
   * TC object property: active_seq
   */
  public static final String PROP_ACTIVE_SEQ = "active_seq";

  /**
   * TC object property: items_tag
   */
  public static final String PROP_ITEMS_TAG = "items_tag";

  /**
   * 时间表任务获取 时间表的属性 schedule_tag
   */
  public static final String PROP_Schedule_Tag = "schedule_tag";

  /**
   * TC 分类属性的标识： 入了分类库，则其值为 YES
   */
  public static final String PROP_CIS_CLASSIFIED = "ics_classified";

  /**
   * TC object property: revision_list
   */
  public static final String PROP_REVISION_LIST = "revision_list";

  /**
   * TC object property: template_name
   */
  public static final String PROP_TEMPLATE_NAME = "template_name";

  /**
   * TC object property: release_status_list
   */
  public static final String PROP_RELEASE_STATUS_LIST = "release_status_list";

  /**
   * TC object property: structure_revisions
   */
  public static final String PROP_STRUCTURE_REVISIONS = "structure_revisions";

  /**
   * TC object property: last_release_status
   */
  public static final String PROP_LAST_RELEASE_STATUS = "last_release_status";

  /**
   * ItemRevision property: EC_problem_item_rel
   */
  public static final String PROP_EC_PROBLEM_ITEM_REL = "EC_problem_item_rel";

  /**
   * ItemRevision property: EC_affected_item_rel
   */
  public static final String PROP_EC_AFFECTED_ITEM_REL = "EC_affected_item_rel";

  /**
   * BOMLine property: bl_plmxml_occ_xform
   */
  public static final String PROP_OCC_XFORM = "bl_plmxml_occ_xform";

  /**
   * BOMLine property: bl_sequence_no
   */
  public static final String PROP_BL_SEQUENCE_NO = "bl_sequence_no";

  /**
   * BOMLine property: bl_quantity
   */
  public static final String PROP_BL_QUANTITY = "bl_quantity";

  /**
   * BOMLine property: bl_has_children
   */
  public static final String PROP_BL_HAS_CHILDREN = "bl_has_children";

  /**
   * BOMLine property: bl_indented_title
   */
  public static final String PROP_BL_TITLE = "bl_indented_title";

  /**
   * BOMLine property: bl_all_child_lines
   */
  public static final String PROP_BL_ALL_CHILD_LINES = "bl_all_child_lines";

  /**
   * BOMLine property: bl_clone_stable_occurrence_id, BOMLINE唯一不变的ID
   */
  public static final String PROP_bl_clone_stable_occurrence_id = "bl_clone_stable_occurrence_id";

  /**
   * BOMLine property: bl_item
   */
  public static final String PROP_BL_ITEM = "bl_item";

  /**
   * BOMLine property: bl_revision
   */
  public static final String PROP_BL_REVISION = "bl_revision";

  /**
   * MEActivity property: root_activity
   */
  public static final String PROP_Root_Activity = "root_activity";

  /**
   * Item/Revision property: owning_user
   */
  public static final String PROP_OWNING_USER = "owning_user";

  /**
   * Item/Revision property: owning_group
   */
  public static final String PROP_OWNING_GROUP = "owning_group";

  /**
   * EPMTask 备注属性 comments
   */
  public static final String PROP_COMMENTS = "comments";

  /**
   * item property: uom_tag
   */
  public static final String PROP_UOM_TAG = "uom_tag";

  /**
   * UnitOfMeasure property: symbol
   */
  public static final String PROP_SYMBOL = "symbol";

  /**
   * ScheduleTask property
   */
  public static final String PROP_FND0PARENTTASK = "fnd0ParentTask";
  // -------------property name end ----------------------

  // -----------------------Query Structure start -----------------------
  /**
   * Process templates name: __Process_Templates
   */
  public static final String QUERY_PROCESS_TEMPLATES_NAME = "__Process_Templates";

  /**
   * Query attribute name: template_classification
   */
  public static final String QUERY_ATTR_TEMPLATE_CLASSIFICATION = "template_classification";

  /**
   * Query attribute name: DatasetName
   */
  public static final String QUERY_ATTR_DATASETNAME_KEY = "DatasetName";
  // -----------------------Query Structure end -----------------------

  // -----------------------Misc Constant------------------------------
  public static final boolean UNIT_OF_MEASURE_FAST_MODE = true;
  // -----------------------Misc Constant------------------------------

  // ------------------------Schedule constant start-----------------------------
  /**
   * A load schedules option which can be set to one of the following values: 1) 0 -> load the full
   * schedule including all sub schedules and their children . 2) 1 -> load only schedule summaries
   * partially
   */
  public static final String SM_STRUCTURE_PARTIAL_CONTEXT = "SM_Structure_Partial_Context";
  /**
   * Integer Option: "SM_Structure_Load_Context" Values: 0 = loading schedule 1 = loading
   * sub-schedule 4 = inserting sub schedule by reference
   */
  public static final String SM_STRUCTURE_LOAD_CONTEXT = "SM_Structure_Load_Context";
  /**
   * Integer Option: "SM_Structure_Client_Context" Values: 0 = RAC client 1 = Server client (for
   * Synchronous dispatcher) 2 = MSP plugin client
   */
  public static final String SM_STRUCTURE_CLIENT_CONTEXT = "SM_Structure_Client_Context";

  // ------------------------Schedule constant end-------------------------------

  // ------------------------Classification constant start-----------------------------
  /**
   * Unit system of the class. Type of Class. Valid values are - Group = (1 << 0) Class = (1 << 1)
   * View = (1 << 2) Storage class = (1 << 4)
   */
  public static final String STORAGE_CLASS = "StorageClass";
  /**
   * Unit system of measure in which the Classification object is stored in.Usually use "METRIC";
   */
  public static final String METRIC = "METRIC";

  // ------------------------Classification constant end-------------------------------

}

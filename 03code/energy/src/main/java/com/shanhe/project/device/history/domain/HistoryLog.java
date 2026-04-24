package com.shanhe.project.device.history.domain;

import com.shanhe.project.device.config.domain.MonitorData;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 设备历史记录对象 dev_history_log
 *
 * @author wjh
 * @since 2024-12-31
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HistoryLog extends MonitorData
{
    private static final long serialVersionUID = 1L;

    /** 历史记录id */
    private Long historyId;

    /** 设备id */
    private Long configId;

    /** 包序号 */
    private Integer packNum;

    /** 历史项 */
    private String itemCode;

    /** 历史项 */
    private String itemName;
    /** 设备名称 */
    private String configName;

    /** 告警状态 0-是，1-否 */
    private Integer alarmStatus;

    /** 历史项列表 */
    private List<String> itemCodes;

    /** 属性信息 */
    private String dataInfo;

    /** 原始值信息 */
    private String valueInfo;




    // 导出路径
    private String exportPath;
}

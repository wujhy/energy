package com.shanhe.project.sync.domain;

import com.shanhe.project.device.config.domain.BatteryMonitor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 历史数据
 *
 * @author wjh
 * @since 2025/5/19
 */
@Data
public class ConfigHistoryVo implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 设备主键ID
     */
    private Long devId;
    /**
     * 蓄电池组编号，1,2,3,4
     */
    private Integer packNum;
    /**
     * 测点历史数据
     */
    private List<ConfigHistoryItemVo> listData;
    /**
     * 单体蓄电池
     */
    private List<BatteryMonitor> listData2;
}

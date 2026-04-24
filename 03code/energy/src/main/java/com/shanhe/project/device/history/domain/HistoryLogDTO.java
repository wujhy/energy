package com.shanhe.project.device.history.domain;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 设备历史记录对象 dev_history_log
 *
 * @author wjh
 * @since 2024-12-31
 */
@Data
@ColumnWidth(20)
public class HistoryLogDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ExcelProperty("设备名称")
    private String configName;

    @ExcelProperty("监控项")
    private String itemName;

    @ExcelProperty("数据信息")
    private String dataInfo;

    @ExcelProperty("采集时间")
    private Date createTime;

    public static HistoryLogDTO of(HistoryLog item) {
        HistoryLogDTO dto = new HistoryLogDTO();
        dto.setConfigName(item.getConfigName());
        dto.setItemName(item.getItemName());
        dto.setDataInfo(item.getDataInfo());
        dto.setCreateTime(item.getCreateTime());
        return dto;
    }
}

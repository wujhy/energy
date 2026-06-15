package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.mapper.BatteryModuleRealtimeMapper;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 标准实时模型只读适配服务。
 * <p>
 * 供 JSON/TCP、页面、告警等外部输出读取标准实时数据。
 * 当 jsonTcpRealtimeSourceEnabled=false 时返回 null，调用方应回退旧数据源。
 *
 * @author wjh
 * @since 2026-06-03
 */
@Slf4j
@Service
public class BatteryModuleRealtimeAdapterService {

    @Resource
    private BatteryCollectorProperties properties;

    @Resource
    private BatteryModuleRealtimeMapper realtimeMapper;

    @Resource
    private BatteryModuleRealtimeSnapshotService snapshotService;

    /**
     * 判断标准实时数据源是否启用。
     *
     * @return true 表示启用
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(properties.getJsonTcpRealtimeSourceEnabled());
    }

    /**
     * 查询指定电池组的单体实时数据。
     *
     * @param packNum 电池组编号
     * @return 单体实时数据列表；未启用或无数据时返回 null
     */
    public List<BatteryModuleCellRealtime> getCellRealtime(Integer packNum) {
        if (!isEnabled() || packNum == null) {
            return null;
        }
        try {
            com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot snapshot =
                    snapshotService == null ? null : snapshotService.getSnapshot(packNum);
            List<BatteryModuleCellRealtime> cells = snapshot == null ? realtimeMapper.selectCells(packNum) : snapshot.getCells();
            return (cells != null && !cells.isEmpty()) ? cells : null;
        } catch (Exception e) {
            log.warn("查询标准单体实时数据失败, packNum={}", packNum, e);
            return null;
        }
    }

    /**
     * 查询指定电池组的组实时数据。
     *
     * @param packNum 电池组编号
     * @return 组实时数据；未启用或无数据时返回 null
     */
    public BatteryModuleGroupRealtime getGroupRealtime(Integer packNum) {
        if (!isEnabled() || packNum == null) {
            return null;
        }
        try {
            com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot snapshot =
                    snapshotService == null ? null : snapshotService.getSnapshot(packNum);
            return snapshot == null ? realtimeMapper.selectGroup(packNum) : snapshot.getGroup();
        } catch (Exception e) {
            log.warn("查询标准组实时数据失败, packNum={}", packNum, e);
            return null;
        }
    }
}

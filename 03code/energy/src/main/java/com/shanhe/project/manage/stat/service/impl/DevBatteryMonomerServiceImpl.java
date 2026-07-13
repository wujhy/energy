package com.shanhe.project.manage.stat.service.impl;

import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.collector.battery.service.BatteryModuleRealtimeSnapshotService;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import com.shanhe.project.manage.stat.domain.DevBatteryMonomer;
import com.shanhe.project.manage.stat.mapper.DevBatteryMonomerMapper;
import com.shanhe.project.manage.stat.service.IDevBatteryMonomerService;
import com.shanhe.project.sync.domain.BatteryMonomerBatVo;
import com.shanhe.project.sync.service.ClientReportService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DevBatteryMonomerServiceImpl implements IDevBatteryMonomerService {

    @Resource
    private DevBatteryMonomerMapper devBatteryMonomerMapper;

    @Resource
    private IBatteryPackService batteryPackService;

    @Resource
    private BatteryModuleRealtimeSnapshotService realtimeSnapshotService;

    @Resource
    private ClientReportService clientReportService;

    @Override
    public List<DevBatteryMonomer> selectList(Integer packNum) {
        BatteryPack batteryPack = batteryPackService.selectBatteryInfoByPackNum(packNum);
        if (batteryPack == null) {
            return new ArrayList<>();
        }
        return devBatteryMonomerMapper.selectList(batteryPack.getPackId());
    }

    @Override
    public void init(Integer packNum) {
        BatteryPack batteryPack = batteryPackService.selectBatteryInfoByPackNum(packNum);
        if (batteryPack == null) {
            throw new RuntimeException("请先配置电池组信息");
        }

        List<BatteryModuleCellRealtime> batteryList = resolveBatteryCells(packNum);
        if (CollectionUtils.isEmpty(batteryList)) {
            throw new RuntimeException("暂无无上报数据");
        }

        for (BatteryModuleCellRealtime item : batteryList) {
            if (item.getResistance() == null || item.getResistance() <= 0) {
                throw new RuntimeException("单体【" + item.getBatNum() + "】内阻无效");
            }
        }

        deleteByPackId(batteryPack.getPackId());

        List<DevBatteryMonomer> devBatteryMonomers = batteryList.stream()
                .map(item -> new DevBatteryMonomer(batteryPack.getPackId(), item.getBatNum(), item.getResistance()))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(devBatteryMonomers)) {
            return;
        }
        devBatteryMonomerMapper.insertList(devBatteryMonomers);

        clientReportService.uploadBatteryMonomer(packNum, devBatteryMonomers, null);
    }

    @Override
    public void delete() {
        devBatteryMonomerMapper.delete();
    }

    @Override
    public Double getMaxResistance(Integer packNum) {
        List<BatteryModuleCellRealtime> batteryList = resolveBatteryCells(packNum);
        if (CollectionUtils.isEmpty(batteryList)) {
            return 0.0;
        }

        List<DevBatteryMonomer> devBatteryMonomers = selectList(packNum);
        if (devBatteryMonomers == null || devBatteryMonomers.isEmpty()) {
            return 0.0;
        }
        Map<Integer, Integer> monomerMap = devBatteryMonomers.stream()
                .collect(Collectors.toMap(DevBatteryMonomer::getBatNum, DevBatteryMonomer::getResistance, (v1, v2) -> v2));

        Double max = null;
        for (BatteryModuleCellRealtime batteryMonitor : batteryList) {
            Integer monomerResistance = monomerMap.get(batteryMonitor.getBatNum());
            if (monomerResistance == null || monomerResistance <= 0) {
                continue;
            }
            if (batteryMonitor.getResistance() == null || batteryMonitor.getResistance() <= 0) {
                continue;
            }
            if (batteryMonitor.getResistance() <= monomerResistance) {
                continue;
            }
            double percentage = (double) (batteryMonitor.getResistance() - monomerResistance) / monomerResistance;
            if (percentage > 1) {
                max = 1.0;
                break;
            }
            if (max == null || percentage > max) {
                max = percentage;
            }
        }

        return max == null ? 0.0 : max;
    }

    List<BatteryModuleCellRealtime> resolveBatteryCells(Integer packNum) {
        BatteryModuleRealtimeSnapshot snapshot = realtimeSnapshotService == null
                ? null : realtimeSnapshotService.getCachedSnapshot(packNum);
        return snapshot == null ? new ArrayList<>() : snapshot.getCells();
    }

    @Override
    public void deleteByPackId(Long packId) {
        devBatteryMonomerMapper.deleteByPackId(packId);
    }

    @Override
    public void init(BatteryPack batteryPack, List<BatteryMonomerBatVo> childDev) {
        if (CollectionUtils.isEmpty(childDev)) {
            return;
        }

        for (BatteryMonomerBatVo item : childDev) {
            if (item.getResistance() == null || item.getResistance() <= 0) {
                throw new RuntimeException("单体【" + item.getBatNum() + "】内阻无效");
            }
        }

        deleteByPackId(batteryPack.getPackId());
        List<DevBatteryMonomer> devBatteryMonomers = new ArrayList<>();
        childDev.forEach(item -> {
            DevBatteryMonomer devBatteryMonomer = new DevBatteryMonomer();
            devBatteryMonomer.setPackId(batteryPack.getPackId());
            devBatteryMonomer.setBatNum(item.getBatNum());
            devBatteryMonomer.setResistance(item.getResistance().intValue());
            devBatteryMonomers.add(devBatteryMonomer);
        });
        if (CollectionUtils.isEmpty(devBatteryMonomers)) {
            return;
        }
        devBatteryMonomerMapper.insertList(devBatteryMonomers);
    }
}

package com.shanhe.project.energy.stat.service.impl;

import com.shanhe.project.device.config.domain.BatteryMonitor;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.energy.stat.domain.DevBatteryMonomer;
import com.shanhe.project.energy.stat.mapper.DevBatteryMonomerMapper;
import com.shanhe.project.energy.stat.service.IDevBatteryMonomerService;
import com.shanhe.project.sync.domain.BatteryMonomerBatVo;
import com.shanhe.project.sync.service.ClientReportService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zhoubin
 * @date 2025/8/14
 */
@Service
public class DevBatteryMonomerServiceImpl implements IDevBatteryMonomerService {
    @Resource
    private DevBatteryMonomerMapper devBatteryMonomerMapper;
    @Resource
    private IBatteryPackService batteryPackService;
    @Resource
    private BatteryReportLogService batteryReportLogService;
    @Resource
    private ClientReportService clientReportService;

    @Override
    public List<DevBatteryMonomer> selectList(Long configId, Integer packNum) {
        BatteryPack batteryPack = batteryPackService.selectBatteryInfoByPackNum(configId, packNum);
        if (batteryPack == null) {
            return new ArrayList<>();
        }
        return devBatteryMonomerMapper.selectList(batteryPack.getPackId());
    }

    @Override
    public void init(Long configId, Integer packNum) {
        BatteryPack batteryPack = batteryPackService.selectBatteryInfoByPackNum(configId, packNum);

        // 获取单体数据
        if (batteryPack == null) {
            throw new RuntimeException("请先配置电池组信息");
        }

        BatteryReportLog batteryReportLog = batteryReportLogService.lastCache(configId, packNum);
        if (batteryReportLog == null) {
            throw new RuntimeException("暂无无上报数据");
        }
        List<BatteryMonitor> batteryList = batteryReportLog.getBatteryList();
        if (CollectionUtils.isEmpty(batteryList)) {
            throw new RuntimeException("暂无无上报单体数据");
        }

        // 合并过滤和校验逻辑，减少遍历次数
        for (BatteryMonitor item : batteryList) {
            if (item.getResistance() == null || item.getResistance() <= 0) {
                // 若存在无效项，直接返回
                throw new RuntimeException("单体【" + item.getBatNum() + "】内阻无效");
            }
        }

        // 删除旧数据
        deleteByPackId(batteryPack.getPackId());

        // 转换初始值
        List<DevBatteryMonomer> devBatteryMonomers = batteryList.stream()
                .map(item -> new DevBatteryMonomer(batteryPack.getPackId(), item.getBatNum(), item.getResistance()))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(devBatteryMonomers)) {
            return;
        }
        devBatteryMonomerMapper.insertList(devBatteryMonomers);

        clientReportService.uploadBatteryMonomer(configId, packNum, devBatteryMonomers, null);
    }

    @Override
    public void delete() {
        devBatteryMonomerMapper.delete();
    }

    @Override
    public Double getMaxResistance(Long configId, Integer packNum) {
        // 查询所有单体数据
        BatteryReportLog packInfo = batteryReportLogService.lastCache(configId, packNum);
        if (packInfo == null || null == packInfo.getBatteryList()) {
            return 0.0;
        }

        List<DevBatteryMonomer> devBatteryMonomers = selectList(configId, packNum);
        if (devBatteryMonomers == null || devBatteryMonomers.isEmpty()) {
            return 0.0;
        }
        Map<Integer, Integer> monomerMap = devBatteryMonomers.stream().collect(Collectors.toMap(DevBatteryMonomer::getBatNum, DevBatteryMonomer::getResistance, (v1, v2) -> v2));
        // 内阻变化率= ( (实测内阻 - 初始内阻) / 初始内阻 ) × 100%
        //  失效/危险：增加超过30%，，电池已超限，必须立即更换。
        //  实测内阻最小不能超过初始内阻，实测内阻最高只能为初始内阻的1倍

        Double max = null;
        for (BatteryMonitor batteryMonitor : packInfo.getBatteryList()) {
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
            // 使用浮点数运算确保精度，并乘以100得到百分比
            double percentage = (double) (batteryMonitor.getResistance() - monomerResistance) / monomerResistance;
            if (percentage > 1) {
                max = 1.0;
                break;
            }
            if (null == max || percentage > max) {
                max = percentage;
            }
        }

        return max == null ? 0.0 : max;
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

        // 合并过滤和校验逻辑，减少遍历次数
        for (BatteryMonomerBatVo item : childDev) {
            if (item.getResistance() == null || item.getResistance() <= 0) {
                // 若存在无效项，直接返回
                throw new RuntimeException("单体【" + item.getBatNum() + "】内阻无效");
            }
        }

        // 删除旧数据
        deleteByPackId(batteryPack.getPackId());
        List<DevBatteryMonomer> devBatteryMonomers =
                new ArrayList<>();
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

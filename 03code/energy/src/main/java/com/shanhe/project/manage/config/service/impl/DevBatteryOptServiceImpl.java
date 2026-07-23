package com.shanhe.project.manage.config.service.impl;

import com.shanhe.common.constant.Constants;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.project.manage.config.domain.DevBatteryOpt;
import com.shanhe.project.manage.config.mapper.DevBatteryOptMapper;
import com.shanhe.project.manage.config.service.IDevBatteryOptService;
import com.shanhe.project.sync.service.ClientReportService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 【蓄电池测试操作参数】Service业务层处理
 *
 * @author wjh
 * @since 2025/5/15
 */
@Service
public class DevBatteryOptServiceImpl implements IDevBatteryOptService {
    /** 蓄电池测试操作映射。 */
    @Resource
    private DevBatteryOptMapper devBatteryOptMapper;
    /** 客户端上报服务。 */
    @Resource
    private ClientReportService clientReportService;

    @Override
    public DevBatteryOpt selectDevBatteryOptByPackNum(Integer packNum, Integer testType) {
        DevBatteryOpt tmp = new DevBatteryOpt();
        tmp.setPackNum(packNum);
        tmp.setConfigId(Constants.DEFAULT_CONFIG_ID);
        tmp.setTestType(testType);
        List<DevBatteryOpt> list = this.selectDevBatteryOptList(tmp);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return tmp;
    }

    @Override
    public List<DevBatteryOpt> selectDevBatteryOptList(DevBatteryOpt devBatteryOpt) {
        return devBatteryOptMapper.selectDevBatteryOptList(devBatteryOpt);
    }

    @Override
    public void insertDevBatteryOpt(DevBatteryOpt devBatteryOpt) {
        if (devBatteryOpt == null) {
            return;
        }
        normalizeSaveDefaults(devBatteryOpt);
        if (devBatteryOpt.getOptId() == null) {
            DevBatteryOpt bt = this.selectDevBatteryOptByPackNum(devBatteryOpt.getPackNum(), devBatteryOpt.getTestType());
            if (bt == null || bt.getOptId() == null) {
                devBatteryOpt.setOptId(IdUtils.getSnowflakeId());
                devBatteryOptMapper.insertDevBatteryOpt(devBatteryOpt);
            } else {
                devBatteryOpt.setOptId(bt.getOptId());
                this.updateDevBatteryOpt(devBatteryOpt);
            }
        } else {
            this.updateDevBatteryOpt(devBatteryOpt);
        }

        // 是否上报
        if (!devBatteryOpt.getIsSync()) {
            clientReportService.uploadBatteryOpt(devBatteryOpt);
        }
    }

    @Override
    public void updateDevBatteryOpt(DevBatteryOpt devBatteryOpt) {
        devBatteryOptMapper.updateDevBatteryOpt(devBatteryOpt);
    }

    @Override
    public void deleteByPackNum(Integer packNum) {
        devBatteryOptMapper.deleteByPackNum(packNum);
    }

    /** 统一测试计划保存默认值，页面和同步入口只负责传入业务参数。 */
    private void normalizeSaveDefaults(DevBatteryOpt devBatteryOpt) {
        if (devBatteryOpt.getConfigId() == null) {
            devBatteryOpt.setConfigId(Constants.DEFAULT_CONFIG_ID);
        }
        if (devBatteryOpt.getExecCount() == null) {
            devBatteryOpt.setExecCount(0);
        }
        if (devBatteryOpt.getIsSync() == null) {
            devBatteryOpt.setIsSync(false);
        }
    }
}

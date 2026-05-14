package com.shanhe.project.device.config.service.impl;

import com.shanhe.common.constant.Constants;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.project.device.config.domain.DevBatteryOpt;
import com.shanhe.project.device.config.mapper.DevBatteryOptMapper;
import com.shanhe.project.device.config.service.IDevBatteryOptService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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
    @Resource
    private DevBatteryOptMapper devBatteryOptMapper;

    @Override
    public DevBatteryOpt selectDevBatteryOptByOptId(Long optId) {
        return devBatteryOptMapper.selectDevBatteryOptByOptId(optId);
    }

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
    }

    @Override
    public int insertDevBatteryOptList(List<DevBatteryOpt> devBatteryOpts) {
        if (CollectionUtils.isEmpty(devBatteryOpts)) {
            return 1;
        }
        devBatteryOpts.forEach(devBatteryOpt -> devBatteryOpt.setOptId(IdUtils.getSnowflakeId()));
        return devBatteryOptMapper.insertDevBatteryOptList(devBatteryOpts);
    }

    @Override
    public void updateDevBatteryOpt(DevBatteryOpt devBatteryOpt) {
        devBatteryOptMapper.updateDevBatteryOpt(devBatteryOpt);
    }

    @Override
    public int deleteDevBatteryOptByOptIds(List<Long> optIds) {
        return devBatteryOptMapper.deleteDevBatteryOptByOptIds(optIds);
    }

    @Override
    public int deleteDevBatteryOptByOptId(Long optId) {
        return devBatteryOptMapper.deleteDevBatteryOptByOptId(optId);
    }

    @Override
    public void deleteByConfigId(Integer packNum) {
        devBatteryOptMapper.deleteByConfigId(Constants.DEFAULT_CONFIG_ID, packNum);
    }

}

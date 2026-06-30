package com.shanhe.project.manage.config.service.impl;

import com.shanhe.common.constant.Constants;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.project.manage.config.domain.DevBatteryOpt;
import com.shanhe.project.manage.config.mapper.DevBatteryOptMapper;
import com.shanhe.project.manage.config.service.IDevBatteryOptService;
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
    /** 蓄电池测试操作映射。 */
    @Resource
    private DevBatteryOptMapper devBatteryOptMapper;

    /**
     * 根据操作ID查询蓄电池测试操作参数
     *
     * @param optId 操作主键
     * @return 操作参数
     */
    @Override
    public DevBatteryOpt selectDevBatteryOptByOptId(Long optId) {
        return devBatteryOptMapper.selectDevBatteryOptByOptId(optId);
    }

    /**
     * 根据电池组编号和测试类型查询操作参数
     *
     * @param packNum 电池组编号
     * @param testType 测试类型
     * @return 操作参数
     */
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

    /**
     * 查询操作参数列表
     *
     * @param devBatteryOpt 查询条件
     * @return 操作参数列表
     */
    @Override
    public List<DevBatteryOpt> selectDevBatteryOptList(DevBatteryOpt devBatteryOpt) {
        return devBatteryOptMapper.selectDevBatteryOptList(devBatteryOpt);
    }

    /**
     * 新增或更新操作参数
     *
     * @param devBatteryOpt 操作参数
     */
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
    }

    /**
     * 批量新增操作参数
     *
     * @param devBatteryOpts 操作参数列表
     * @return 结果
     */
    @Override
    public int insertDevBatteryOptList(List<DevBatteryOpt> devBatteryOpts) {
        if (CollectionUtils.isEmpty(devBatteryOpts)) {
            return 1;
        }
        devBatteryOpts.forEach(devBatteryOpt -> devBatteryOpt.setOptId(IdUtils.getSnowflakeId()));
        return devBatteryOptMapper.insertDevBatteryOptList(devBatteryOpts);
    }

    /**
     * 更新操作参数
     *
     * @param devBatteryOpt 操作参数
     */
    @Override
    public void updateDevBatteryOpt(DevBatteryOpt devBatteryOpt) {
        devBatteryOptMapper.updateDevBatteryOpt(devBatteryOpt);
    }

    /**
     * 根据操作ID批量删除
     *
     * @param optIds 操作ID列表
     * @return 结果
     */
    @Override
    public int deleteDevBatteryOptByOptIds(List<Long> optIds) {
        return devBatteryOptMapper.deleteDevBatteryOptByOptIds(optIds);
    }

    /**
     * 根据操作ID删除
     *
     * @param optId 操作主键
     * @return 结果
     */
    @Override
    public int deleteDevBatteryOptByOptId(Long optId) {
        return devBatteryOptMapper.deleteDevBatteryOptByOptId(optId);
    }

    /**
     * 删除指定电池组的操作参数
     *
     * @param packNum 电池组编号
     */
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

package com.shanhe.project.manage.config.service;

import com.shanhe.project.manage.config.domain.DevBatteryOpt;

import java.util.List;

/**
 * 【蓄电池测试操作参数】Service接口
 *
 * @author wjh
 * @since 2025/5/15
 */
public interface IDevBatteryOptService {
    /**
     * 查询【蓄电池测试操作参数】
     *
     * @param optId 【蓄电池测试操作参数】主键
     * @return 【蓄电池测试操作参数】
     */
    DevBatteryOpt selectDevBatteryOptByOptId(Long optId);

    /**
     * 查询【蓄电池测试操作参数】
     *
     * @param packNum 蓄电池组编号
     * @param testType 测试类型1内阻测试2连接条测试，3容量测试，4浮充测试，5备电时长测试
     * @return 【蓄电池测试操作参数】
     */
    DevBatteryOpt selectDevBatteryOptByPackNum(Integer packNum, Integer testType);

    /**
     * 查询【蓄电池测试操作参数】列表
     *
     * @param devBatteryOpt 【蓄电池测试操作参数】
     * @return 【蓄电池测试操作参数】集合
     */
    List<DevBatteryOpt> selectDevBatteryOptList(DevBatteryOpt devBatteryOpt);

    /**
     * 新增【蓄电池测试操作参数】
     *
     * @param devBatteryOpt 【蓄电池测试操作参数】
     */
    void insertDevBatteryOpt(DevBatteryOpt devBatteryOpt);

    /**
     * 批量插入蓄电池测试参数
     *
     * @param devBatteryOpts 测试参数列表
     * @return 结果
     */
    int insertDevBatteryOptList(List<DevBatteryOpt> devBatteryOpts);

    /**
     * 修改【蓄电池测试操作参数】
     *
     * @param devBatteryOpt 【蓄电池测试操作参数】
     */
    void updateDevBatteryOpt(DevBatteryOpt devBatteryOpt);

    /**
     * 批量删除【蓄电池测试操作参数】
     *
     * @param optIds 需要删除的【蓄电池测试操作参数】主键集合
     * @return 结果
     */
    int deleteDevBatteryOptByOptIds(List<Long> optIds);

    /**
     * 删除【蓄电池测试操作参数】信息
     *
     * @param optId 【蓄电池测试操作参数】主键
     * @return 结果
     */
    int deleteDevBatteryOptByOptId(Long optId);

    /**
     * 删除【蓄电池测试操作参数】信息
     *
     * @param packNum 蓄电池组编号
     */
    void deleteByPackNum(Integer packNum);
}

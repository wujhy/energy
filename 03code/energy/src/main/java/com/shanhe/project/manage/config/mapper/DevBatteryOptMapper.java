package com.shanhe.project.manage.config.mapper;

import com.shanhe.project.manage.config.domain.DevBatteryOpt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 蓄电池测试操作参数
 *
 * @author wjh
 * @since 2025/5/15
 */
@Mapper
public interface DevBatteryOptMapper {
    /**
     * 查询【蓄电池测试操作参数】
     *
     * @param optId 【蓄电池测试操作参数】主键
     * @return 【蓄电池测试操作参数】
     */
     DevBatteryOpt selectDevBatteryOptByOptId(Long optId);

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
     * @return 结果
     */
    int insertDevBatteryOpt(DevBatteryOpt devBatteryOpt);

    /**
     * 批量插入蓄电池测试参数
     *
     * @param devBatteryOpt 测试参数列表
     * @return 结果
     */
    int insertDevBatteryOptList(@Param("list")List<DevBatteryOpt> devBatteryOpt);

    /**
     * 修改【蓄电池测试操作参数】
     *
     * @param devBatteryOpt 【蓄电池测试操作参数】
     * @return 结果
     */
    int updateDevBatteryOpt(DevBatteryOpt devBatteryOpt);

    /**
     * 删除【蓄电池测试操作参数】
     *
     * @param optId 【蓄电池测试操作参数】主键
     * @return 结果
     */
    int deleteDevBatteryOptByOptId(Long optId);

    /**
     * 批量删除【蓄电池测试操作参数】
     *
     * @param optIds 需要删除的数据主键集合
     * @return 结果
     */
    int deleteDevBatteryOptByOptIds(@Param("optIds") List<Long> optIds);

    /**
     * 根据电池组编号删除
     * @param packNum 电池组编号
     */
    void deleteByPackNum(@Param("packNum") Integer packNum);
}

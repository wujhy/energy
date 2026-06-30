package com.shanhe.project.manage.opt.service;

import com.shanhe.project.manage.opt.vo.BatterySetVO;

/**
 * 复位服务
 *
 * @author wjh
 * @since 2025/10/14
 */
public interface RestoreService {
    /**
     * 复位
     *
     * @param batterySetVO 电池设置参数
     */
    void restore(BatterySetVO batterySetVO);

    /**
     * 清除组数据
     *
     * @param batterySetVO 电池设置参数
     */
    void delPack(BatterySetVO batterySetVO);

}

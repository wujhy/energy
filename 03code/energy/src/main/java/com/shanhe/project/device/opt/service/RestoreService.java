package com.shanhe.project.device.opt.service;

import com.shanhe.project.device.opt.vo.BatterySetVO;

/**
 * @author zhoubin
 * @date 2025/10/14
 */
public interface RestoreService {
    /**
     * 复位
     */
    void restore(BatterySetVO batterySetVO);

    /**
     * 清除组数据
     */
    void delPack(BatterySetVO batterySetVO);

}

package com.shanhe.project.energy.capacity.service;


import com.shanhe.project.energy.capacity.vo.DataPoint;
import com.shanhe.project.energy.stat.domain.StatBatteryBat;

import java.util.Date;
import java.util.List;

/**
 * 数据采集点
 *
 * @author xuxw
 */
public interface DataPointService {
    /**
     * 查询蓄电池放电数据的数据
     *
     * @param configId     蓄电池ID
     * @param packNum   蓄电池编号
     * @param batNum    单体编号
     * @param startTime 开始时间
     * @param endTime   结束时间
     */
    List<DataPoint> findCurrentDataPoint(Long configId, Integer packNum, Integer batNum, Date startTime, Date endTime);

    /**
     * 统计设备的平均电流
     */
    Double getAvgCurrent(Long configId, Integer packNum, Date startTime, Date endTime);

}

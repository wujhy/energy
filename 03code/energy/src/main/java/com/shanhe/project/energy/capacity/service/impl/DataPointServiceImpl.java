package com.shanhe.project.energy.capacity.service.impl;

import com.shanhe.project.energy.capacity.service.DataPointService;
import com.shanhe.project.energy.capacity.vo.DataPoint;
import com.shanhe.common.constant.Constants;
import com.shanhe.project.energy.stat.domain.StatBatteryBat;
import com.shanhe.project.energy.stat.mapper.StatBatteryBatMapper;
import com.shanhe.project.energy.stat.mapper.StatBatteryPackMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 */
@Service
public class DataPointServiceImpl implements DataPointService {

    @Resource
    private StatBatteryBatMapper statBatteryBatMapper;

    @Resource
    private StatBatteryPackMapper statBatteryPackMapper;

    @Override
    public List<DataPoint> findCurrentDataPoint(Integer packNum, Integer batNum, Date startTime, Date endTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String startDateTimeStr = startTime != null ? sdf.format(startTime) : null;
        String endDateTimeStr = endTime != null ? sdf.format(endTime) : null;

        List<DataPoint> list = statBatteryBatMapper.selectDataPointList(Constants.DEFAULT_CONFIG_ID, packNum, batNum, startDateTimeStr, endDateTimeStr);
        //转换数据
        if (list != null && list.size() > 2) {
            //移除第一个放电数据，放电时，电压会快速下跌，需做优化处理
            list.remove(0);
        }
        return list;
    }

    @Override
    public Double getAvgCurrent(Integer packNum, Date startTime, Date endTime) {
        StatBatteryBat param = new StatBatteryBat();
        param.setConfigId(Constants.DEFAULT_CONFIG_ID);
        param.setPackNum(packNum);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Map<String, Object> params = param.getParams();
        params.put("beginTime", sdf.format(startTime));
        params.put("endTime", sdf.format(endTime));
        return statBatteryPackMapper.getAvgCurrent(param);
    }
}

package com.shanhe.project.iot.CM03N;

import cn.hutool.core.util.StrUtil;
import com.shanhe.framework.enums.BatteryCidEnum;
import com.shanhe.framework.comm.tcp.model.DeviceData;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.iot.battery.BatteryAlarmHandler;
import com.shanhe.project.iot.battery.BatteryOptResHandler;
import com.shanhe.project.iot.battery.BatteryPackHandler;
import com.shanhe.project.iot.battery.BatteryParamsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 蓄电池总处理类
 *
 * @author wjh
 * @since 2025/4/9
 */
@Service
public class BatteryHandler {
    protected static Logger logger = LoggerFactory.getLogger(BatteryHandler.class);

    @Resource
    private BatteryParamsHandler batteryParamsHandler;
    @Resource
    private BatteryOptResHandler batteryOptResHandler;
    @Resource
    private BatteryAlarmHandler batteryAlarmHandler;
    @Resource
    private BatteryPackHandler batteryPackHandler;

    /**
     * 上传蓄电池数据
     *
     * @param config 设备信息
     * @param deviceData 接收数据信息
     */
    public void doUploadData(Config config, DeviceData deviceData) {
        // 上报电池内容不足
        if (StrUtil.isBlank(deviceData.getInfo()) || deviceData.getInfo().length() < 14) {
            return;
        }


        // 电池私有指令
        String cmdType = deviceData.getInfo().substring(12, 14);
        BatteryCidEnum batteryCidEnum = BatteryCidEnum.find(cmdType);
        // 响应内容
        logger.info("{}：{} => {}", cmdType, batteryCidEnum.getDictLabel(), deviceData.getInfo());
        switch (batteryCidEnum){
            case _82:
                //蓄电池实时数据
                batteryPackHandler.uploadBatteryPackData(config, deviceData);
                break;
            case _83:
                //屏蔽电池组告警参数
                batteryParamsHandler.signParamsResponse(deviceData);
                break;
            case _86:
                //读取蓄电池电池组单体数量以及规格
                batteryPackHandler.uploadBatterPack(config, deviceData);
                break;
            case _87:
                //上传电池组报警状态
                batteryAlarmHandler.uploadBatteryWarnData(config, deviceData);
                break;
            case _8A:
                //响应单个告警参数屏蔽
                batteryParamsHandler.shieldAlarmReply(deviceData);
                break;
            case _8B:
                //上传电池组参数
                batteryParamsHandler.uploadBatteryParamsData(config, deviceData);
                break;
            case _8D:
                //上传设备故障类告警状态
                batteryAlarmHandler.deviceFaultAlarmUpload(config, deviceData);
                break;
            case _8E:
                //上传设备型号及软件版本号
                batteryOptResHandler.uploadBatterySoftNum(config, deviceData);
                break;
            case _85:
                batteryOptResHandler.batteryResponse85(config, deviceData);
                break;
            case _2A:
            case _88:
            case _89:
            case _99:
            case _A8:
            case _8F:
            case _E0:
            case _E1:
            case _E2:
            case _E3:
            case _E4:
            case _E5:
            case _E6:
            case _E7:
            case _E8:
            case _F8:
            case _F6: // 恢复出厂设置响应
                // 普通应答，统一缓存结果
                batteryOptResHandler.uploadBatteryResponse(config, deviceData);
                break;
            case _F9:
                // 设置指令，统一缓存结果
                batteryOptResHandler.setBatteryResponse(config, deviceData);
                break;
            case _E9:
                // 读取屏蔽报警参数响应
                batteryParamsHandler.alarmMsgSave(config, deviceData);
                break;
            case _EB:
                // 电池组工作模式
                batteryOptResHandler.getModeStatus(config, deviceData);
                break;
            case _F5:
                // 恢复出厂设置响应
                break;
            default:
                break;
        }
    }
}

package com.shanhe.project.iot.CM03N;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.LinearCalculator;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.DataTypeEnum;
import com.shanhe.framework.enums.DeviceTypeEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.framework.comm.tcp.model.DeviceData;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.domain.ConfigAttribute;
import com.shanhe.project.device.config.service.IConfigAttributeService;
import com.shanhe.project.device.config.service.IConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 开关量/模拟量数据数据
 *
 * @author wjh
 * @since 2025/6/6
 */
@Service
public class DataSwitchHandler {

    protected static Logger logger = LoggerFactory.getLogger(DataSwitchHandler.class);

    @Resource
    private IConfigService configService;
    @Resource
    private IConfigAttributeService configAttributeService;
    @Resource
    private IAlarmLogService alarmLogService;

    /** 开关量属性名 */
    String switchingCode = "status";

    /** 模拟量属性名 */
    String analogCode = "isOpen";

    /**
     * 响应读取模拟量
     * 1、应答结果(0/1表示正常/异常）  （1字节）
     * 2、模拟量类型                 （1字节）
     *    1：输入模拟量
     *    2：输出模拟量
     * 3、模拟量端口号（1...n）           (1字节）
     * 3、模拟量类型                     (1字节）
     * 4、端口号模拟量值(1....n)          (2字节）
     */
    public void cmdD5(DeviceData deviceData) {
        // 应答结果
        int resResult = CodingUtil.hexParseInt(deviceData.getInfo().substring(0, 2));
        if (Objects.equals(1, resResult)) {
            logger.error("D5：响应读取模拟量异常 => {}", deviceData.getInfo());
            return;
        }
        // 模拟量类型，输出模拟量不处理
        int dataType = CodingUtil.hexParseInt(deviceData.getInfo().substring(2, 4));
        if (Objects.equals(2, dataType)) {
            return;
        }

        // 模拟量数量（模拟量数量*4+2）
        int num = (CodingUtil.hexStringToInteger(deviceData.getLength()) - 2) / 4;

        // 数据内容
        String data = deviceData.getInfo().substring(4);
        int offset;
        for (int i = 0; i < num; i++) {
            offset = i * 8;
            Integer port = CodingUtil.hexParseInt(data.substring(offset, offset + 2));
            Integer type = CodingUtil.hexParseInt(data.substring(offset + 2, offset + 4));
            int portValue = CodingUtil.hexParseInt(data.substring(offset + 4, offset + 8));
            String value = CodingUtil.decimal(portValue, "#.0", 10);

            // 获取设备
            Config config = configService.getCacheBy(type, port, 1);
            if (config == null || !Objects.equals(config.getStatus(), YesNoEnum.YES.getDictValue())) {
                logger.debug("D5：响应读取模拟量，未找到端口信息，devId：{}，type:{}，port：{}", deviceData.getImei(), type, port);
                continue;
            }
            this.setValue(config, analogCode, value);
        }
    }

    /**
     * 响应设置输出模拟量
     */
    public void cmdD6(DeviceData deviceData) {
        if (StrUtil.isBlank(deviceData.getImei()) || StrUtil.isBlank(deviceData.getC3()) || StrUtil.isBlank(deviceData.getInfo())) {
            return;
        }
        try {
            String key = String.format(CacheKeyEnum.RESULT_CX.getKey(), deviceData.getC0(), deviceData.getC1(), deviceData.getC2(), deviceData.getC3());
            int result = CodingUtil.hexParseInt(deviceData.getInfo().substring(0, 2));
            CacheUtils.put(CacheKeyEnum.RESULT_CX.getCache(), key, result == 0 ? 0 : 1);
            if (result != 0) {
                logger.error("响应设置输出模拟量异常 imei：{} 返回的结果：{}", key, result);
            }
        } catch (Exception e) {
            logger.error("响应设置输出模拟量异常 imei：{} 返回结果异常：{}", deviceData.getImei(), deviceData.getInfo());
        }
    }

    /**
     * 响应读取开关量
     * INFO：
     * 1、应答结果(0/1表示正常/异常）  （1字节）
     * 2、开关量类型                  (1字节）
     *    1：输入开关量
     *    2：输出开关量
     * 3、开关量编号(1....n)          (1字节）
     * 4、开关量类别                  (1字节）
     * 5、开关量状态(1....n)          (1字节）
     *    0：低电平或常开
     *    1：高电平或常闭
     */
    public void cmdD7(DeviceData deviceData) {
        // 响应结果
        int resResult = CodingUtil.hexParseInt(deviceData.getInfo().substring(0, 2));
        if (Objects.equals(1, resResult)) {
            logger.error("D7：响应读取开关量 => {}", deviceData.getInfo());
            return;
        }
        // 开关量类型，输出开关量不处理
        int dataType = CodingUtil.hexParseInt(deviceData.getInfo().substring(2, 4));
        if (Objects.equals(2, dataType)) {
            return;
        }

        // 开关量数量（开关量数量*3+2）
        int num = (CodingUtil.hexStringToInteger(deviceData.getLength()) - 2) / 3;

        // 开关量数据内容
        String data = deviceData.getInfo().substring(4);
        int offset;
        for (int i = 0; i < num; i++) {
            offset = i * 6;
            Integer port = CodingUtil.hexParseInt(data.substring(offset, offset + 2));
            Integer type = CodingUtil.hexParseInt(data.substring(offset + 2, offset + 4));
            int value = CodingUtil.hexParseInt(data.substring(offset + 4, offset + 6));

            // 获取设备
            Config config = configService.getCacheBy(type, port, 1);
            if (config == null || !Objects.equals(config.getStatus(), YesNoEnum.YES.getDictValue())) {
                logger.debug("D7：响应读取开关量，未找到端口信息，devId：{}，type:{}，port：{}", deviceData.getImei(), type, port);
                continue;
            }
            // 空调设备
            if (Objects.equals(config.getType(), DeviceTypeEnum._4.getDictValue())) {
                continue;
            }
            this.setValue(config, switchingCode, Integer.toString(value));
        }
    }

    /**
     * 响应设置输出开关量
     */
    public void cmdD8(DeviceData deviceData) {
        if (StrUtil.isBlank(deviceData.getImei()) || StrUtil.isBlank(deviceData.getC3()) || StrUtil.isBlank(deviceData.getInfo())) {
            return;
        }
        try {
            String key = String.format(CacheKeyEnum.RESULT_CX.getKey(), deviceData.getC0(), deviceData.getC1(), deviceData.getC2(), deviceData.getC3());
            int resResult = CodingUtil.hexParseInt(deviceData.getInfo().substring(0, 2));
            if (Objects.equals(1, resResult)) {
                logger.error("响应设置输出开关量 imei：{} 返回的结果：{}", key, resResult);
            }
            CacheUtils.put(CacheKeyEnum.RESULT_CX.getCache(), key, resResult == 0 ? 0 : 1);
        } catch (Exception e) {
            logger.error("响应设置输出开关量 imei：{} 返回结果异常：{}", deviceData.getImei(), deviceData.getInfo());
        }
    }

    /**
     * 设置开关量告警处理
     *
     * @param config 设备
     * @param itemCode 字段名
     * @param value 值
     */
    private void setValue(Config config, String itemCode, String value) {
        // 保存设备在线时间
        CacheUtils.put(String.format(CacheKeyEnum.CONFIG_ONLINE.getKey(), config.getType(), config.getPort(), config.getChannel()), new Date());
        // 字段
        ConfigAttribute attribute = configAttributeService.getCacheBy(config.getConfigId(), itemCode);
        if (attribute == null) {
            logger.error("deviceName={}的属性={}无效", config.getName(), itemCode);
            return;
        }

        // 模拟量、线性计算
        if (Objects.equals(attribute.getType(), DataTypeEnum._2.getDictValue())
                && Objects.equals(attribute.getIsLinear(), YesNoEnum.YES.getDictValue())) {
            if (attribute.getSpb() != null && attribute.getSpb() > 0
                    && attribute.getSpk() != null && attribute.getSpk() > 0) {
                value = LinearCalculator.calculate(attribute.getSpk(), attribute.getSpb(), value);
            }
        }

        // 校验是否告警处理
        alarmLogService.alarmValid(attribute, null, value, config.getType());
    }
}

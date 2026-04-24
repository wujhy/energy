package com.shanhe.project.device.opt.service;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.enums.BatteryCidEnum;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.TcpCharEnum;
import com.shanhe.framework.enums.TcpCidEnum;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.opt.cmd.DeviceModel;
import com.shanhe.project.device.opt.vo.BatterySetVO;
import com.shanhe.project.iot.model.BatteryModeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import oshi.util.Util;

import java.util.Calendar;

/**
 * 开关量控制类
 *
 * @author wjh
 * @since 2025/7/10
 */
@Service
public class ControlBatterySet extends ControlBase {

    protected static Logger logger = LoggerFactory.getLogger(ControlBatterySet.class);
    /** 缓存结果 **/
    CacheKeyEnum cacheKeyEnum = CacheKeyEnum.RESULT;

    /**
     * 设置电池组告警参数
     */
    public AjaxResult doSet(BatterySetVO batterySetVO, BatteryCidEnum cidEnum) {
        // 主机信息
        Host host = super.getHost();
        // 设备信息
        Config config = super.getConfig(batterySetVO.getConfigId());
        // 协议内容
        StringBuilder info = new StringBuilder();
        // 指令头、默认地址、指令编码
        info.append(TcpCharEnum.HEAD_53.getDictValue());
        info.append("01").append(cidEnum.getDictValue());
        // 响应动态指令
        String dynCid;
        switch (cidEnum) {
            case _03:
                // 设置电池组告警参数
                dynCid = BatteryCidEnum._83.getDictValue();
                // 长度
                info.append("05");
                // 包序号
                info.append(CodingUtil.integerToHexString(batterySetVO.getPackNum(), 2));
                // 异常等级
                info.append(CodingUtil.integerToHexString(batterySetVO.getLevel(), 2));
                // 参数号、参数值
                info.append(batterySetVO.getParamNum());
                info.append(batterySetVO.getParamValue());
                break;
            case _05:
                batterySetVO.setNeedDynResult(false);
                // 设置系统状态响应
                dynCid = BatteryCidEnum._85.getDictValue();
                // 长度
                info.append("02");
                // 参数号、参数值
                info.append(batterySetVO.getParamNum());
                info.append(batterySetVO.getParamValue());
                break;
            case _08:
                // 手动设置模块编号
                dynCid = BatteryCidEnum._88.getDictValue();
                // 长度
                info.append("03");
                // 包序号、单体编号、新编号
                info.append(CodingUtil.integerToHexString(batterySetVO.getPackNum(), 2));
                info.append(CodingUtil.integerToHexString(batterySetVO.getModelNum(), 2));
                info.append(CodingUtil.integerToHexString(batterySetVO.getNewModelNum(), 2));
                break;
            case _09:
                batterySetVO.setNeedDynResult(false);
                // 配置电池组
                dynCid = BatteryCidEnum._89.getDictValue();
                // 长度
                info.append("05");
                // 包序号、电池规格、电池节数、电池容量
                info.append(CodingUtil.integerToHexString(batterySetVO.getPackNum(), 2));
                info.append(CodingUtil.integerToHexString(batterySetVO.getBatSinModel(), 2));
                info.append(CodingUtil.integerToHexString(batterySetVO.getBatSinSize(), 2));
                info.append(CodingUtil.integerToHexString(batterySetVO.getBatCapacity().intValue(), 4));
                break;
            case _0E:
                // 设备型号及软件版本号
                dynCid = BatteryCidEnum._8E.getDictValue();
                // 长度
                info.append("00");
                break;
            case _18:
                // 自动设置模块编号
                dynCid = BatteryCidEnum._A8.getDictValue();
                // 长度
                info.append("01");
                // 包序号
                info.append(CodingUtil.integerToHexString(batterySetVO.getPackNum(), 2));
                break;
            case _19:
                // 未设置单体编号，则为整组内阻系数设置，无需等待响应
                if (batterySetVO.getModelNum() == null || batterySetVO.getModelNum() == 0) {
                    batterySetVO.setNeedDynResult(false);
                    batterySetVO.setModelNum(0);
                }
                // 设置内阻系数
                dynCid = BatteryCidEnum._99.getDictValue();
                // 长度
                info.append("04");
                // 包序号、单体编号、内阻系数
                info.append(CodingUtil.integerToHexString(batterySetVO.getPackNum(), 2));
                info.append(CodingUtil.integerToHexString(batterySetVO.getModelNum(), 2));
                int resistance = (int) (batterySetVO.getResistance() * 1000);
                if (resistance > 65535) {
                    return AjaxResult.error("内阻系数过大！");
                }
                info.append(CodingUtil.integerToHexString(resistance, 4));
                break;
            case _20:
                // 清鼓包数据
                dynCid = BatteryCidEnum._2A.getDictValue();
                // 长度
                info.append("01");
                // 包序号
                info.append(CodingUtil.integerToHexString(batterySetVO.getPackNum(), 2));
                break;
            case _37:
                // 设置时间
                dynCid = BatteryCidEnum._E7.getDictValue();
                // 长度
                info.append("08").append("04");
                // 未指定时间，取服务器当前时间
                if (StrUtil.isBlank(batterySetVO.getDatetime())) {
                    Calendar calendar = Calendar.getInstance();
                    info.append(CodingUtil.integerToHexString(calendar.get(Calendar.YEAR), 4));
                    info.append(CodingUtil.integerToHexString(calendar.get(Calendar.MONTH) + 1, 2));
                    info.append(CodingUtil.integerToHexString(calendar.get(Calendar.DAY_OF_MONTH), 2));
                    info.append(CodingUtil.integerToHexString(calendar.get(Calendar.HOUR_OF_DAY), 2));
                    info.append(CodingUtil.integerToHexString(calendar.get(Calendar.MINUTE), 2));
                    info.append(CodingUtil.integerToHexString(calendar.get(Calendar.SECOND), 2));
                } else {
                    info.append(CodingUtil.stringToHexString(batterySetVO.getDatetime().substring(0, 4), 4));
                    info.append(CodingUtil.stringToHexString(batterySetVO.getDatetime().substring(4, 6), 2));
                    info.append(CodingUtil.stringToHexString(batterySetVO.getDatetime().substring(6, 8), 2));
                    info.append(CodingUtil.stringToHexString(batterySetVO.getDatetime().substring(8, 10), 2));
                    info.append(CodingUtil.stringToHexString(batterySetVO.getDatetime().substring(10, 12), 2));
                    info.append(CodingUtil.stringToHexString(batterySetVO.getDatetime().substring(12, 14), 2));
                }
                break;
            case _38:
                // 均衡设置
                dynCid = BatteryCidEnum._E8.getDictValue();
                // 长度
                info.append("02");
                // 手动均衡、自动均衡
                info.append(CodingUtil.integerToHexString(batterySetVO.getManualBalanced(), 2));
                info.append(CodingUtil.integerToHexString(batterySetVO.getAutoBalanced(), 2));
                break;
            case _3B:
                // 电池组工作模式
                dynCid = BatteryCidEnum._EB.getDictValue();
                // 长度
                info.append("01");
                // 包序号
                info.append(CodingUtil.integerToHexString(batterySetVO.getPackNum(), 2));
                break;
            case _75:
                // 恢复出厂设置
                dynCid = BatteryCidEnum._F5.getDictValue();
                // 长度
                info.append("00");
                break;
            case _78:
                // 清组数据
                dynCid = BatteryCidEnum._F8.getDictValue();
                // 长度
                info.append("01");
                // 包序号
                info.append(CodingUtil.integerToHexString(batterySetVO.getPackNum(), 2));
                break;
            case _79:
                // 清主机数据
                dynCid = BatteryCidEnum._F9.getDictValue();
                // 长度
                info.append("01").append("01");
                break;
            case _76:
                // 清主机数据
                dynCid = BatteryCidEnum._F6.getDictValue();
                // 长度
                info.append("06");
                // 包序号
                info.append(CodingUtil.integerToHexString(batterySetVO.getPackNum(), 2));
                // 单体编号
                info.append(CodingUtil.integerToHexString(batterySetVO.getModelNum(), 2));
                // 数据类型
                info.append(CodingUtil.integerToHexString(batterySetVO.getDataType(), 2));
                // 数据高低
                info.append(CodingUtil.integerToHexString(batterySetVO.getDataStatus(), 2));
                // 数据值


                /**
                 * 负数处理
                 */
                // batterySetVO.getDataInfo() 负数处理
                if (batterySetVO.getDataInfo() < 0 ) {
                    batterySetVO.setDataInfo(batterySetVO.getDataInfo() + 65536);
                }

                info.append(CodingUtil.integerToHexString(batterySetVO.getDataInfo(), 4));
                break;
            default:
                return AjaxResult.error("指令异常！");
        }
        // 校验和
        info.append(CodingUtil.energyCheckSum(info.substring(TcpCharEnum.HEAD_53.getDictValue().length())));
        // 指令尾
        info.append(TcpCharEnum.END_0D.getDictValue());

        // 日志
        logger.debug("下发指令：{}", info);

        // 是否重复请求
        String resultKey = "";
        if (batterySetVO.getNeedDynResult()) {
            resultKey = super.setControlStatus(config, batterySetVO.getPackNum(), dynCid, cacheKeyEnum);
        }

        // 下发指令
        CommServer.returnCmd(DeviceModel.getCmd(host, config, info.toString(), TcpCidEnum._54.getDictValue(), dynCid));

        // 结果监控
        if (cidEnum == BatteryCidEnum._0E) {
            return getVersionResult(resultKey);
        }
//        else if (cidEnum == BatteryCidEnum._3B) {
//            return getModelResult(resultKey);
//        }
        else if (batterySetVO.getNeedDynResult()) {
            return super.getControlResult(resultKey, cacheKeyEnum);
        } else {
            return AjaxResult.success();
        }
    }

    /**
     * 监听控制指令执行结果
     *
     * @param resultKey 缓存key
     * @return 结果
     */
    public AjaxResult getVersionResult(String resultKey) {
        for (int i = 0; i < 20; i++) {
            Object result = CacheUtils.get(cacheKeyEnum.getCache(), resultKey);
            if (result == null) {
                // 超时
                break;
            } else if (result instanceof String) {
                CacheUtils.remove(cacheKeyEnum.getCache(), resultKey);
                return AjaxResult.success(result);
            }
            Util.sleep(500L);
        }

        return AjaxResult.success("");
    }

    /**
     * 监听控制指令执行结果
     *
     * @param resultKey 缓存key
     * @return 结果
     */
    public AjaxResult getModelResult(String resultKey) {
        while (true) {
            Object result = CacheUtils.get(cacheKeyEnum.getCache(), resultKey);
            if (result == null) {
                // 超时
                break;
            } else if (result instanceof BatteryModeInfo) {
                CacheUtils.remove(cacheKeyEnum.getCache(), resultKey);
                return AjaxResult.success(result);
            }
            Util.sleep(500L);
        }

        return AjaxResult.error("请求失败");
    }

    /**
     * 监听控制指令执行结果
     */
    public BatteryModeInfo getModelResult(BatterySetVO batterySetVO) {
        // 设备信息
        Object result = CacheUtils.get(cacheKeyEnum.getCache(),
                String.format(cacheKeyEnum.getKey(), batterySetVO.getConfigId(), null, BatteryCidEnum._EB.getDictValue()));

        if (result instanceof BatteryModeInfo) {
            return (BatteryModeInfo) result;
        }
        BatteryModeInfo batteryModeInfo = new BatteryModeInfo();
        batteryModeInfo.setPackNum(batterySetVO.getPackNum());
        batteryModeInfo.setMode(0);
        batteryModeInfo.setResult(0);
        batteryModeInfo.setStatus(0);
        return batteryModeInfo;
    }

    /**
     * 清除编号数据
     */
    public AjaxResult clearModelNum(Long configId, Integer packNum) {
        String key = String.format(cacheKeyEnum.getKey(), configId, null, BatteryCidEnum._EB.getDictValue());
        // 设备信息
        if (null == packNum) {
            CacheUtils.remove(cacheKeyEnum.getCache(), key);
            return AjaxResult.success();
        }
        Object result = CacheUtils.get(cacheKeyEnum.getCache(), key);
        if (null != result) {
            if (result instanceof BatteryModeInfo) {
                BatteryModeInfo batteryModeInfo = (BatteryModeInfo) result;
                if (ObjUtil.equals(packNum, batteryModeInfo.getPackNum())) {
                    CacheUtils.remove(cacheKeyEnum.getCache(), key);
                }
            }
        }

        return AjaxResult.success();
    }
}

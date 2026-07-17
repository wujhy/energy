package com.shanhe.project.manage.opt.domain;

import com.shanhe.framework.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 通用设备操作日志
 *
 * @author wjh
 * @since 2025/7/9
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OptLog extends BaseEntity {
    /** 主键 */
    private Long id;
    /** 设备ID */
    private Long configId;
    /** 组序号 */
    private Integer packNum;
    /** 操作类型 */
    private Integer type;
    /** 内容参数 */
    private String content;
    /** 内容参数 */
    private Map<String, Object> params;
    /** 操作结果 */
    private Integer result;
    /** 额定容量 单位 A */
    private Double batCapacity;

    /** 预估容量 单位 AH */
    private Double bcapacity;

    /** 放电容量 */
    private Double dischargeCapacity;

    /** 平均电流 单位 A */
    private Double current;

    /** 更新时间 */
    private String createTimeStr;

    /** 来源。 */
    private String source;
    /** 通道名称。 */
    private String channelName;
    /** 目标类型。 */
    private String targetType;
    /** 目标地址。 */
    private Integer targetAddress;
    /** 工作模式。 */
    private Integer mode;
    /** 命令状态：pending/success/failed/rejected/timeout。 */
    private String status;
    /** 请求码。 */
    private Integer requestCode;
    /** 响应码。 */
    private Integer responseCode;
    /** 协议编码。 */
    private String protocolCode;
    /** 命令名称。 */
    private String commandName;
    /** 请求载荷。 */
    private String requestPayload;
    /** 响应载荷。 */
    private String responsePayload;
    /** 错误信息。 */
    private String errorMessage;
    /** 轮询批次号。 */
    private String pollBatchNo;
    /** 开始时间。 */
    private String startedAt;
    /** 结束时间。 */
    private String endedAt;

    ///////////////////////////////////////// 缓存


}
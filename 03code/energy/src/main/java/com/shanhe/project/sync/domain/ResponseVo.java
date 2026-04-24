package com.shanhe.project.sync.domain;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 同步平台响应类
 *
 * @author wjh
 * @since 2025/5/19
 */
@Data
@Accessors(chain = true)
public class ResponseVo implements Serializable {
    /** 时间戳 **/
    private Long timestamp;
    /** 请求方法 **/
    private String method;
    /** 设备ID **/
    private String imei;
    /** 结果码：0成功1失败 **/
    private Integer code;
    /** 结果描述信息 */
    private String message;
    /** 业务ID **/
    private String businessId;
    /** 响应内容 **/
    private Object content;

    /**
     * 响应处理结果
     */
    public ResponseVo(String imei, String method, String businessId, Object content) {
        this.timestamp = System.currentTimeMillis();
        this.imei = imei;
        this.method = method;
        this.businessId = businessId;
        this.code = 0;
        this.content = content;
    }

    /**
     * 响应处理结果
     */
    public ResponseVo(String imei, String method, String businessId, String message) {
        this.timestamp = System.currentTimeMillis();
        this.imei = imei;
        this.method = method;
        this.businessId = businessId;
        this.code = message == null ? 0 : 1;
        this.message = message;
    }

    /**
     * 响应处理结果
     */
    public ResponseVo(String imei, String method, String businessId, Integer code, String message) {
        this.timestamp = System.currentTimeMillis();
        this.imei = imei;
        this.method = method;
        this.businessId = businessId;
        this.code = code;
        this.message = message;
    }

    public String toJsonString() {
        return JSONObject.toJSONString(this);
    }
}

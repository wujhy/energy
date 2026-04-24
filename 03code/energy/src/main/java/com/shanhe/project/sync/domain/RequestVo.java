package com.shanhe.project.sync.domain;

import com.alibaba.fastjson2.JSONObject;
import com.shanhe.framework.enums.HostTypeEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 同步平台请求类
 *
 * @author wjh
 * @since 2025/5/19
 */
@Data
@Accessors(chain = true)
public class RequestVo implements Serializable {
    /** 时间戳 **/
    private Long timestamp;
    /** 设备ID **/
    private String imei;
    /** 请求方法 **/
    private String method;
    /** 设备模型 **/
    private String deviceModel;
    /** 业务ID **/
    private String businessId;
    /** 认证token **/
    private String token;
    /** 内容 **/
    private Object content;
    /** 类型 **/
    private Integer validType;

    public RequestVo() {}

    public RequestVo(String imei, String method, Object content) {
        this.timestamp = System.currentTimeMillis();
        this.deviceModel = HostTypeEnum._2CM03N.getDictValue();
        this.imei = imei;
        this.method = method;
        this.content = content;
    }

    public String toJsonString() {
        return JSONObject.toJSONString(this);
    }
}

package com.shanhe.project.monitor.patrol.domain;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.shanhe.common.utils.DateUtils;
import com.shanhe.framework.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * 巡检对象
 *
 * @author wjh
 * @since 2025/5/16
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Patrol extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long pId;
    /** 巡检名称 */
    private String name;
    /** 主机 */
    private String imei;
    /** 用户名 */
    private String userName;

    /** 巡检时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date pDate;
    private String pDateStr;

    /** 巡检结果0正常1异常 */
    private Integer pResult;

    /** 是否已上报0是1否 */
    private Integer pReport;
    /** 上报时间 */
    private Date pReportDate;

    public Long getpId() {
        return pId;
    }

    public void setpId(Long pId) {
        this.pId = pId;
    }

    public Date getpDate() {
        return pDate;
    }

    public void setpDate(Date pDate) {
        this.pDateStr = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, pDate);
        this.pDate = pDate;
    }

    public Date getpReportDate() {
        return pReportDate;
    }

    public void setpReportDate(Date pReportDate) {
        this.pReportDate = pReportDate;
    }

    public Integer getpResult() {
        return pResult;
    }

    public void setpResult(Integer pResult) {
        this.pResult = pResult;
    }

    public Integer getpReport() {
        return pReport;
    }

    public void setpReport(Integer pReport) {
        this.pReport = pReport;
    }

    /** 巡检设备列表 */
    private List<PatrolContent> configList;
    private String configs;

    public void setConfigList(List<PatrolContent> configList) {
        if (configList != null && !configList.isEmpty()) {
            configs = JSON.toJSONString(configList);
        }
        this.configList = configList;
    }

    public void setConfigs(String configs) {
        if (StrUtil.isNotBlank(configs)) {
            configList = JSON.parseArray(configs, PatrolContent.class);
        }
        this.configs = configs;
    }

    public String getpDateStr() {
        if (this.pDate != null) {
            this.pDateStr = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, this.pDate);
        }
        return pDateStr;
    }
}

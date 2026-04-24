package com.shanhe.project.sync.common;

import com.shanhe.project.monitor.patrol.domain.Patrol;
import com.shanhe.project.sync.domain.PatrolVo;

import java.util.Objects;

/**
 * 巡检工具类
 *
 * @author wjh
 * @since 2025/6/27
 */
public class PatrolUtil {

    /**
     * 上报巡检
     *
     * @param patrol 本地
     */
    public static PatrolVo uploadPatrol(Patrol patrol) {
        if (Objects.isNull(patrol)) {
            return null;
        }
        PatrolVo patrolVo = new PatrolVo();
        patrolVo.setpId(patrol.getpId());
        patrolVo.setImei(patrol.getImei());
        patrolVo.setpDate(patrol.getpDate());
        patrolVo.setpResult(patrol.getpResult());
        patrolVo.setRemark(patrol.getRemark());
        patrolVo.setUserName(patrol.getUserName());
        patrolVo.setListData(patrol.getConfigList());
        return patrolVo;
    }
}

package com.shanhe.project.monitor.patrol.service;

import com.shanhe.project.monitor.patrol.domain.Patrol;
import com.shanhe.project.monitor.patrol.vo.PatrolVO;

import java.util.List;

/**
 * 巡检Service接口
 *
 * @author wjh
 * @since 2025/5/16
 */
public interface IPatrolService {
    /**
     * 查询巡检
     *
     * @param pId 巡检主键
     * @return 巡检
     */
    Patrol selectByPid(Long pId);

    /**
     * 查询巡检列表
     *
     * @param patrol 巡检
     * @return 巡检集合
     */
    List<Patrol> selectList(Patrol patrol);

    /**
     * 新增巡检
     *
     * @param patrol 巡检
     */
    void insert(Patrol patrol);
    /**
     * 更新巡检
     *
     * @param patrol 巡检
     */
    Patrol update(PatrolVO patrol);

    /**
     * 上报巡检
     */
    void report(PatrolVO patrol);

    /**
     * 批量删除巡检
     *
     * @param pIds 需要删除的巡检主键集合
     */
    void deleteByPids(List<Long> pIds);

    /**
     * 删除巡检信息
     *
     * @param pId 巡检主键
     */
    void deleteByPid(Long pId);

    /**
     * 判断是否已经巡检
     */
    Boolean hasPatrol();
}

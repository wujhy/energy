package com.shanhe.project.monitor.patrol.service;

import com.shanhe.project.monitor.patrol.domain.PatrolTemplate;

import java.util.List;

/**
 * 巡检模板Service接口
 *
 * @author wjh
 * @since 2025/7/1
 */
public interface IPatrolTemplateService {
    /**
     * 查询巡检模板
     *
     * @param id 巡检模板主键
     * @return 巡检模板
     */
    PatrolTemplate selectById(Long id);

    /**
     * 查询巡检模板列表
     *
     * @param patrolTemplate 巡检模板
     * @return 巡检模板集合
     */
    List<PatrolTemplate> selectList(PatrolTemplate patrolTemplate);

    /**
     * 查询巡检模板
     *
     * @return 巡检模板集合
     */
    PatrolTemplate selectOne();

    /**
     * 新增巡检模板
     *
     * @param patrolTemplate 巡检模板
     */
    void insert(PatrolTemplate patrolTemplate);

    /**
     * 修改巡检模板
     *
     * @param patrolTemplate 巡检模板
     */
    void update(PatrolTemplate patrolTemplate);

    /**
     * 删除巡检模板信息
     *
     * @param id 巡检模板主键
     */
    void deleteById(Long id);
}

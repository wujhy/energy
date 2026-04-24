package com.shanhe.project.monitor.patrol.service;

import com.shanhe.project.monitor.patrol.domain.PatrolTemplateContent;

import java.util.List;

/**
 * 巡检模板内容Service接口
 *
 * @author wjh
 * @since 2025/7/1
 */
public interface IPatrolTemplateContentService {
    /**
     * 查询巡检模板内容
     *
     * @param id 巡检模板内容主键
     * @return 巡检模板内容
     */
    PatrolTemplateContent selectById(Long id);

    /**
     * 查询巡检模板内容列表
     *
     * @param patrolTemplateContent 巡检模板内容
     * @return 巡检模板内容集合
     */
    List<PatrolTemplateContent> selectList(PatrolTemplateContent patrolTemplateContent);

    /**
     * 查询巡检模板内容
     *
     * @return 巡检模板内容集合
     */
    List<PatrolTemplateContent> viewList();

    /**
     * 新增巡检模板内容
     *
     * @param patrolTemplateContent 巡检模板内容
     */
    void insert(PatrolTemplateContent patrolTemplateContent);

    /**
     * 新增巡检模板内容
     *
     * @param patrolTemplateContent 巡检模板内容
     */
    void save(PatrolTemplateContent patrolTemplateContent);

    /**
     * 修改巡检模板内容
     *
     * @param patrolTemplateContent 巡检模板内容
     */
    void update(PatrolTemplateContent patrolTemplateContent);

    /**
     * 删除巡检模板内容信息
     *
     * @param id 巡检模板内容主键
     */
    void deleteById(Long id);

    /**
     * 删除巡检模板内容信息
     *
     * @param templateId 巡检模板主键
     */
    void deleteByTemplateId(Long templateId);
}

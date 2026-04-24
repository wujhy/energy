package com.shanhe.project.monitor.patrol.mapper;

import com.shanhe.project.monitor.patrol.domain.PatrolTemplateContent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 巡检模板内容Mapper接口
 *
 * @author wjh
 * @since 2025/7/1
 */
@Mapper
public interface PatrolTemplateContentMapper {
    /**
     * 查询巡检模板内容
     *
     * @param id 巡检模板内容主键
     */
    PatrolTemplateContent selectById(Long id);

    /**
     * 查询巡检模板内容列表
     *
     * @param patrolTemplateContent 巡检模板内容
     */
    List<PatrolTemplateContent> selectList(PatrolTemplateContent patrolTemplateContent);

    /**
     * 查询巡检模板内容
     *
     * @param patrolTemplateContent 巡检模板内容
     */
    PatrolTemplateContent selectOne(PatrolTemplateContent patrolTemplateContent);

    /**
     * 新增巡检模板内容
     *
     * @param patrolTemplateContent 巡检模板内容
     */
    void insert(PatrolTemplateContent patrolTemplateContent);

    void insertList(@Param("list") List<PatrolTemplateContent> patrolTemplateContent);

    /**
     * 修改巡检模板内容
     *
     * @param patrolTemplateContent 巡检模板内容
     */
    void update(PatrolTemplateContent patrolTemplateContent);

    /**
     * 删除巡检模板内容
     *
     * @param id 巡检模板内容主键
     */
    void deleteById(Long id);

    /**
     * 批量删除巡检模板内容
     *
     * @param ids 需要删除的数据主键集合
     */
    void deleteByIds(@Param("ids") List<Long> ids);

    void deleteByTemplateId(Long id);
}

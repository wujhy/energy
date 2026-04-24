package com.shanhe.project.monitor.patrol.mapper;

import com.shanhe.project.monitor.patrol.domain.PatrolTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 巡检模板Mapper接口
 *
 * @author wjh
 * @since 2025/7/1
 */
@Mapper
public interface PatrolTemplateMapper {
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
     * @return 巡检模板
     */
    PatrolTemplate selectOne();

    /**
     * 新增巡检模板
     *
     * @param patrolTemplate 巡检模板
     */
    void insert(PatrolTemplate patrolTemplate);

    void insertList(@Param("list") List<PatrolTemplate> patrolTemplates);

    /**
     * 修改巡检模板
     *
     * @param patrolTemplate 巡检模板
     */
    void update(PatrolTemplate patrolTemplate);

    /**
     * 删除巡检模板
     *
     * @param id 巡检模板主键
     */
    void deleteById(Long id);

    /**
     * 批量删除巡检模板
     *
     * @param ids 需要删除的数据主键集合
     */
    void deleteByIds(@Param("ids") List<Long> ids);
}

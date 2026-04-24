package com.shanhe.project.monitor.patrol.mapper;

import com.shanhe.project.monitor.patrol.domain.Patrol;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 巡检
 *
 * @author wjh
 * @since 2025/5/16
 */
@Mapper
public interface PatrolMapper {
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
     * 修改巡检
     *
     * @param patrol 巡检
     */
    void update(Patrol patrol);

    /**
     * 上报结果
     */
    void report(Long pId);

    /**
     * 删除巡检
     *
     * @param pId 巡检主键
     */
    void deleteByPid(Long pId);

    /**
     * 批量删除巡检
     *
     * @param pIds 需要删除的数据主键集合
     */
    void deleteByPids(@Param("pIds") List<Long> pIds);

    /**
     * 判断是否已经巡检
     */
    Long hasPatrol();
}

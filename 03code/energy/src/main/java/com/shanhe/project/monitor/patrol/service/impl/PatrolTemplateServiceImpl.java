package com.shanhe.project.monitor.patrol.service.impl;

import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.project.monitor.patrol.domain.PatrolTemplate;
import com.shanhe.project.monitor.patrol.mapper.PatrolTemplateContentMapper;
import com.shanhe.project.monitor.patrol.mapper.PatrolTemplateMapper;
import com.shanhe.project.monitor.patrol.service.IPatrolTemplateService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 巡检模板Service业务层处理
 *
 * @author wjh
 * @since 2025/7/1
 */
@Service
public class PatrolTemplateServiceImpl implements IPatrolTemplateService {
    @Resource
    private PatrolTemplateContentMapper patrolTemplateContentMapper;
    @Resource
    private PatrolTemplateMapper patrolTemplateMapper;

    @Override
    public PatrolTemplate selectById(Long id) {
        return patrolTemplateMapper.selectById(id);
    }

    @Override
    public List<PatrolTemplate> selectList(PatrolTemplate patrolTemplate) {
        return patrolTemplateMapper.selectList(patrolTemplate);
    }

    @Override
    public PatrolTemplate selectOne() {
        return patrolTemplateMapper.selectOne();
    }

    @Override
    public void insert(PatrolTemplate patrolTemplate) {
        patrolTemplate.setId(IdUtils.getSnowflakeId());
        patrolTemplate.setTimestamp(System.currentTimeMillis());
        patrolTemplateMapper.insert(patrolTemplate);
    }

    @Override
    public void update(PatrolTemplate patrolTemplate) {
        patrolTemplate.setTimestamp(System.currentTimeMillis());
        patrolTemplateMapper.update(patrolTemplate);
    }

    @Override
    public void deleteById(Long id) {
        patrolTemplateMapper.deleteById(id);
        patrolTemplateContentMapper.deleteByTemplateId(id);
    }
}

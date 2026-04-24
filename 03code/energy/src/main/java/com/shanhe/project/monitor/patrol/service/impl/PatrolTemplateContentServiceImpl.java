package com.shanhe.project.monitor.patrol.service.impl;

import com.shanhe.common.exception.ServiceException;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.project.monitor.patrol.domain.PatrolTemplate;
import com.shanhe.project.monitor.patrol.domain.PatrolTemplateContent;
import com.shanhe.project.monitor.patrol.mapper.PatrolTemplateContentMapper;
import com.shanhe.project.monitor.patrol.mapper.PatrolTemplateMapper;
import com.shanhe.project.monitor.patrol.service.IPatrolTemplateContentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 巡检模板内容Service业务层处理
 *
 * @author wjh
 * @since 2025/7/1
 */
@Service
public class PatrolTemplateContentServiceImpl implements IPatrolTemplateContentService {

    @Resource
    private PatrolTemplateContentMapper patrolTemplateContentMapper;
    @Resource
    private PatrolTemplateMapper patrolTemplateMapper;

    @Override
    public PatrolTemplateContent selectById(Long id) {
        return patrolTemplateContentMapper.selectById(id);
    }

    /**
     * 查询巡检模板内容列表
     *
     * @param patrolTemplateContent 巡检模板内容
     * @return 巡检模板内容
     */
    @Override
    public List<PatrolTemplateContent> selectList(PatrolTemplateContent patrolTemplateContent) {
        return patrolTemplateContentMapper.selectList(patrolTemplateContent);
    }

    @Override
    public List<PatrolTemplateContent> viewList() {
        PatrolTemplate patrolTemplate = patrolTemplateMapper.selectOne();
        if (patrolTemplate == null) {
            throw new ServiceException("未设置有效巡检模板");
        }
        return patrolTemplateContentMapper.selectList(new PatrolTemplateContent().setTemplateId(patrolTemplate.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insert(PatrolTemplateContent patrolTemplateContent) {
        patrolTemplateContent.setId(IdUtils.getSnowflakeId());
        patrolTemplateContentMapper.insert(patrolTemplateContent);

        resetTimestamp(patrolTemplateContent.getTemplateId());
    }

    @Override
    public void save(PatrolTemplateContent patrolTemplateContent) {
        patrolTemplateContent.setId(IdUtils.getSnowflakeId());
        patrolTemplateContentMapper.insert(patrolTemplateContent);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PatrolTemplateContent patrolTemplateContent) {
        patrolTemplateContentMapper.update(patrolTemplateContent);

        resetTimestamp(patrolTemplateContent.getTemplateId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        PatrolTemplateContent patrolTemplateContent = patrolTemplateContentMapper.selectById(id);
        if (patrolTemplateContent == null) {
            return;
        }
        patrolTemplateContentMapper.deleteById(id);

        resetTimestamp(patrolTemplateContent.getTemplateId());
    }

    @Override
    public void deleteByTemplateId(Long templateId) {
        patrolTemplateContentMapper.deleteByTemplateId(templateId);
    }

    /**
     * 重置时间
     */
    private void resetTimestamp(Long templateId) {
        Date date = new Date();
        PatrolTemplate patrolTemplate = new PatrolTemplate().setId(templateId);
        patrolTemplate.setUpdateTime(date);

        patrolTemplate.setTimestamp(date.getTime());
        patrolTemplateMapper.update(patrolTemplate);
    }
}

package com.shanhe.project.monitor.patrol.service.impl;

import com.shanhe.common.exception.ServiceException;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.monitor.patrol.domain.Patrol;
import com.shanhe.project.monitor.patrol.domain.PatrolContent;
import com.shanhe.project.monitor.patrol.vo.PatrolVO;
import com.shanhe.project.monitor.patrol.mapper.PatrolMapper;
import com.shanhe.project.monitor.patrol.service.IPatrolService;
import com.shanhe.project.sync.service.ClientReportService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * 巡检Service业务层处理
 *
 * @author wjh
 * @since 2025/5/16
 */
@Service
public class PatrolServiceImpl implements IPatrolService {

    @Resource
    private PatrolMapper patrolMapper;
    @Resource
    private ClientReportService clientReportService;

    @Override
    public Patrol selectByPid(Long pId) {
        return patrolMapper.selectByPid(pId);
    }

    /**
     * 查询巡检列表
     *
     * @param patrol 巡检
     * @return 巡检
     */
    @Override
    public List<Patrol> selectList(Patrol patrol) {
        return patrolMapper.selectList(patrol);
    }

    /**
     * 新增巡检
     *
     * @param patrol 巡检
     */
    @Override
    public void insert(Patrol patrol) {
        patrol.setpId(IdUtils.getSnowflakeId());
        // 巡检结果
        this.result(patrol);

        patrolMapper.insert(patrol);
    }

    @Override
    public Patrol update(PatrolVO patrolVO) {
        Patrol patrol = patrolMapper.selectByPid(patrolVO.getpId());
        if (patrol == null) {
            throw new ServiceException("巡检记录不存在，操作失败");
        }

        if (Objects.equals(patrol.getpReport(), YesNoEnum.YES.getDictValue())) {
            throw new ServiceException("巡检记录已上报，不可编辑");
        }
        patrol.setRemark(patrolVO.getRemark());
        patrol.setUserName(patrolVO.getUserName());
        patrol.setConfigList(patrolVO.getConfigList());
        patrol.setpReport(YesNoEnum.YES.getDictValue());
        // 巡检结果
        this.result(patrol);

        patrolMapper.update(patrol);
        return patrol;
    }

    private void result(Patrol patrol) {
        // 默认为正常
        patrol.setpResult(YesNoEnum.YES.getDictValue());
        if (patrol.getConfigList() == null || patrol.getConfigList().isEmpty()) {
            return;
        }
        // 存在异常项则为异常
        for (PatrolContent patrolContent : patrol.getConfigList()) {
            if (Objects.equals(patrolContent.getStatus(), YesNoEnum.NO.getDictValue())) {
                patrol.setpResult(YesNoEnum.NO.getDictValue());
                break;
            }
        }
    }

    @Override
    public void report(PatrolVO patrolVO) {
        // 更新巡检记录
        Patrol patrol = this.update(patrolVO);

        // 检查是否需要上报
        if (clientReportService.needSend()) {
            if (!clientReportService.canSend()) {
                throw new ServiceException("未与服务端建立连接，巡检记录不可上报");
            }
            // 执行上报
            clientReportService.uploadPatrol(patrol);
        }

        // 更新记录
        patrolMapper.report(patrol.getpId());
    }

    /**
     * 批量删除巡检
     *
     * @param pIds 需要删除的巡检主键
     */
    @Override
    public void deleteByPids(List<Long> pIds) {
        patrolMapper.deleteByPids(pIds);
    }

    /**
     * 删除巡检信息
     *
     * @param pId 巡检主键
     */
    @Override
    public void deleteByPid(Long pId) {
        Patrol patrol = patrolMapper.selectByPid(pId);
        if (patrol == null) {
            return;
        }

        if (Objects.equals(patrol.getpReport(), YesNoEnum.YES.getDictValue())) {
            throw new ServiceException("巡检记录已上报，不可删除");
        }
        patrolMapper.deleteByPid(pId);
    }

    @Override
    public Boolean hasPatrol() {
        return patrolMapper.hasPatrol() > 0;
    }
}

package com.shanhe.project.sync.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.http.HttpUtil;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.file.JarUploadUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.project.monitor.server.service.SystemService;
import com.shanhe.project.sync.domain.RequestVo;
import com.shanhe.project.sync.service.ClientReportService;
import lombok.extern.slf4j.Slf4j;

import java.util.TimerTask;

/**
 * 升级异步任务
 *
 * @author wjh
 * @since 2025/6/19
 */
@Slf4j
public class DeployTask {

    public static TimerTask deploy(RequestVo request, String softVersion, String url, ClientReportService clientReportService) {
        // 下载文件进度 10 / 100
        final double[] downloadFileProgress = { 0.1D };

        return new TimerTask()
        {
            @Override
            public void run()
            {
                String msg = null;
                try {
                    // hutool基于远程URL下载jar包到本地
                    long size = HttpUtil.downloadFile(url, JarUploadUtils.getSoftFile(), new StreamProgress(){
                        @Override
                        public void start() {
                            log.info("开始下载：{}", url);
                        }
                        // 每隔 10% 记录一次日志
                        @Override
                        public void progress(long total, long progressSize) {
                            double downloadFileProgressTemp = (double) progressSize / total;
                            if (downloadFileProgressTemp >= downloadFileProgress[0]) {
                                downloadFileProgress[0] += 0.1D;
                                log.info("已下载: {}% [{} / {}]", downloadFileProgressTemp * 100,
                                        FileUtil.readableFileSize(progressSize), FileUtil.readableFileSize(total));
                            }
                        }
                        @Override
                        public void finish() {
                            log.info("下载完成！");
                        }
                    });
                    // 文件小于80M，则下载失败
                    if (JarUploadUtils.validJarSize(size)) {
                        msg = "下载失败，文件大小异常！！！";
                    }
                } catch (Exception e) {
                    msg = String.format("下载失败：%s", e.getMessage());
                    log.error(msg);
                } finally {
                    // 清除缓存
                    CacheUtils.remove(CacheKeyEnum.DEPLOY_DOWNLOAD.getCache(), CacheKeyEnum.DEPLOY_DOWNLOAD.getKey());
                    // 响应结果
                    clientReportService.updateSoftResult(request.getImei(), softVersion, msg);
                }

                // 无异常信息，执行升级
                if (msg == null) {
                    // 下载完成，开始升级
                    log.info("下载完成，开始升级");
                    SystemService.deploy();
                }
            }
        };
    }

}

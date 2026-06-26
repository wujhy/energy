package com.shanhe.project.system.file.service;


import com.shanhe.project.system.file.domain.SysLogFile;

import java.io.IOException;
import java.util.List;

/**
 * 文件 服务层
 *
 * @author wjh
 * @since 2026-05-25
 */
public interface ISysFileService {

    /**
     * 列出文件目录下的内容
     *
     * @param directoryPath 目录
     * @return 日志文件列表
     * @throws IOException IO异常
     */
    List<SysLogFile> listContents(String directoryPath) throws IOException;

}
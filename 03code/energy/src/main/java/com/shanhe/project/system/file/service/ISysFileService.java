package com.shanhe.project.system.file.service;


import com.shanhe.project.system.file.domain.SysLogFile;

import java.io.IOException;
import java.util.List;

/**
 * 文件
 *
 * @author zhoubin
 */
public interface ISysFileService {

    /**
     * 列出文件目录下的内容
     *
     * @param directoryPath 目录
     */
    List<SysLogFile> listContents(String directoryPath) throws IOException;

}
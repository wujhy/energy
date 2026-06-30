package com.shanhe.project.manage.file.service.impl;

import com.shanhe.common.constant.Constants;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.common.utils.StringUtils;
import com.shanhe.project.manage.file.domain.SysLogFile;
import com.shanhe.project.manage.file.service.ISysFileService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件服务实现
 *
 * @author wjh
 * @since 2026-05-25
 */
@Service
public class SysFileServiceImpl implements ISysFileService {

    private void check(String resource) {
        // 禁止目录上跳级别
        if (StringUtils.contains(resource, "..")) {
            throw new ServiceException("禁止目录上跳级别");
        }
    }

    @Override
    public List<SysLogFile> listContents(String directoryPath) throws IOException {
        check(directoryPath);

        Path currentDirectory = Paths.get(directoryPath);
        if (!Files.exists(currentDirectory) || !Files.isDirectory(currentDirectory)) {
            throw new ServiceException("路径不存在或不是一个目录: " + currentDirectory);
        }

        List<SysLogFile> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentDirectory)) {
            for (Path entry : stream) {
                boolean isDirectory = Files.isDirectory(entry);
                SysLogFile sysLogFile = new SysLogFile();
                sysLogFile.setFileName(entry.getFileName().toString());
                sysLogFile.setIsDir(isDirectory ? "1" : "0");
                if (!isDirectory) {
                    sysLogFile.setFileSize(Files.size(entry));
                }
                sysLogFile.setLastModifiedTime(new Date(Files.getLastModifiedTime(entry).toMillis()));
                sysLogFile.setFilePath(entry.toString());

                if (isDirectory  && Constants.USB_PATH.equals(directoryPath)) {
                    int count = 0;
                    try (DirectoryStream<Path> subStream = Files.newDirectoryStream(entry)) {
                        for (Path ignored : subStream) {
                            count++;
                            break;
                        }
                    } catch (Exception ignored) {
                    }
                    sysLogFile.setIsSub(count);
                }
                result.add(sysLogFile);
            }
        }

        if (Constants.USB_PATH.equals(directoryPath)) {
            result = result.stream().filter(sysLogFile -> sysLogFile.getIsSub() > 0).collect(Collectors.toList());
            if (result.isEmpty()) {
                throw new RuntimeException("U盘识别失败");
            }
        }
        return result;
    }

}

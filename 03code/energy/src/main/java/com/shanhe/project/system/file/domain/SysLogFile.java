package com.shanhe.project.system.file.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统日志文件对象
 *
 * @author wjh
 * @since 2026-05-25
 */
@Data
public class SysLogFile implements Serializable {
    private static final long serialVersionUID = 1L;

    // 文件名称
    private String fileName;

    // 文件路径
    private String filePath;

    // 文件大小
    private Long fileSize;

    // 是否文件夹
    private String isDir;

    // 最后跟新时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastModifiedTime;

    // 是否子文件
    private Integer isSub;

}

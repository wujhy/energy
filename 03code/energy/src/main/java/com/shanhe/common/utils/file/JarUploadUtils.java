package com.shanhe.common.utils.file;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.shanhe.framework.consts.SysConst;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.rmi.ServerException;

/**
 * jar包上传工具类
 *
 * @author wjh
 * @since 2025/7/15
 */
public class JarUploadUtils {

    /** 升级jar包大小：最小、最大 */
    public static final long JAR_MIN_SIZE = 80 * 1024 * 1024;
    public static final long JAR_MAX_SIZE = 120 * 1024 * 1024;
    public static final String JAR_EXTENSION = ".jar";

    /**
     * 根据文件路径上传
     *
     * @param file 上传的文件
     */
    public static void uploadSoft(MultipartFile file) throws IOException {
        try {
            if (!StrUtil.endWith(file.getOriginalFilename(), JAR_EXTENSION)) {
                throw new ServerException("文件类型错误！！");
            }
            if (validJarSize(file.getSize())) {
                throw new ServerException("文件大小不符！！");
            }
            // 保存
            file.transferTo(getSoftFile());
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /** 获取升级包文件 */
    public static File getSoftFile() throws ServerException {
        File softFile = FileUtil.file(SysConst.getSoftDownloadPath());
        if (softFile.exists() || softFile.getParentFile().exists() || softFile.getParentFile().mkdirs()) {
            return softFile;
        } else {
            throw new ServerException("存储文件不存在！！");
        }
    }

    /**
     * 校验jar文件大小
     */
    public static boolean validJarSize(Long size) {
        return size < JAR_MIN_SIZE || size > JAR_MAX_SIZE;
    }
}
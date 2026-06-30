package com.shanhe.common.utils.file;

/**
 * 媒体类型工具类
 * 
 * @author ruoyi
 */
public class MimeTypeUtils
{
    /** PNG图片MIME类型。 */
    public static final String IMAGE_PNG = "image/png";

    /** JPG图片MIME类型。 */
    public static final String IMAGE_JPG = "image/jpg";

    /** JPEG图片MIME类型。 */
    public static final String IMAGE_JPEG = "image/jpeg";

    /** BMP图片MIME类型。 */
    public static final String IMAGE_BMP = "image/bmp";

    /** GIF图片MIME类型。 */
    public static final String IMAGE_GIF = "image/gif";

    /** 图片文件扩展名数组。 */
    public static final String[] IMAGE_EXTENSION = { "bmp", "gif", "jpg", "jpeg", "png" };

    /** Flash文件扩展名数组。 */
    public static final String[] FLASH_EXTENSION = { "swf", "flv" };

    /** 媒体文件扩展名数组。 */
    public static final String[] MEDIA_EXTENSION = { "swf", "flv", "mp3", "wav", "wma", "wmv", "mid", "avi", "mpg",
            "asf", "rm", "rmvb" };

    /** 视频文件扩展名数组。 */
    public static final String[] VIDEO_EXTENSION = { "mp4", "avi", "rmvb" };

    /** 默认允许上传的文件扩展名数组。 */
    public static final String[] DEFAULT_ALLOWED_EXTENSION = {
            // 图片
            "bmp", "gif", "jpg", "jpeg", "png",
            // word excel powerpoint
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "html", "htm", "txt",
            // 压缩文件
            "rar", "zip", "gz", "bz2",
            // 视频格式
            "mp4", "avi", "rmvb",
            // pdf
            "pdf" };

    public static String getExtension(String prefix)
    {
        switch (prefix)
        {
            case IMAGE_PNG:
                return "png";
            case IMAGE_JPG:
                return "jpg";
            case IMAGE_JPEG:
                return "jpeg";
            case IMAGE_BMP:
                return "bmp";
            case IMAGE_GIF:
                return "gif";
            default:
                return "";
        }
    }
}

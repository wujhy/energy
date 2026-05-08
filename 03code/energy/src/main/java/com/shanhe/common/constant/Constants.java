package com.shanhe.common.constant;

/**
 * 通用常量信息
 *
 * @author wjh
 * @since 2025/7/14
 */
public class Constants
{
    /**
     * UTF-8 字符集
     */
    public static final String INDEX = "index.html";
    /**
     * UTF-8 字符集
     */
    public static final String UTF8 = "UTF-8";

    /**
     * GBK 字符集
     */
    public static final String GBK = "GBK";

    /**
     * http请求
     */
    public static final String HTTP = "http://";

    /**
     * https请求
     */
    public static final String HTTPS = "https://";

    /**
     * 通用成功标识
     */
    public static final String SUCCESS = "0";

    /**
     * 通用失败标识
     */
    public static final String FAIL = "1";

    /**
     * 登录成功
     */
    public static final String LOGIN_SUCCESS = "Success";

    /**
     * 注销
     */
    public static final String LOGOUT = "Logout";

    /**
     * 注册
     */
    public static final String REGISTER = "Register";

    /**
     * 登录失败
     */
    public static final String LOGIN_FAIL = "Error";

    /**
     * 当前记录起始索引
     */
    public static final String PAGE_NUM = "pageNum";

    /**
     * 每页显示记录数
     */
    public static final String PAGE_SIZE = "pageSize";

    /**
     * 排序列
     */
    public static final String ORDER_BY_COLUMN = "orderByColumn";

    /**
     * 排序的方向 "desc" 或者 "asc".
     */
    public static final String IS_ASC = "isAsc";

    /**
     * 系统用户授权缓存
     */
    public static final String SYS_AUTH_CACHE = "sys-authCache";

    /**
     * 资源映射路径 前缀
     */
    public static final String RESOURCE_PREFIX = "/profile";
    /**
     * 单体电池预估容量KEY
     */
    public static final String CAP_BAT = "cap_bat_";

    /**
     * 资源映射路径 前缀
     */
    public static final String USB_PATH = "/media";

    /**
     * 默认蓄电池设备ID
     */
    public static final Long DEFAULT_CONFIG_ID = 1L;

    /**
     * 默认蓄电池模板ID
     */
    public static final Long DEFAULT_TEMPLATE_ID = 1L;
}

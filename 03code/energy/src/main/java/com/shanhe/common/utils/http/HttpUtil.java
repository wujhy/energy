package com.shanhe.common.utils.http;

import com.alibaba.fastjson2.JSON;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTTP工具类
 *
 * @author wjh
 * @since 2025/7/19
 */
public class HttpUtil {

	private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);

	public final static String SUCCESS = "200";
	public final static String FAIL = "404";
	public final static int CONN_TIMEOUT = 2000;
	public final static int READ_TIMEOUT = 2000;

	public static void main(String[] args) {

	}

	/**
	 * HTTP GET
	 * 
	 * @param urlStr 请求地址
	 * @param retry 重试次数
	 * @return 结果
	 */
	public static String doGet(String urlStr, int retry) {
		String flag = FAIL;
		for (int i = 0; i < retry; i++) {
			flag = httpGet(urlStr);
			if (flag.startsWith(SUCCESS)) {
				flag = flag.substring(3);
				break;
			}
		}
		return flag;
	}

	/**
	 * URL请求
	 */
	public static String httpGet(String urlStr) {
		StringBuilder flag = new StringBuilder(FAIL);
		URL u;
		try {
			u = new URL(urlStr);
			if ("https".equalsIgnoreCase(u.getProtocol())) {
				SslUtils.ignoreSsl();
			}
			HttpURLConnection conn = (HttpURLConnection) u.openConnection();
			conn.setRequestProperty("Content-Type", "text/html; charset=UTF-8");
			conn.setConnectTimeout(CONN_TIMEOUT);// 连接超时
			conn.setReadTimeout(READ_TIMEOUT);// 读操作超时
			conn.connect();
			flag = new StringBuilder(String.valueOf(conn.getResponseCode())); // suc=200

			InputStream is = conn.getInputStream();
			InputStreamReader in = new InputStreamReader(is, StandardCharsets.UTF_8);
			BufferedReader reader = new BufferedReader(in);

			String lines;
			while (StringUtils.isNotEmpty((lines = reader.readLine()))) {
				flag.append(lines);
			}
		} catch (SocketTimeoutException s) {
            logger.error("!!!连接或读操作超时!{}", urlStr);
		} catch (UnknownHostException unknown) {
            logger.error("!!!无效的URL地址!{}", urlStr);
		} catch (FileNotFoundException f) {
            logger.error("!!!数据流异常!{}", urlStr);
		} catch (Exception e) {
            logger.error("!!!Http请求报错!{}：{}", urlStr, e.getMessage());
		}

		return flag.toString();
	}

	/**
	 * HTTP POST
	 */
	public static String doPost(String urlStr, Map<String, String> params,
			String encode, int retry) {
		String flag = FAIL;
		for (int i = 0; i < retry; i++) {
			flag = httpPostReq(urlStr, params, encode);
			if (flag.startsWith(SUCCESS)) {
				flag = flag.substring(3);
				break;
			}
		}
		return flag;
	}

	/**
	 * HTTP POST File请求
	 */
	public static String httpPostReq(String urlStr, Map<String, String> params,
			String encode) {
		StringBuilder result = new StringBuilder();
		OutputStream outputStream = null;
		InputStream inputStream = null;
		InputStreamReader inputStreamReader = null;
		BufferedReader reader = null;
		try {
			URL url = new URL(urlStr);
			if ("https".equalsIgnoreCase(url.getProtocol())) {
				SslUtils.ignoreSsl();
			}
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("User-agent", "Mozilla/4.0");
			conn.setRequestProperty("Accept-Language", "zh-cn,zh;q=0.5");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setUseCaches(false);
			conn.setConnectTimeout(CONN_TIMEOUT);// 连接超时
			conn.setReadTimeout(READ_TIMEOUT);// 读操作超时
			if (StringUtils.isEmpty(encode)) {
				encode = "utf-8";
			}
			if (params != null) {
				StringBuilder param = new StringBuilder();
				for (String key : params.keySet()) {
					param.append("&");
					param.append(key).append("=");
					param.append(java.net.URLEncoder.encode(params.get(key), encode));
				}
				outputStream = conn.getOutputStream();
				outputStream.write(param.toString().getBytes());
				outputStream.flush();
				outputStream.close();
			}
			inputStream = conn.getInputStream();
			inputStreamReader = new InputStreamReader(inputStream);
			reader = new BufferedReader(inputStreamReader);
			result = new StringBuilder(String.valueOf(conn.getResponseCode())); // suc=200
			if (conn.getResponseCode() == 200) {
				String line;
				for (line = reader.readLine(); line != null; line = reader.readLine()) {
					result.append(line);
				}
			}
		} catch (MalformedURLException e) {
            logger.error("!!!Http请求URL报错!{}参数 :{}", urlStr, JSON.toJSONString(params));
		} catch (IOException e) {
            logger.error("!!!Http请求IO报错!{}参数 :{}", urlStr, JSON.toJSONString(params));
		} catch (Exception e) {
            logger.error("!!!Http请求报错!{}参数 :{}，错误：{}", urlStr, JSON.toJSONString(params), e.getMessage());
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ignored) {}
			}
			if (inputStreamReader != null) {
				try {
					inputStreamReader.close();
				} catch (IOException ignored) {}
			}
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException ignored) {}
			}
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException ignored) {}
			}
		}

		return result.toString();
	}

	public static String urlPost(String urlStr, StringBuffer params, int retry) {
		String flag = FAIL;
		int c = retry;
		for (int i = 0; i < c; i++) {
			flag = httpPostReq(urlStr, params);
			if (flag.startsWith(SUCCESS)) {
				flag = flag.substring(3);
				break;
			}
		}
		return flag;
	}

	public static String httpPostReq(String urlStr, StringBuffer params) {
		StringBuilder result = new StringBuilder();
		try {
			URL url = new URL(urlStr);
			if ("https".equalsIgnoreCase(url.getProtocol())) {
				SslUtils.ignoreSsl();
			}
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("User-agent", "Mozilla/4.0");
			conn.setRequestProperty("Accept-Language", "zh-cn,zh;q=0.5");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setUseCaches(false);

			if (params != null) {
				conn.getOutputStream().write(params.toString().getBytes());
				conn.getOutputStream().flush();
				conn.getOutputStream().close();
			}

			BufferedReader br = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			result = new StringBuilder(String.valueOf(conn.getResponseCode())); // suc=200
			if (conn.getResponseCode() == 200) {
				String line;
				for (line = br.readLine(); line != null; line = br.readLine()) {
					result.append(line);
				}
			}
		} catch (MalformedURLException e) {
			System.out.println("!!!Http请求URL报错!" + urlStr+ "参数 :"+JSON.toJSONString(params));
		} catch (IOException e) {
			System.out.println("!!!Http请求IO报错!" + urlStr+ "参数 :"+JSON.toJSONString(params));
		} catch (Exception e) {
			System.out.println("!!!Http请求报错!" + urlStr+ "参数 :"+JSON.toJSONString(params));
		}

		return result.toString();
	}
}

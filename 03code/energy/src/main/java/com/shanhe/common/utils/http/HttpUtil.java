package com.shanhe.common.utils.http;

import com.alibaba.fastjson2.JSON;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class HttpUtil {

	/** HTTP请求成功状态码。 */
	public final static String SUCCESS = "200";
	/** HTTP请求失败状态码。 */
	public final static String FAIL = "404";
	/** HTTP连接超时时间（毫秒）。 */
	public final static int CONN_TIMEOUT = 2000;
	/** HTTP读取超时时间（毫秒）。 */
	public final static int READ_TIMEOUT = 2000;

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

	/** URL请求 */
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
			// 连接超时
			conn.setConnectTimeout(CONN_TIMEOUT);
			// 读操作超时
			conn.setReadTimeout(READ_TIMEOUT);
			conn.connect();
			// suc=200
			flag = new StringBuilder(String.valueOf(conn.getResponseCode()));

			InputStream is = conn.getInputStream();
			InputStreamReader in = new InputStreamReader(is, StandardCharsets.UTF_8);
			BufferedReader reader = new BufferedReader(in);

			String lines;
			while (StringUtils.isNotEmpty((lines = reader.readLine()))) {
				flag.append(lines);
			}
		} catch (SocketTimeoutException s) {
            log.error("!!!连接或读操作超时!{}", urlStr);
		} catch (UnknownHostException unknown) {
            log.error("!!!无效的URL地址!{}", urlStr);
		} catch (FileNotFoundException f) {
            log.error("!!!数据流异常!{}", urlStr);
		} catch (Exception e) {
            log.error("!!!Http请求报错!{}：{}", urlStr, e.getMessage());
		}

		return flag.toString();
	}

	/** HTTP POST */
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

	/** HTTP POST File请求 */
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
			// 连接超时
			conn.setConnectTimeout(CONN_TIMEOUT);
			// 读操作超时
			conn.setReadTimeout(READ_TIMEOUT);
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
			// suc=200
			result = new StringBuilder(String.valueOf(conn.getResponseCode()));
			if (conn.getResponseCode() == 200) {
				String line;
				for (line = reader.readLine(); line != null; line = reader.readLine()) {
					result.append(line);
				}
			}
		} catch (MalformedURLException e) {
            log.error("!!!Http请求URL报错!{}参数 :{}", urlStr, JSON.toJSONString(params));
		} catch (IOException e) {
            log.error("!!!Http请求IO报错!{}参数 :{}", urlStr, JSON.toJSONString(params));
		} catch (Exception e) {
            log.error("!!!Http请求报错!{}参数 :{}，错误：{}", urlStr, JSON.toJSONString(params), e.getMessage());
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
			// suc=200
			result = new StringBuilder(String.valueOf(conn.getResponseCode()));
			if (conn.getResponseCode() == 200) {
				String line;
				for (line = br.readLine(); line != null; line = br.readLine()) {
					result.append(line);
				}
			}
		} catch (MalformedURLException e) {
			log.warn("Http请求URL报错! {} 参数: {}", urlStr, JSON.toJSONString(params), e);
		} catch (IOException e) {
			log.warn("Http请求IO报错! {} 参数: {}", urlStr, JSON.toJSONString(params), e);
		} catch (Exception e) {
			log.warn("Http请求报错! {} 参数: {}", urlStr, JSON.toJSONString(params), e);
		}

		return result.toString();
	}
}

package com.shanhe.common.utils.http;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;

/**
 * Ssl工具类
 *
 * @author wjh
 * @since 2025/11/13
 */
public class SslUtils {
	private static void trustAllHttpsCertificates() throws Exception {
		TrustManager[] trustAllCerts = new TrustManager[1];
		TrustManager tm = new Mtm();
		trustAllCerts[0] = tm;
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, null);
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	}

	static class Mtm implements TrustManager, X509TrustManager {
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		public boolean isServerTrusted(X509Certificate[] certs) {
			return true;
		}

		public boolean isClientTrusted(X509Certificate[] certs) {
			return true;
		}
		@Override
		public void checkServerTrusted(X509Certificate[] certs, String authType) { }
		@Override
		public void checkClientTrusted(X509Certificate[] certs, String authType) {}
	}

	/**
	 * 忽略HTTPS请求的SSL证书，必须在openConnection之前调用
	 */
	public static void ignoreSsl() throws Exception {
		HostnameVerifier hv = (urlHostName, session) -> {
			System.out.println("Warning: URL Host: " + urlHostName
					+ " vs. " + session.getPeerHost());
			return true;
		};
		trustAllHttpsCertificates();
		HttpsURLConnection.setDefaultHostnameVerifier(hv);
	}

}
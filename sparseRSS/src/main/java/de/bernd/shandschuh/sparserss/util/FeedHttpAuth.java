package de.bernd.shandschuh.sparserss.util;

import java.net.URL;
import java.nio.charset.StandardCharsets;

import de.bernd.shandschuh.sparserss.BASE64;

/**
 * HTTP Basic authentication helpers for feed refresh (pure Java, unit-testable).
 */
public final class FeedHttpAuth {

	private FeedHttpAuth() {
	}

	public static boolean hasStoredCredentials(String authUser, String authPassword) {
		return !isEmpty(authUser) || !isEmpty(authPassword);
	}

	public static boolean isHttpsUrl(String url) {
		return url != null && url.regionMatches(true, 0, "https://", 0, 8);
	}

	/**
	 * Prepends {@code http://} or {@code https://} when the user omitted a scheme.
	 */
	public static String normalizeFeedUrl(String url, String authUser, String authPassword) {
		if (url == null) {
			return null;
		}
		if (url.regionMatches(true, 0, "http://", 0, 7) || url.regionMatches(true, 0, "https://", 0, 8)) {
			return url;
		}
		if (hasStoredCredentials(authUser, authPassword)) {
			return "https://" + url;
		}
		return "http://" + url;
	}

	public static String basicAuthorizationValue(String user, String password) {
		String u = user != null ? user : "";
		String p = password != null ? password : "";
		return BASE64.encode((u + ':' + p).getBytes(StandardCharsets.UTF_8));
	}

	public static String basicAuthorizationValueFromUserInfo(String userInfo) {
		if (userInfo == null) {
			return null;
		}
		return BASE64.encode(userInfo.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Returns {@code Basic &lt;base64&gt;} or null if credentials must not be sent to {@code requestUrl}.
	 */
	public static String authorizationHeader(URL feedUrl, URL requestUrl, String authUser, String authPassword) {
		if (feedUrl == null || requestUrl == null || !hostsAndSchemeAllowCredentials(feedUrl, requestUrl)) {
			return null;
		}
		if (hasStoredCredentials(authUser, authPassword)) {
			return "Basic " + basicAuthorizationValue(authUser, authPassword);
		}
		String userInfo = requestUrl.getUserInfo();
		if (userInfo != null) {
			return "Basic " + basicAuthorizationValueFromUserInfo(userInfo);
		}
		return null;
	}

	static boolean hostsAndSchemeAllowCredentials(URL feedUrl, URL requestUrl) {
		if (!"https".equalsIgnoreCase(feedUrl.getProtocol()) || !"https".equalsIgnoreCase(requestUrl.getProtocol())) {
			return false;
		}
		String feedHost = feedUrl.getHost();
		String requestHost = requestUrl.getHost();
		if (feedHost == null || requestHost == null) {
			return false;
		}
		return feedHost.equalsIgnoreCase(requestHost);
	}

	private static boolean isEmpty(String s) {
		return s == null || s.length() == 0;
	}
}

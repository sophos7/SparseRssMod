package de.bernd.shandschuh.sparserss.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import de.bernd.shandschuh.sparserss.Strings;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Instrumented HTTP GET for RSS feed refresh ({@link com.datadog.android.okhttp.DatadogInterceptor} when RUM is on).
 */
public final class FeedHttpFetch {

	private static final String KEY_USERAGENT = "User-agent";
	private static final String VALUE_USERAGENT = "Mozilla/5.0";
	private static final String GZIP = "gzip";

	private static Proxy sProxy;

	private FeedHttpFetch() {
	}

	public static void setProxy(Proxy proxy) {
		sProxy = proxy;
	}

	public static FeedHttpResponse executeGet(String url, boolean imposeUserAgent, boolean followHttpHttpsRedirects,
			URL feedAuthUrl, String authUser, String authPassword) throws IOException {
		return executeGet(new URL(url), imposeUserAgent, followHttpHttpsRedirects, feedAuthUrl, authUser, authPassword);
	}

	public static FeedHttpResponse executeGet(URL url, boolean imposeUserAgent, boolean followHttpHttpsRedirects,
			URL feedAuthUrl, String authUser, String authPassword) throws IOException {
		OkHttpClient client = buildClient(followHttpHttpsRedirects);
		Request.Builder builder = new Request.Builder().url(url).get();
		if (imposeUserAgent) {
			builder.header(KEY_USERAGENT, VALUE_USERAGENT);
		}
		builder.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		String authorization = FeedHttpAuth.authorizationHeader(feedAuthUrl, url, authUser, authPassword);
		if (authorization != null) {
			builder.header("Authorization", authorization);
		}
		Response response = client.newCall(builder.build()).execute();
		if (!followHttpHttpsRedirects && isCrossProtocolRedirect(response, url)) {
			response.close();
			throw new IOException("https<->http redirect - enable in settings");
		}
		if (!response.isSuccessful() && response.code() != 304) {
			int code = response.code();
			response.close();
			if (code == 404) {
				throw new FileNotFoundException(url.toString());
			}
			throw new IOException("HTTP " + code);
		}
		return new FeedHttpResponse(response);
	}

	private static OkHttpClient buildClient(boolean followHttpHttpsRedirects) {
		OkHttpClient.Builder builder = new OkHttpClient.Builder()
				.connectTimeout(30, TimeUnit.SECONDS)
				.readTimeout(30, TimeUnit.SECONDS)
				.followRedirects(followHttpHttpsRedirects)
				.followSslRedirects(followHttpHttpsRedirects);
		if (sProxy != null) {
			builder.proxy(sProxy);
		}
		return Diagnostics.instrument(builder).build();
	}

	private static boolean isCrossProtocolRedirect(Response response, URL original) {
		if (response.code() < 300 || response.code() >= 400) {
			return false;
		}
		String location = response.header("Location");
		if (location == null) {
			return false;
		}
		return (original.getProtocol().equals(Strings._HTTP) && location.startsWith(Strings.HTTPS))
				|| (original.getProtocol().equals(Strings._HTTPS) && location.startsWith(Strings.HTTP));
	}

	public static final class FeedHttpResponse implements AutoCloseable {

		private final Response response;

		FeedHttpResponse(Response response) {
			this.response = response;
		}

		public URL getUrl() {
			return response.request().url().url();
		}

		public String getContentType() {
			return response.header("Content-Type");
		}

		public InputStream getBodyStream() throws IOException {
			ResponseBody body = response.body();
			if (body == null) {
				throw new IOException("Empty response body");
			}
			InputStream stream = body.byteStream();
			if (GZIP.equals(response.header("Content-Encoding")) && !(stream instanceof GZIPInputStream)) {
				return new GZIPInputStream(stream);
			}
			return stream;
		}

		@Override
		public void close() {
			response.close();
		}
	}
}

package de.bernd.shandschuh.sparserss.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class FeedHttpAuthTest {

	@Test
	public void basicAuthorizationValueUsesUtf8() {
		String value = FeedHttpAuth.basicAuthorizationValue("rss", "päss");
		String decoded = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
		assertEquals("rss:päss", decoded);
	}

	@Test
	public void normalizeFeedUrlDefaultsToHttpsWhenCredentialsSet() {
		assertEquals("https://feed.example/rss", FeedHttpAuth.normalizeFeedUrl("feed.example/rss", "u", "p"));
		assertEquals("http://feed.example/rss", FeedHttpAuth.normalizeFeedUrl("feed.example/rss", null, null));
	}

	@Test
	public void authorizationHeaderOmittedForDifferentHost() throws Exception {
		URL feed = new URL("https://cdn.example/feed.xml");
		URL other = new URL("https://other.example/feed.xml");
		assertNull(FeedHttpAuth.authorizationHeader(feed, other, "rss", "secret"));
	}

	@Test
	public void authorizationHeaderOmittedForHttpRequest() throws Exception {
		URL feed = new URL("https://cdn.example/feed.xml");
		URL http = new URL("http://cdn.example/feed.xml");
		assertNull(FeedHttpAuth.authorizationHeader(feed, http, "rss", "secret"));
	}

	@Test
	public void authorizationHeaderSentForMatchingHttpsHost() throws Exception {
		URL feed = new URL("https://cdn.example/feed.xml");
		URL same = new URL("https://cdn.example/other/path");
		assertEquals("Basic " + FeedHttpAuth.basicAuthorizationValue("rss", "secret"),
				FeedHttpAuth.authorizationHeader(feed, same, "rss", "secret"));
	}

	@Test
	public void storedCredentialsTakePrecedenceOverUrlUserInfo() throws Exception {
		URL feed = new URL("https://cdn.example/feed.xml");
		URL withUser = new URL("https://other:wrong@cdn.example/feed.xml");
		String header = FeedHttpAuth.authorizationHeader(feed, withUser, "rss", "stored");
		assertTrue(header.contains(FeedHttpAuth.basicAuthorizationValue("rss", "stored")));
		assertFalse(header.contains(FeedHttpAuth.basicAuthorizationValueFromUserInfo("other:wrong")));
	}
}

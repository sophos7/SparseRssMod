package de.bernd.shandschuh.sparserss.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FeedRefreshSelectionTest {

	@Test
	public void autoExclusionsOnly() {
		assertEquals("(noautorefresh IS NULL OR noautorefresh=0)",
				FeedRefreshSelection.build(null, true, false, true));
	}

	@Test
	public void wifiAndAutoExclusions() {
		assertEquals("(wifionly IS NULL OR wifionly=0) AND (noautorefresh IS NULL OR noautorefresh=0)",
				FeedRefreshSelection.build(null, false, false, true));
	}

	@Test
	public void singleFeedIgnoresFilters() {
		assertNull(FeedRefreshSelection.build("3", false, false, true));
	}
}

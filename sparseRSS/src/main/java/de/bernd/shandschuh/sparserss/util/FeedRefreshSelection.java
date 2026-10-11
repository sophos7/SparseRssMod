package de.bernd.shandschuh.sparserss.util;

import de.bernd.shandschuh.sparserss.provider.FeedData;

/**
 * SQL {@code WHERE} fragments for {@link de.bernd.shandschuh.sparserss.service.RssJobService} feed queries.
 */
public final class FeedRefreshSelection {

	private FeedRefreshSelection() {
	}

	public static String build(String feedId, boolean onWifi, boolean overrideWifiOnly, boolean applyAutoRefreshExclusions) {
		if (feedId != null) {
			return null;
		}
		StringBuilder where = new StringBuilder();
		if (!overrideWifiOnly && !onWifi) {
			appendAnd(where, FeedData.FeedColumns.WIFIONLY + " IS NULL OR " + FeedData.FeedColumns.WIFIONLY + "=0");
		}
		if (applyAutoRefreshExclusions) {
			appendAnd(where, FeedData.FeedColumns.NOAUTOREFRESH + " IS NULL OR " + FeedData.FeedColumns.NOAUTOREFRESH + "=0");
		}
		return where.length() == 0 ? null : where.toString();
	}

	private static void appendAnd(StringBuilder where, String clause) {
		if (where.length() > 0) {
			where.append(" AND ");
		}
		where.append('(').append(clause).append(')');
	}
}

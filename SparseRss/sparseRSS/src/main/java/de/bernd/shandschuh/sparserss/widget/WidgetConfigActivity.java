/**
 * Sparse rss
 *
 * Copyright (c) 2010-2012 Stefan Handschuh
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package de.bernd.shandschuh.sparserss.widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import de.bernd.shandschuh.sparserss.R;
import de.bernd.shandschuh.sparserss.Util;
import de.bernd.shandschuh.sparserss.provider.FeedData;
import de.bernd.shandschuh.sparserss.util.PreferenceUtils;

public class WidgetConfigActivity extends AppCompatActivity {
	private int widgetId;

	private static final String NAMECOLUMN = new StringBuilder("ifnull(").append(FeedData.FeedColumns.NAME).append(',').append(FeedData.FeedColumns.URL).append(") as title").toString();

	public static final String ZERO = "0";

	@Override
	protected void onCreate(Bundle bundle) {
		Util.setTheme(this);
		super.onCreate(bundle);
		setResult(RESULT_CANCELED);

		Bundle extras = getIntent().getExtras();

		if (extras != null) {
			widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		}
		if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish();
			return;
		}
		setContentView(R.layout.widgetconfig);

		if (bundle == null) {
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.widget_preferences, new WidgetPreferencesFragment())
					.commitNow();
		}
		final WidgetPreferencesFragment fragment = (WidgetPreferencesFragment) getSupportFragmentManager().findFragmentById(R.id.widget_preferences);

		if (fragment.hasFeeds()) {
			findViewById(R.id.save_button).setOnClickListener(view -> {
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
				SharedPreferences.Editor preferences = prefs.edit();

				boolean hideRead = false;

				preferences.putBoolean(widgetId + ".hideread", hideRead);

				String feedIds = fragment.getSelectedFeedIds();
				String entryCount = fragment.getEntryCount();

				preferences.putString(widgetId + ".feeds", feedIds);
				preferences.putString(widgetId + ".entrycount", entryCount);

				int color = prefs.getInt("widget.background", SparseRSSAppWidgetProvider.STANDARD_BACKGROUND);

				preferences.putInt(widgetId + ".background", color);
				preferences.commit();

				SparseRSSAppWidgetProvider.updateAppWidget(this, widgetId, hideRead, entryCount, feedIds, color);
				setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId));
				finish();
			});
		} else {
			// no feeds found --> use all feeds, no dialog needed
			setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId));
		}
	}

	public static class WidgetPreferencesFragment extends PreferenceFragmentCompat {
		private boolean hasFeeds;

		@Override
		public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
			setPreferencesFromResource(R.xml.widgetpreferences, rootKey);

			PreferenceCategory feedsPreferenceCategory = findPreference("widget.visiblefeeds");

			try (Cursor cursor = requireContext().getContentResolver().query(FeedData.FeedColumns.CONTENT_URI, new String[] {FeedData.FeedColumns._ID, NAMECOLUMN}, null, null, null)) {
				if (cursor != null && cursor.moveToFirst()) {
					hasFeeds = true;

					CheckBoxPreference allFeeds = new CheckBoxPreference(requireContext());
					allFeeds.setTitle(R.string.all_feeds);
					allFeeds.setKey(ZERO);
					allFeeds.setDisableDependentsState(true);
					feedsPreferenceCategory.addPreference(allFeeds);

					for (; !cursor.isAfterLast(); cursor.moveToNext()) {
						CheckBoxPreference feed = new CheckBoxPreference(requireContext());
						feed.setTitle(cursor.getString(1));
						feed.setKey(Integer.toString(cursor.getInt(0)));
						feedsPreferenceCategory.addPreference(feed);
						feed.setDependency(ZERO);
					}
				}
			}
			PreferenceUtils.removeIconSpace(getPreferenceScreen());
		}

		@Override
		public void onDisplayPreferenceDialog(Preference preference) {
			if (preference instanceof ColorPickerDialogPreference) {
				ColorPickerPreferenceDialogFragment dialog = ColorPickerPreferenceDialogFragment.newInstance(preference.getKey());
				dialog.setTargetFragment(this, 0);
				dialog.show(getParentFragmentManager(), "colorpicker");
			} else {
				super.onDisplayPreferenceDialog(preference);
			}
		}

		boolean hasFeeds() {
			return hasFeeds;
		}

		String getEntryCount() {
			return ((ListPreference) findPreference("widget.entrycount")).getValue();
		}

		String getSelectedFeedIds() {
			PreferenceCategory feeds = findPreference("widget.visiblefeeds");
			StringBuilder builder = new StringBuilder();

			for (int n = 0, i = feeds.getPreferenceCount(); n < i; n++) {
				CheckBoxPreference preference = (CheckBoxPreference) feeds.getPreference(n);

				if (preference.isChecked()) {
					if (n == 0) {
						break;
					} else {
						if (builder.length() > 0) {
							builder.append(',');
						}
						builder.append(preference.getKey());
					}
				}
			}
			return builder.toString();
		}
	}

}

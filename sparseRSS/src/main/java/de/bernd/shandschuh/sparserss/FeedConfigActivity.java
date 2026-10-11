/**
 * Sparse rss
 *
 * Copyright (c) 2012 Stefan Handschuh
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

package de.bernd.shandschuh.sparserss;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import de.bernd.shandschuh.sparserss.R;
import de.bernd.shandschuh.sparserss.provider.FeedData;
import de.bernd.shandschuh.sparserss.util.FeedHttpAuth;

public class FeedConfigActivity extends Activity {
	private static final String WASACTIVE = "wasactive";
	public static final String[] PROJECTION = new String[] {FeedData.FeedColumns.NAME, FeedData.FeedColumns.URL, FeedData.FeedColumns.WIFIONLY, FeedData.FeedColumns.BEIMLADEN, FeedData.FeedColumns.SYNC, FeedData.FeedColumns.TOPFEED, FeedData.FeedColumns.AUTH_USER, FeedData.FeedColumns.AUTH_PASSWORD, FeedData.FeedColumns.NOAUTOREFRESH};
	
	private EditText nameEditText;
	
	private EditText urlEditText;
	
	private EditText authUserEditText;
	
	private EditText authPasswordEditText;
	
	private CheckBox refreshOnlyWifiCheckBox;
	private CheckBox noAutoRefreshCheckBox;
	private CheckBox syncCheckBox;
//	private CheckBox cardViewCheckBox;
	private CheckBox topfeedCheckBox;
	
	private Spinner spinner;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.feedsettings);
		setResult(RESULT_CANCELED);
		
		Intent intent = getIntent();
		
		nameEditText = (EditText) findViewById(R.id.feed_title);
		urlEditText = (EditText) findViewById(R.id.feed_url);
		authUserEditText = (EditText) findViewById(R.id.feed_auth_username);
		authPasswordEditText = (EditText) findViewById(R.id.feed_auth_password);
		refreshOnlyWifiCheckBox = (CheckBox) findViewById(R.id.wifionlycheckbox);
		noAutoRefreshCheckBox = (CheckBox) findViewById(R.id.noautorefreshcheckbox);
		syncCheckBox = (CheckBox) findViewById(R.id.synccheckbox);
//		cardViewCheckBox = (CheckBox) findViewById(R.id.cardviewcheckbox);
//		cardViewCheckBox.setChecked(Util.getTestListPrefs(getApplicationContext()));
		topfeedCheckBox = (CheckBox) findViewById(R.id.topfeedcheckbox);
		
		spinner = (Spinner) findViewById(R.id.spinner1);
		
	    String[] mStrings = {"Feed", "Browser", "Mobilize", "Instapaper", "Readability", "AMP", "GoogleWeblight", "Readability4J"};  // 0..7
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, mStrings);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
			
		if (intent.getAction().equals(Intent.ACTION_INSERT)) {
			
			ClipboardManager cm = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
			try {
				if (cm.getPrimaryClip().getItemCount() > 0) { // kann knallen...
					String clip = "" + cm.getPrimaryClip().getItemAt(0).getText();
					if (!"".equals(clip)) {
						urlEditText.setText(clip);
					}
				}
			} catch (Exception e) {
				System.err.println("FeedConfigActivity - no Clipboard");
			}
			
			setTitle(R.string.newfeed_title);
			restoreInstanceState(savedInstanceState);
			((Button) findViewById(R.id.button_ok)).setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					String authUser = authUserEditText.getText().toString();
					String authPassword = authPasswordEditText.getText().toString();
					String url = urlEditText.getText().toString();
					if (!validateCredentialsForUrl(url, authUser, authPassword)) {
						return;
					}
					url = FeedHttpAuth.normalizeFeedUrl(url, authUser, authPassword);
					
					Cursor cursor = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI, null, new StringBuilder(FeedData.FeedColumns.URL).append(Strings.DB_ARG).toString(), new String[] {url}, null);
					
					if (cursor.moveToFirst()) {
						cursor.close();
						Toast.makeText(FeedConfigActivity.this, R.string.error_feedurlexists, Toast.LENGTH_LONG).show();
					} else {
						cursor.close();
						ContentValues values = new ContentValues();
						
						values.put(FeedData.FeedColumns.WIFIONLY, refreshOnlyWifiCheckBox.isChecked() ? 1 : 0);
						values.put(FeedData.FeedColumns.NOAUTOREFRESH, noAutoRefreshCheckBox.isChecked() ? 1 : 0);
						values.put(FeedData.FeedColumns.SYNC, syncCheckBox.isChecked() ? 1 : 0);
						values.put(FeedData.FeedColumns.BEIMLADEN, spinner.getSelectedItemPosition() );
						values.put(FeedData.FeedColumns.TOPFEED, topfeedCheckBox.isChecked() ? 1 : 0);
						values.put(FeedData.FeedColumns.URL, url);
						values.put(FeedData.FeedColumns.ERROR, (String) null);
						putAuthColumns(values, authUser, authPassword);
						
						String name = nameEditText.getText().toString();
						
						if (name.trim().length() > 0) {
							values.put(FeedData.FeedColumns.NAME, name);
						}
						Uri insert = getContentResolver().insert(FeedData.FeedColumns.CONTENT_URI, values);
						
						Util.setViewerPrefs(FeedConfigActivity.this, insert.getLastPathSegment(), spinner.getSelectedItemPosition());

						setResult(RESULT_OK);
						finish();
					}
				}
			});
		} else {
			setTitle(R.string.editfeed_title);
			
			if (!restoreInstanceState(savedInstanceState)) {
				Uri uri = intent.getData();
				Cursor cursor = getContentResolver().query(uri, PROJECTION, null, null, null);
					
				if (cursor.moveToNext()) {
					nameEditText.setText(cursor.getString(0));
					urlEditText.setText(cursor.getString(1));
					refreshOnlyWifiCheckBox.setChecked(cursor.getInt(2) == 1);
			        spinner.setSelection(cursor.getInt(3));
					syncCheckBox.setChecked(cursor.getInt(4) == 1);
					topfeedCheckBox.setChecked(cursor.getInt(5) == 1);
					if (!cursor.isNull(6)) {
						authUserEditText.setText(cursor.getString(6));
					}
					if (!cursor.isNull(7)) {
						authPasswordEditText.setText(cursor.getString(7));
					}
					noAutoRefreshCheckBox.setChecked(!cursor.isNull(8) && cursor.getInt(8) == 1);
					cursor.close();
				} else {
					cursor.close();
					Toast.makeText(FeedConfigActivity.this, R.string.error, Toast.LENGTH_LONG).show();
					finish();
				}
				spinner.setSelection(Util.getViewerPrefs(FeedConfigActivity.this, uri.getLastPathSegment()));
			}
			((Button) findViewById(R.id.button_ok)).setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					String authUser = authUserEditText.getText().toString();
					String authPassword = authPasswordEditText.getText().toString();
					String url = urlEditText.getText().toString();
					if (!validateCredentialsForUrl(url, authUser, authPassword)) {
						return;
					}
					url = FeedHttpAuth.normalizeFeedUrl(url, authUser, authPassword);
					
					Cursor cursor = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI, new String[] {FeedData.FeedColumns._ID}, new StringBuilder(FeedData.FeedColumns.URL).append(Strings.DB_ARG).toString(), new String[] {url}, null);
					
					if (cursor.moveToFirst() && !getIntent().getData().getLastPathSegment().equals(cursor.getString(0))) {
						cursor.close();
						Toast.makeText(FeedConfigActivity.this, R.string.error_feedurlexists, Toast.LENGTH_LONG).show();
					} else {
						cursor.close();
						ContentValues values = new ContentValues();
						
						values.put(FeedData.FeedColumns.URL, url);
						
						String name = nameEditText.getText().toString();
						
						values.put(FeedData.FeedColumns.NAME, name.trim().length() > 0 ? name : null);
						values.put(FeedData.FeedColumns.FETCHMODE, 0);
						values.put(FeedData.FeedColumns.WIFIONLY, refreshOnlyWifiCheckBox.isChecked() ? 1 : 0);
						values.put(FeedData.FeedColumns.NOAUTOREFRESH, noAutoRefreshCheckBox.isChecked() ? 1 : 0);
						values.put(FeedData.FeedColumns.SYNC, syncCheckBox.isChecked() ? 1 : 0);
						values.put(FeedData.FeedColumns.TOPFEED, topfeedCheckBox.isChecked() ? 1 : 0);
						values.put(FeedData.FeedColumns.BEIMLADEN, spinner.getSelectedItemPosition() );
						values.put(FeedData.FeedColumns.ERROR, (String) null);
						putAuthColumns(values, authUser, authPassword);
						getContentResolver().update(getIntent().getData(), values, null, null);

						// getIntent().getData() content://de.bernd.shandschuh.sparserss.provider.FeedData/feeds/2
						// getIntent().getData().getLastPathSegment()   2, der Pk des Datensatzes
						Util.setViewerPrefs(FeedConfigActivity.this, getIntent().getData().getLastPathSegment(), spinner.getSelectedItemPosition());

						//listViewCheckBox erstmal ohne db ?ber alle Feeds
//						Util.setTestListPrefs(getApplicationContext(), cardViewCheckBox.isChecked());
						
						setResult(RESULT_OK);
						finish();
					}
				}
				
			});
			
		}
		
		((Button) findViewById(R.id.button_cancel)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	private boolean validateCredentialsForUrl(String url, String authUser, String authPassword) {
		if (!FeedHttpAuth.hasStoredCredentials(authUser, authPassword)) {
			return true;
		}
		String schemeCheckUrl = FeedHttpAuth.normalizeFeedUrl(url, authUser, authPassword);
		if (!FeedHttpAuth.isHttpsUrl(schemeCheckUrl)) {
			Toast.makeText(this, R.string.error_credentials_require_https, Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}
	
	private static void putAuthColumns(ContentValues values, String authUser, String authPassword) {
		values.put(FeedData.FeedColumns.AUTH_USER, authUser.trim().length() > 0 ? authUser : null);
		values.put(FeedData.FeedColumns.AUTH_PASSWORD, authPassword.length() > 0 ? authPassword : null);
	}
	
	private boolean restoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null && savedInstanceState.getBoolean(WASACTIVE, false)) {
			nameEditText.setText(savedInstanceState.getCharSequence(FeedData.FeedColumns.NAME));
			urlEditText.setText(savedInstanceState.getCharSequence(FeedData.FeedColumns.URL));
			authUserEditText.setText(savedInstanceState.getCharSequence(FeedData.FeedColumns.AUTH_USER));
			authPasswordEditText.setText(savedInstanceState.getCharSequence(FeedData.FeedColumns.AUTH_PASSWORD));
			refreshOnlyWifiCheckBox.setChecked(savedInstanceState.getBoolean(FeedData.FeedColumns.WIFIONLY));
			noAutoRefreshCheckBox.setChecked(savedInstanceState.getBoolean(FeedData.FeedColumns.NOAUTOREFRESH));
			spinner.setSelection(savedInstanceState.getInt(FeedData.FeedColumns.BEIMLADEN));
			syncCheckBox.setChecked(savedInstanceState.getBoolean(FeedData.FeedColumns.SYNC));
			topfeedCheckBox.setChecked(savedInstanceState.getBoolean(FeedData.FeedColumns.TOPFEED));
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(WASACTIVE, true);
		outState.putCharSequence(FeedData.FeedColumns.NAME, nameEditText.getText());
		outState.putCharSequence(FeedData.FeedColumns.URL, urlEditText.getText());
		outState.putCharSequence(FeedData.FeedColumns.AUTH_USER, authUserEditText.getText());
		outState.putCharSequence(FeedData.FeedColumns.AUTH_PASSWORD, authPasswordEditText.getText());
		outState.putBoolean(FeedData.FeedColumns.WIFIONLY, refreshOnlyWifiCheckBox.isChecked());
		outState.putBoolean(FeedData.FeedColumns.NOAUTOREFRESH, noAutoRefreshCheckBox.isChecked());
		outState.putInt(FeedData.FeedColumns.BEIMLADEN, spinner.getSelectedItemPosition());
		outState.putBoolean(FeedData.FeedColumns.SYNC, syncCheckBox.isChecked());
		outState.putBoolean(FeedData.FeedColumns.TOPFEED, topfeedCheckBox.isChecked());
	}

}

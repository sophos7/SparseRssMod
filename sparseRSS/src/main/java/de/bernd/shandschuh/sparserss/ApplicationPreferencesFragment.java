package de.bernd.shandschuh.sparserss;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.InputType;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.io.InputStream;
import java.io.OutputStream;

import de.bernd.shandschuh.sparserss.service.RssJobService;
import de.bernd.shandschuh.sparserss.util.Diagnostics;
import de.bernd.shandschuh.sparserss.util.PreferenceUtils;
import de.bernd.shandschuh.sparserss.util.SettingsBackup;

public class ApplicationPreferencesFragment extends PreferenceFragmentCompat {

	private static final String KEY_SETTINGS_IMPORT = "settings.import";
	private static final String KEY_SETTINGS_EXPORT = "settings.export";
	private static final String KEY_DEBUG_VIEW_LOG = "debug.view_log";
	private static final String KEY_DEBUG_SCHEDULED_JOBS = "debug.scheduled_jobs";

	private final ActivityResultLauncher<String> notificationPermissionLauncher =
			registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
			});

	private final ActivityResultLauncher<Intent> ringtoneLauncher =
			registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
				Intent data = result.getData();
				if (result.getResultCode() == Activity.RESULT_OK && data != null) {
					Uri ringtone = IntentCompat.getParcelableExtra(data, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri.class);
					PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
							.putString(Strings.SETTINGS_NOTIFICATIONSRINGTONE, ringtone == null ? Strings.EMPTY : ringtone.toString())
							.apply();
				}
			});

	private final ActivityResultLauncher<String[]> importSettingsLauncher =
			registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
				if (uri != null) {
					try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
						int count = SettingsBackup.importFrom(requireContext(), inputStream);
						Diagnostics.action("settings_import");
						Util.applyRefreshSettings(requireContext());
						Util.msgBox(requireContext(), getString(R.string.message_settingsimported, count));
						requireActivity().recreate();
					} catch (Exception e) {
						new AlertDialog.Builder(requireContext())
								.setMessage(R.string.error_settingsimport)
								.setTitle(R.string.error)
								.setPositiveButton(android.R.string.ok, null)
								.show();
					}
				}
			});

	private final ActivityResultLauncher<String> exportSettingsLauncher =
			registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
				if (uri != null) {
					try (OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri)) {
						SettingsBackup.export(requireContext(), outputStream);
						Diagnostics.action("settings_export");
						Util.msgBox(requireContext(),
								String.format(getString(R.string.message_exportedto), getDisplayName(uri)));
					} catch (Exception e) {
						new AlertDialog.Builder(requireContext())
								.setMessage(R.string.error_settingsexport)
								.setTitle(R.string.error)
								.setPositiveButton(android.R.string.ok, null)
								.show();
					}
				}
			});

	@Override
	public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
		setPreferencesFromResource(R.xml.preferences, rootKey);
		PreferenceUtils.removeIconSpace(getPreferenceScreen());

		findPreference(Strings.SETTINGS_LIGHTTHEME).setOnPreferenceChangeListener((preference, newValue) -> {
			Activity activity = requireActivity();
			RSSOverview.chooseColorDialog(activity, new Intent(activity, ApplicationPreferencesActivity.class));
			return true;
		});

		findPreference(Strings.SETTINGS_NOTIFICATIONSENABLED).setOnPreferenceChangeListener((preference, newValue) -> {
			if (Boolean.TRUE.equals(newValue)) {
				RssJobService.createNotificationChannel(requireContext());
			}
			if (Boolean.TRUE.equals(newValue) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
					&& ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
				notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
			}
			return true;
		});

		Preference ringtone = findPreference(Strings.SETTINGS_NOTIFICATIONSRINGTONE);
		Preference vibrate = findPreference(Strings.SETTINGS_NOTIFICATIONSVIBRATE);
		Preference channel = findPreference(Strings.SETTINGS_NOTIFICATIONSCHANNEL);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			ringtone.getParent().removePreference(ringtone);
			vibrate.getParent().removePreference(vibrate);
			channel.setOnPreferenceClickListener(preference -> {
				startActivity(new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
						.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName())
						.putExtra(Settings.EXTRA_CHANNEL_ID, RssJobService.CHANNEL_ID));
				return true;
			});
		} else {
			channel.getParent().removePreference(channel);
			ringtone.setOnPreferenceClickListener(preference -> {
				String current = PreferenceManager.getDefaultSharedPreferences(requireContext())
						.getString(Strings.SETTINGS_NOTIFICATIONSRINGTONE, null);
				Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
						.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
						.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
						.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
						.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
								current == null || current.isEmpty() ? null : Uri.parse(current));
				ringtoneLauncher.launch(intent);
				return true;
			});
		}

		findPreference(Strings.SETTINGS_EFFICIENTFEEDPARSING).setOnPreferenceChangeListener((preference, newValue) -> {
			if (Boolean.FALSE.equals(newValue)) {
				new AlertDialog.Builder(requireContext())
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setTitle(android.R.string.dialog_alert_title)
						.setMessage(R.string.warning_moretraffic)
						.setPositiveButton(android.R.string.ok, (dialog, which) -> ((CheckBoxPreference) preference).setChecked(false))
						.setNegativeButton(android.R.string.cancel, null)
						.show();
				return false;
			}
			return true;
		});

		EditTextPreference proxyPort = findPreference(Strings.SETTINGS_PROXYPORT);
		proxyPort.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));

		findPreference(KEY_SETTINGS_IMPORT).setOnPreferenceClickListener(preference -> {
			importSettingsLauncher.launch(new String[]{"*/*"});
			return true;
		});
		findPreference(KEY_SETTINGS_EXPORT).setOnPreferenceClickListener(preference -> {
			exportSettingsLauncher.launch("sparse_rss_settings_" + System.currentTimeMillis() + ".json");
			return true;
		});

		boolean debuggable = (requireContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
		Preference viewLog = findPreference(KEY_DEBUG_VIEW_LOG);
		Preference scheduledJobs = findPreference(KEY_DEBUG_SCHEDULED_JOBS);
		if (debuggable) {
			viewLog.setOnPreferenceClickListener(preference -> {
				startActivity(new Intent(requireContext(), SendLogActivity.class));
				return true;
			});
			scheduledJobs.setOnPreferenceClickListener(preference -> {
				Util.jobInfos(requireContext());
				return true;
			});
		} else {
			viewLog.getParent().removePreference(viewLog);
			scheduledJobs.getParent().removePreference(scheduledJobs);
		}
	}

	private String getDisplayName(Uri uri) {
		try (Cursor cursor = requireContext().getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				return cursor.getString(0);
			}
		}
		return uri.toString();
	}
}

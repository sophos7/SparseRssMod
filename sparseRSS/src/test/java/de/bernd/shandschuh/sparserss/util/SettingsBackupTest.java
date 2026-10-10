package de.bernd.shandschuh.sparserss.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.json.JSONException;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SettingsBackupTest {

	@Test
	public void roundTripsEveryPreferenceType() throws Exception {
		Map<String, Object> prefs = new HashMap<>();
		prefs.put("lighttheme", true);
		prefs.put("keeptime", "7");
		prefs.put("SETTINGS_COLOR_MODE", 2);

		Map<String, Object> restored = SettingsBackup.fromJson(SettingsBackup.toJson(prefs));

		assertEquals(prefs, restored);
	}

	@Test
	public void leavesOutDeviceSpecificConsentAndStateEntries() throws Exception {
		Map<String, Object> prefs = new HashMap<>();
		prefs.put("notifications.ringtone", "content://media/1");
		prefs.put("diagnostics.enabled", true);
		prefs.put("diagnostics.mask", false);
		prefs.put("lastscheduledrefresh", 1234567890123L);
		prefs.put("keeptime", "7");

		Map<String, Object> restored = SettingsBackup.fromJson(SettingsBackup.toJson(prefs));

		assertEquals(Collections.singletonMap("keeptime", "7"), restored);
	}

	@Test
	public void ignoresUnknownKeysInAHandEditedFile() throws Exception {
		String json = "{\"format\":\"sparserss-settings\",\"version\":1,\"settings\":{"
				+ "\"diagnostics.enabled\":{\"type\":\"boolean\",\"value\":true}}}";

		assertEquals(0, SettingsBackup.fromJson(json).size());
	}

	@Test
	public void rejectsFilesThatAreNotASettingsBackup() {
		assertThrows(JSONException.class, () -> SettingsBackup.fromJson("{\"hello\":1}"));
		assertThrows(JSONException.class, () -> SettingsBackup.fromJson("not json"));
	}

	@Test
	public void rejectsNewerFormatVersions() {
		String json = "{\"format\":\"sparserss-settings\",\"version\":99,\"settings\":{}}";

		assertThrows(JSONException.class, () -> SettingsBackup.fromJson(json));
	}
}

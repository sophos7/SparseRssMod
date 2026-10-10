package de.bernd.shandschuh.sparserss.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import org.json.JSONException;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class SettingsBackupTest {

	@Test
	public void roundTripsEveryPreferenceType() throws Exception {
		Map<String, Object> prefs = new HashMap<>();
		prefs.put("lighttheme", true);
		prefs.put("keeptime", "7");
		prefs.put("count", 3);
		prefs.put("stamp", 1234567890123L);
		prefs.put("ratio", 1.5f);

		Map<String, Object> restored = SettingsBackup.fromJson(SettingsBackup.toJson(prefs));

		assertEquals(prefs, restored);
	}

	@Test
	public void leavesOutDeviceSpecificAndConsentSettings() throws Exception {
		Map<String, Object> prefs = new HashMap<>();
		prefs.put("notifications.ringtone", "content://media/1");
		prefs.put("diagnostics.enabled", true);
		prefs.put("diagnostics.mask", false);
		prefs.put("keeptime", "7");

		Map<String, Object> restored = SettingsBackup.fromJson(SettingsBackup.toJson(prefs));

		assertEquals(1, restored.size());
		assertFalse(restored.containsKey("diagnostics.enabled"));
	}

	@Test
	public void ignoresExcludedKeysInAHandEditedFile() throws Exception {
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

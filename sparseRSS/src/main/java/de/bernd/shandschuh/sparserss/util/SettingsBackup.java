package de.bernd.shandschuh.sparserss.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import de.bernd.shandschuh.sparserss.Strings;
import de.bernd.shandschuh.sparserss.Util;

/** Exports and imports the app settings as a JSON file. Feeds are handled separately by OPML. */
public final class SettingsBackup {

	private static final String FORMAT = "sparserss-settings";
	private static final int VERSION = 1;

	private static final String KEY_FORMAT = "format";
	private static final String KEY_VERSION = "version";
	private static final String KEY_SETTINGS = "settings";
	private static final String KEY_TYPE = "type";
	private static final String KEY_VALUE = "value";

	private static final String TYPE_BOOLEAN = "boolean";
	private static final String TYPE_STRING = "string";
	private static final String TYPE_INT = "int";
	private static final String TYPE_LONG = "long";
	private static final String TYPE_FLOAT = "float";

	/**
	 * Only the options from the settings screen travel. Device specific values (ringtone), consent
	 * (diagnostics) and app state such as the last refresh time stay out.
	 */
	private static final Set<String> KEYS = new HashSet<>(Arrays.asList(
			Strings.SETTINGS_LIGHTTHEME,
			Util.SETTINGS_COLOR_MODE,
			Strings.SETTINGS_PRIORITIZE,
			Strings.SETTINGS_KEEPTIME,
			Strings.SETTINGS_DISABLEPICTURES,
			Util.SETTINGS_SHOW_BOTTOM_BAR,
			Strings.SETTINGS_REFRESHENABLED,
			Strings.SETTINGS_REFRESHINTERVAL,
			Strings.SETTINGS_REFRESHONPENENABLED,
			Strings.SETTINGS_OVERRIDEWIFIONLY,
			Strings.SETTINGS_ENCLOSUREWARNINGSENABLED,
			Strings.SETTINGS_NOTIFICATIONSENABLED,
			Strings.SETTINGS_NOTIFICATIONSVIBRATE,
			Strings.SETTINGS_STANDARDUSERAGENT,
			Strings.SETTINGS_HTTPHTTPSREDIRECTS,
			Strings.SETTINGS_EFFICIENTFEEDPARSING,
			Strings.SETTINGS_PROXYENABLED,
			Strings.SETTINGS_PROXYWIFIONLY,
			Strings.SETTINGS_PROXYHOST,
			Strings.SETTINGS_PROXYPORT,
			Strings.SETTINGS_PROXYTYPE));

	private SettingsBackup() {
	}

	public static void export(Context context, OutputStream out) throws IOException, JSONException {
		Map<String, ?> all = PreferenceManager.getDefaultSharedPreferences(context).getAll();
		out.write(toJson(all).getBytes(StandardCharsets.UTF_8));
	}

	/** Returns the number of imported settings. */
	public static int importFrom(Context context, InputStream in) throws IOException, JSONException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] chunk = new byte[4096];
		int read;
		while ((read = in.read(chunk)) > 0) {
			buffer.write(chunk, 0, read);
		}
		Map<String, Object> settings = fromJson(new String(buffer.toByteArray(), StandardCharsets.UTF_8));

		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		for (Map.Entry<String, Object> entry : settings.entrySet()) {
			Object value = entry.getValue();
			if (value instanceof Boolean) {
				editor.putBoolean(entry.getKey(), (Boolean) value);
			} else if (value instanceof String) {
				editor.putString(entry.getKey(), (String) value);
			} else if (value instanceof Integer) {
				editor.putInt(entry.getKey(), (Integer) value);
			} else if (value instanceof Long) {
				editor.putLong(entry.getKey(), (Long) value);
			} else if (value instanceof Float) {
				editor.putFloat(entry.getKey(), (Float) value);
			}
		}
		editor.apply();
		return settings.size();
	}

	static String toJson(Map<String, ?> all) throws JSONException {
		JSONObject settings = new JSONObject();
		for (Map.Entry<String, ?> entry : all.entrySet()) {
			if (!KEYS.contains(entry.getKey())) {
				continue;
			}
			JSONObject item = toItem(entry.getValue());
			if (item != null) {
				settings.put(entry.getKey(), item);
			}
		}
		return new JSONObject()
				.put(KEY_FORMAT, FORMAT)
				.put(KEY_VERSION, VERSION)
				.put(KEY_SETTINGS, settings)
				.toString(2);
	}

	static Map<String, Object> fromJson(String json) throws JSONException {
		JSONObject root = new JSONObject(json);
		if (!FORMAT.equals(root.optString(KEY_FORMAT)) || root.optInt(KEY_VERSION, 0) < 1 || root.optInt(KEY_VERSION) > VERSION) {
			throw new JSONException("Not a settings backup");
		}
		JSONObject settings = root.getJSONObject(KEY_SETTINGS);
		Map<String, Object> result = new HashMap<>();
		for (Iterator<String> keys = settings.keys(); keys.hasNext(); ) {
			String key = keys.next();
			if (!KEYS.contains(key)) {
				continue;
			}
			Object value = fromItem(settings.getJSONObject(key));
			if (value != null) {
				result.put(key, value);
			}
		}
		return result;
	}

	private static JSONObject toItem(Object value) throws JSONException {
		String type;
		if (value instanceof Boolean) {
			type = TYPE_BOOLEAN;
		} else if (value instanceof String) {
			type = TYPE_STRING;
		} else if (value instanceof Integer) {
			type = TYPE_INT;
		} else if (value instanceof Long) {
			type = TYPE_LONG;
		} else if (value instanceof Float) {
			type = TYPE_FLOAT;
		} else {
			return null;
		}
		return new JSONObject().put(KEY_TYPE, type).put(KEY_VALUE, value);
	}

	private static Object fromItem(JSONObject item) throws JSONException {
		switch (item.getString(KEY_TYPE)) {
			case TYPE_BOOLEAN:
				return item.getBoolean(KEY_VALUE);
			case TYPE_STRING:
				return item.getString(KEY_VALUE);
			case TYPE_INT:
				return item.getInt(KEY_VALUE);
			case TYPE_LONG:
				return item.getLong(KEY_VALUE);
			case TYPE_FLOAT:
				return (float) item.getDouble(KEY_VALUE);
			default:
				return null;
		}
	}
}

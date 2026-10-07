package de.bernd.shandschuh.sparserss.util;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

public final class PreferenceUtils {

	private PreferenceUtils() {
	}

	/** AndroidX preferences reserve space for an icon by default. */
	public static void removeIconSpace(Preference preference) {
		preference.setIconSpaceReserved(false);
		if (preference instanceof PreferenceGroup) {
			PreferenceGroup group = (PreferenceGroup) preference;
			for (int i = 0; i < group.getPreferenceCount(); i++) {
				removeIconSpace(group.getPreference(i));
			}
		}
	}
}

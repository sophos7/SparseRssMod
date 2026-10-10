package de.bernd.shandschuh.sparserss.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.datadog.android.Datadog;
import com.datadog.android.DatadogSite;
import com.datadog.android.core.configuration.Configuration;
import com.datadog.android.okhttp.DatadogInterceptor;
import com.datadog.android.privacy.TrackingConsent;
import com.datadog.android.rum.GlobalRumMonitor;
import com.datadog.android.rum.Rum;
import com.datadog.android.rum.RumActionType;
import com.datadog.android.rum.RumConfiguration;
import com.datadog.android.rum.RumErrorSource;
import com.datadog.android.rum.tracking.ActivityViewTrackingStrategy;
import com.datadog.android.sessionreplay.ImagePrivacy;
import com.datadog.android.sessionreplay.SessionReplay;
import com.datadog.android.sessionreplay.SessionReplayConfiguration;
import com.datadog.android.sessionreplay.TextAndInputPrivacy;
import com.datadog.android.sessionreplay.TouchPrivacy;
import com.datadog.android.sessionreplay.material.MaterialExtensionSupport;

import java.util.Collections;
import java.util.Map;

import okhttp3.OkHttpClient;

import de.bernd.shandschuh.sparserss.Strings;

/** Opt-in usage, crash and session replay reporting through Datadog RUM. */
public final class Diagnostics {

	private static final String CLIENT_TOKEN = "pubbc68ecfadfdc661a977284b97a8a476b";
	private static final String RUM_APPLICATION_ID = "d232b3fc-629c-4370-8820-b6937f6999b7";

	private static boolean sMasked;

	private Diagnostics() {
	}

	public static boolean isEnabled() {
		return Datadog.isInitialized();
	}

	/** Starts, restarts or stops reporting so it matches the stored settings. */
	public static synchronized void apply(Context context) {
		Context appContext = context.getApplicationContext();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
		boolean enabled = prefs.getBoolean(Strings.SETTINGS_DIAGNOSTICS_ENABLED, false);
		boolean masked = prefs.getBoolean(Strings.SETTINGS_DIAGNOSTICS_MASK, true);

		if (Datadog.isInitialized()) {
			if (enabled && masked == sMasked) {
				return;
			}
			Datadog.stopInstance();
		}
		if (enabled) {
			start(appContext, masked);
		}
	}

	private static void start(Context context, boolean masked) {
		boolean debuggable = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
		String variant = debuggable ? "debug" : "release";

		Configuration config = new Configuration.Builder(CLIENT_TOKEN, variant, variant)
				.useSite(DatadogSite.US1)
				.build();
		Datadog.initialize(context, config, TrackingConsent.GRANTED);

		Rum.enable(new RumConfiguration.Builder(RUM_APPLICATION_ID)
				.useViewTrackingStrategy(new ActivityViewTrackingStrategy(true))
				.trackUserInteractions()
				.trackLongTasks()
				.trackNonFatalAnrs(true)
				.build());

		SessionReplay.enable(new SessionReplayConfiguration.Builder(100f)
				.setTextAndInputPrivacy(masked ? TextAndInputPrivacy.MASK_ALL : TextAndInputPrivacy.MASK_SENSITIVE_INPUTS)
				.setImagePrivacy(masked ? ImagePrivacy.MASK_ALL : ImagePrivacy.MASK_NONE)
				.setTouchPrivacy(masked ? TouchPrivacy.HIDE : TouchPrivacy.SHOW)
				.addExtensionSupport(new MaterialExtensionSupport())
				.build());
		sMasked = masked;
	}

	/** Adds network tracking to the client when reporting is on. */
	public static OkHttpClient.Builder instrument(OkHttpClient.Builder builder) {
		if (Datadog.isInitialized()) {
			builder.addInterceptor(new DatadogInterceptor.Builder(Collections.emptyMap()).build());
		}
		return builder;
	}

	public static void action(String name) {
		action(name, Collections.<String, Object>emptyMap());
	}

	public static void action(String name, Map<String, Object> attributes) {
		if (Datadog.isInitialized()) {
			GlobalRumMonitor.get().addAction(RumActionType.CUSTOM, name, attributes);
		}
	}

	/** Reports a handled failure by name only, so feed addresses never leave the device. */
	public static void error(String message, Throwable cause) {
		if (Datadog.isInitialized()) {
			GlobalRumMonitor.get().addError(message + ": " + cause.getClass().getSimpleName(), RumErrorSource.SOURCE, null,
					Collections.<String, Object>emptyMap());
		}
	}
}

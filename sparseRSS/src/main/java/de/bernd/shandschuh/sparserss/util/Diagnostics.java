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

	private static String sAppliedConfig;

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
		String token = prefs.getString(Strings.SETTINGS_DIAGNOSTICS_TOKEN, "").trim();
		String applicationId = prefs.getString(Strings.SETTINGS_DIAGNOSTICS_APPLICATION_ID, "").trim();
		boolean custom = !token.isEmpty() && !applicationId.isEmpty();
		if (!custom) {
			token = CLIENT_TOKEN;
			applicationId = RUM_APPLICATION_ID;
		}
		DatadogSite site = custom
				? parseSite(prefs.getString(Strings.SETTINGS_DIAGNOSTICS_SITE, null))
				: DatadogSite.US1;
		String config = masked + "|" + token + "|" + applicationId + "|" + site.name();

		if (Datadog.isInitialized()) {
			if (enabled && config.equals(sAppliedConfig)) {
				return;
			}
			Datadog.stopInstance();
		}
		if (enabled) {
			start(appContext, masked, token, applicationId, site);
			sAppliedConfig = config;
		}
	}

	private static DatadogSite parseSite(String name) {
		try {
			return DatadogSite.valueOf(name);
		} catch (IllegalArgumentException | NullPointerException e) {
			return DatadogSite.US1;
		}
	}

	private static void start(Context context, boolean masked, String token, String applicationId, DatadogSite site) {
		boolean debuggable = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
		String variant = debuggable ? "debug" : "release";

		Configuration config = new Configuration.Builder(token, variant, variant)
				.useSite(site)
				.build();
		Datadog.initialize(context, config, TrackingConsent.GRANTED);

		Rum.enable(new RumConfiguration.Builder(applicationId)
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

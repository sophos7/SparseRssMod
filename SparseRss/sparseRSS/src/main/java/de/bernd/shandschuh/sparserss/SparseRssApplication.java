package de.bernd.shandschuh.sparserss;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class SparseRssApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
			registerActivityLifecycleCallbacks(new EdgeToEdgeInsets());
		}
	}

	/** Android 15+ draws apps edge-to-edge, so keep every screen's content clear of the system bars. */
	private static class EdgeToEdgeInsets extends SimpleActivityLifecycleCallbacks {
		@Override
		public void onActivityPostCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
			View content = activity.findViewById(android.R.id.content);
			ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
				Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
						| WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
				view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
				return WindowInsetsCompat.CONSUMED;
			});

			Window window = activity.getWindow();
			WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
			boolean light = Util.isLightTheme(activity);
			controller.setAppearanceLightStatusBars(light);
			controller.setAppearanceLightNavigationBars(light);
		}
	}
}

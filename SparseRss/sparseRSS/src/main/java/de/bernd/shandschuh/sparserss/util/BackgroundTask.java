package de.bernd.shandschuh.sparserss.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minimal replacement for the deprecated AsyncTask: tasks run one at a time on a
 * background thread and the result is delivered on the main thread.
 */
public abstract class BackgroundTask<Params, Result> {

	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
	private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

	private final AtomicBoolean cancelled = new AtomicBoolean(false);
	private volatile boolean running = false;

	protected void onPreExecute() {
	}

	protected abstract Result doInBackground(Params params);

	protected void onPostExecute(Result result) {
	}

	public final void cancel() {
		cancelled.set(true);
	}

	public final boolean isCancelled() {
		return cancelled.get();
	}

	public final boolean isRunning() {
		return running;
	}

	/** Must be called on the main thread. */
	public final BackgroundTask<Params, Result> execute(final Params params) {
		running = true;
		onPreExecute();
		EXECUTOR.execute(new Runnable() {
			@Override
			public void run() {
				Result result = null;
				try {
					result = doInBackground(params);
				} finally {
					final Result finalResult = result;
					MAIN_HANDLER.post(new Runnable() {
						@Override
						public void run() {
							running = false;
							if (!isCancelled()) {
								onPostExecute(finalResult);
							}
						}
					});
				}
			}
		});
		return this;
	}
}

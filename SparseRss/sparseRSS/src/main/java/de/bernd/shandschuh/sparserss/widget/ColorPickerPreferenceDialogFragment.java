/**
 * Sparse rss
 *
 * Copyright (c) 2010-2012 Stefan Handschuh
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

package de.bernd.shandschuh.sparserss.widget;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceDialogFragmentCompat;

import de.bernd.shandschuh.sparserss.R;

public class ColorPickerPreferenceDialogFragment extends PreferenceDialogFragmentCompat {
	private static final String STATE_COLOR = "color";

	private int color;

	public static ColorPickerPreferenceDialogFragment newInstance(String key) {
		ColorPickerPreferenceDialogFragment fragment = new ColorPickerPreferenceDialogFragment();
		Bundle args = new Bundle(1);
		args.putString(ARG_KEY, key);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			color = savedInstanceState.getInt(STATE_COLOR);
		} else {
			color = ((ColorPickerDialogPreference) getPreference()).getColor();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_COLOR, color);
	}

	@Override
	protected void onBindDialogView(@NonNull final View view) {
		super.onBindDialogView(view);

		view.setBackgroundColor(color);

		final SeekBar redSeekBar = view.findViewById(R.id.seekbar_red);
		final SeekBar greenSeekBar = view.findViewById(R.id.seekbar_green);
		final SeekBar blueSeekBar = view.findViewById(R.id.seekbar_blue);
		final SeekBar transparencySeekBar = view.findViewById(R.id.seekbar_transparency);

		transparencySeekBar.setProgress((Color.alpha(color) * 100) / 255);
		redSeekBar.setProgress((Color.red(color) * 100) / 255);
		greenSeekBar.setProgress((Color.green(color) * 100) / 255);
		blueSeekBar.setProgress((Color.blue(color) * 100) / 255);

		OnSeekBarChangeListener onSeekBarChangeListener = new OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				color = Color.argb(
						(transparencySeekBar.getProgress() * 255) / 100,
						(redSeekBar.getProgress() * 255) / 100,
						(greenSeekBar.getProgress() * 255) / 100,
						(blueSeekBar.getProgress() * 255) / 100);
				view.setBackgroundColor(color);
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		};

		redSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
		greenSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
		blueSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
		transparencySeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
	}

	@Override
	public void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			((ColorPickerDialogPreference) getPreference()).setColor(color);
		}
	}

}

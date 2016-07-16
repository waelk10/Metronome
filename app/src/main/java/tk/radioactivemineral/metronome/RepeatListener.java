/*
 * Copyright (c) 2016.
 * This file is part of Metronome.
 *
 *      Metronome is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      Metronome is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with Metronome.  If not, see <http://www.gnu.org/licenses/>.
 */

package tk.radioactivemineral.metronome;

import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;

/**
 * A class, that can be used as a TouchListener on any view (e.g. a Button).
 * It cyclically runs a clickListener, emulating keyboard-like behaviour. First
 * click is fired immediately, next one after the initialInterval, and subsequent
 * ones after the normalInterval.
 *
 * Interval is scheduled after the onClick completes, so it has to run fast.
 * If it runs slow, it does not generate skipped onClicks. Can be rewritten to
 * achieve this.
 */
public class RepeatListener implements OnTouchListener {

	private Handler handler = new Handler();

	private int initialInterval;
	private final int normalInterval;
	private final OnClickListener clickListener;

	private Runnable handlerRunnable = new Runnable() {
		@Override
		public void run() {
			handler.postDelayed(this, normalInterval);
			clickListener.onClick(downView);
		}
	};

	private View downView;

	/**
	 * @param initialInterval The interval after first click event
	 * @param normalInterval  The interval after second and subsequent click
	 *                        events
	 * @param clickListener   The OnClickListener, that will be called
	 *                        periodically
	 */
	public RepeatListener(int initialInterval, int normalInterval,
						  OnClickListener clickListener) {
		if (clickListener == null)
			throw new IllegalArgumentException("null runnable");
		if (initialInterval < 0 || normalInterval < 0)
			throw new IllegalArgumentException("negative interval");

		this.initialInterval = initialInterval;
		this.normalInterval = normalInterval;
		this.clickListener = clickListener;
	}

	public boolean onTouch(View view, MotionEvent motionEvent) {
		switch (motionEvent.getAction()) {
			case MotionEvent.ACTION_DOWN:
				handler.removeCallbacks(handlerRunnable);
				handler.postDelayed(handlerRunnable, initialInterval);
				downView = view;
				downView.setPressed(true);
				clickListener.onClick(view);
				return true;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				handler.removeCallbacks(handlerRunnable);
				downView.setPressed(false);
				downView = null;
				return true;
		}

		return false;
	}

}
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

/**
 * Utilities for easing the handling and calculation(s) required for implementing the "Tap Tempo" feature.
 */
public class TapTempoUtils {
	private int tapsNumber;
	private Long totalTapTime;

	public TapTempoUtils(int tapsNumber, Long totalTapTime) {
		this.tapsNumber = tapsNumber;
		this.totalTapTime = totalTapTime;
	}

	public int calculateBPM(){
		return (int)((60000 * this.tapsNumber)/this.totalTapTime);
	}
}

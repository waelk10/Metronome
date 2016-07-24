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

import java.util.Objects;

/**
 * This class is used for generating a pitch-chart, using this formula
 * Fn = F0 * 2^(n/N)
 * Where:
 * Fn -> Frequency of the current note/pitch
 * F0 -> Base frequency
 * n  -> Note number (ranging from 0 to N-1)
 * N  -> Number of subdivisions per octave
 */
public class PitchGenerator {
	private final static int TWO = 2;
	private final static double BASE_FREQ_A = 27.5;
	private final static int LENGTH = 88;
	private final static int NOTES_LOOP_OFFSET = 3;
	private final static int NOTES_IN_OCTAVE = 12; //number of notes in an octave in the western scale
	private final static double SCALE_N = 12; //because this is what is used in the Western Scale
	private final static double ROUNDING_FACTOR = 10000.0; //the frequencies will be rounded to the same number of decimal points as the number of digits in this value minus one (note that the value has to be of the format 10^x, where x is a positive INTEGER).
	private final static String INDICATOR = "-";
	private final static String[] BASE_SCALE = {"A", INDICATOR, "A#", "Bb", "B", "C", INDICATOR, "C#", "Db", "D", INDICATOR, "D#", "Eb", "E", "F", INDICATOR, "F#", "Gb", "G", INDICATOR, "G#", "Ab"};

	private double[] freqs;
	private String[] notes;

	public PitchGenerator() {
		this.freqs = new double[LENGTH];
		this.notes = new String[LENGTH];
	}

	public double[] getFreqs() {
		this.freqs[0] = BASE_FREQ_A;
		double ratio, power, roundedValue;
		for (double i = 1; i < LENGTH; i++) {
			ratio = i / SCALE_N;
			power = Math.pow(TWO, ratio);
			roundedValue = BASE_FREQ_A * power;
			roundedValue = Math.round(roundedValue * ROUNDING_FACTOR) / ROUNDING_FACTOR;
			this.freqs[(int) i] = roundedValue;
		}
		return this.freqs;
	}

	public String[] getNotes() {
		this.notes[0] = "A0";
		this.notes[1] = "A#0/Bb0";
		this.notes[2] = "B0";
		int j = 5;
		for (int i = NOTES_LOOP_OFFSET; i < LENGTH; i++) {
			if (j == BASE_SCALE.length) j = 0;
			//main logic
			if (Objects.equals(INDICATOR, BASE_SCALE[j])) {
				j++;
				this.notes[i] = BASE_SCALE[j] + (((i - NOTES_LOOP_OFFSET) / NOTES_IN_OCTAVE) + 1) + '/';
				j++;
				this.notes[i] = this.notes[i] + BASE_SCALE[j] + (((i - NOTES_LOOP_OFFSET) / NOTES_IN_OCTAVE) + 1);
				j++;
			} else {
				this.notes[i] = BASE_SCALE[j] + (((i - NOTES_LOOP_OFFSET) / NOTES_IN_OCTAVE) + 1);
				j++;
			}
		}
		return this.notes;
	}
}

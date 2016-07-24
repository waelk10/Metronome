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

public class Metronome {
	private double bpm;
	private int beat;
	private int silence;

	private double beatSound;
	private double sound;
	private final int tick = 1000; // samples of tick

	private boolean play = true;

	private AudioGenerator audioGenerator;

	public Metronome() {
		audioGenerator = new AudioGenerator(8000);
		audioGenerator.createPlayer();
	}
	public Metronome(AudioGenerator audioGenerator) {
		this.audioGenerator = audioGenerator;
		audioGenerator.createPlayer(audioGenerator.getAudioTrack());
	}

	public void calcSilence() {
		silence = (int) (((60 / bpm) * 8000) - tick);
	}

	public void play() {
		calcSilence();
		double[] tick =
				audioGenerator.getSineWave(this.tick, 8000, beatSound);
		double[] tock =
				audioGenerator.getSineWave(this.tick, 8000, sound);
		double silence = 0;
		double[] sound = new double[8000];
		int t = 0, s = 0, b = 0;
		do {
			for (int i = 0; i < sound.length && play; i++) {
				if (t < this.tick) {
					if (b == 0)
						sound[i] = tock[t];
					else
						sound[i] = tick[t];
					t++;
				} else {
					sound[i] = silence;
					s++;
					if (s >= this.silence) {
						t = 0;
						s = 0;
						b++;
						if (b > (this.beat - 1))
							b = 0;
					}
				}
			}
			audioGenerator.writeSound(sound);
		} while (play);
	}

	public void stop() {
		play = false;
		audioGenerator.destroyAudioTrack();
	}

	public double getBpm() {
		return bpm;
	}

	public void setBpm(double bpm) {
		this.bpm = bpm;
	}

	public int getBeat() {
		return beat;
	}

	public void setBeat(int beat) {
		this.beat = beat;
	}

	public double getBeatSound() {
		return beatSound;
	}

	public void setBeatSound(double beatSound) {
		this.beatSound = beatSound;
	}

	public double getSound() {
		return sound;
	}

	public void setSound(double sound) {
		this.sound = sound;
	}

	public AudioGenerator getAudioGenerator() {
		return audioGenerator;
	}

	//copy maker
	public Metronome copyMetronome() {
		if(!play)
			this.stop();
		Metronome metronomeCopy;
		metronomeCopy = new Metronome(this.getAudioGenerator());
		metronomeCopy.setSound(this.getSound());
		metronomeCopy.setBeatSound(this.getBeatSound());
		metronomeCopy.setBpm(this.getBpm());
		metronomeCopy.setBeat(this.getBeat());
		return metronomeCopy;
	}

	public Boolean playRes() {
		this.play();
		return Boolean.TRUE;
	}
}

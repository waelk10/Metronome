/*
 * Copyright (c) 2019.
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

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;

public class AudioGenerator {
	private int sampleRate;
	private AudioTrack audioTrack;

	public AudioGenerator(int sampleRate) {
		this.sampleRate = sampleRate;
	}

	public double[] getSineWave(int samples, int sampleRate, double frequencyOfTone) {
		double[] sample = new double[samples];
		for (int i = 0; i < samples; i++) {
			sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / frequencyOfTone));
		}
		return sample;
	}

	public double[] getSawtoothWave(int samples, int sampleRate, double frequencyOfTone) {
		double[] sample = new double[samples];
		for (int i = 0; i < samples; i++) {
			sample[i] = 2 * (i % (sampleRate / frequencyOfTone)) / (sampleRate / frequencyOfTone) - 1;
		}
		return sample;
	}

	public double[] getPWMWave(int samples, int sampleRate, double frequencyOfTone) {
		double[] sample = getSineWave(samples, sampleRate, frequencyOfTone);
		//turn the sine wave into a PWM wave
		for (int i = 0; i < sample.length; i++) {
			sample[i] = Math.round(sample[i]);
		}
		//return the modified sample
		return sample;
	}

	public double[] getThinPWMWave(int samples, int sampleRate, double frequencyOfTone, double thinness) {
		double[] sample = getPWMWave(samples, sampleRate, frequencyOfTone);
		//if thinness is not a fraction of 1, return square (as this is an error - gracefully fail)
		if (thinness <= 0 || thinness >= 1)
			return sample;
		//got a square wave, make a PWM one with change to the duty cycle
		//get min and max values so you can modulate the cycle
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		boolean flag_a = true;
		boolean flag_b = true;
		for (int i = 0; i < sample.length && (flag_a || flag_b); i++) {
			if (sample[i] > max) {
				max = sample[i];
				flag_a = false;
			} else if (sample[i] < min) {
				min = sample[i];
				flag_b = false;
			}
		}
		//assuming a uniform wave, modulate it
		for (int i = 0; i < sample.length - 1; i++) {
			if (sample[i] == max) {

				while (sample[i + 1] != max) {
					sample[i] = min;
					i++;
				}
			}
		}
		//return the modified sample
		return sample;
	}



	public byte[] get16BitPcm(double[] samples) {
		byte[] generatedSound = new byte[2 * samples.length];
		int index = 0;
		for (double sample : samples) {
			// scale to maximum amplitude
			short maxSample = (short) ((sample * Short.MAX_VALUE));
			// in 16 bit wav PCM, first byte is the low order byte
			generatedSound[index++] = (byte) (maxSample & 0x00ff);
			generatedSound[index++] = (byte) ((maxSample & 0xff00) >>> 8);
		}
		return generatedSound;
	}

	public void createPlayer() {
		//check API version and setup the AudioTrack object accordingly, anything under 26 uses first option.
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
			audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
				sampleRate, AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT, sampleRate,
				AudioTrack.MODE_STREAM);
		else {
			AudioAttributes.Builder audioAttribuitesBuilder = new AudioAttributes.Builder();
			audioAttribuitesBuilder.setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC);
			AudioFormat.Builder audioFormatBuilder = new AudioFormat.Builder();
			audioFormatBuilder.setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO);
			audioTrack = new AudioTrack.Builder().setAudioAttributes(audioAttribuitesBuilder.build()).setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY).setAudioFormat(audioFormatBuilder.build()).build();
		}
		audioTrack.play();
	}

	//used to avoid of an uninitialized state which lead to a crash!
	public void createPlayer(AudioTrack audioTrack) {
		this.audioTrack = audioTrack;
		if (this.audioTrack.getState() != AudioTrack.STATE_INITIALIZED)
			this.audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
					sampleRate, AudioFormat.CHANNEL_OUT_MONO,
					AudioFormat.ENCODING_PCM_16BIT, sampleRate,
					AudioTrack.MODE_STREAM);
		this.audioTrack.play();
	}

	public void writeSound(double[] samples) {
		byte[] generatedSnd = get16BitPcm(samples);
		audioTrack.write(generatedSnd, 0, generatedSnd.length);
	}

	public void destroyAudioTrack() {
		audioTrack.stop();
		audioTrack.release();
	}

	public AudioTrack getAudioTrack() {
		return audioTrack;
	}
}

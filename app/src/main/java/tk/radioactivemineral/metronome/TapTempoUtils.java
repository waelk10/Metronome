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

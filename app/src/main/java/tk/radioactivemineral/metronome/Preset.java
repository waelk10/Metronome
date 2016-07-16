package tk.radioactivemineral.metronome;

import com.orm.SugarRecord;

public class Preset extends SugarRecord {
	String title;
	int beats;
	int bpm;
	boolean autosave_flag;

	//used by SugarORM
	public Preset (){}

	public Preset(String title, int beats, int bpm, boolean autosaveFlag){
		this.title = title;
		this.beats = beats;
		this.bpm = bpm;
		this.autosave_flag = autosaveFlag;
	}

	//getters

	public String getTitle() {
		return title;
	}

	public int getBeats() {
		return beats;
	}

	public int getBpm() {
		return bpm;
	}

	public boolean isAutosaveFlag() {
		return autosave_flag;
	}
}

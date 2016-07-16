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

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

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * misc utils for use throughout the app
 *
 * Sidenote on the storage logic:
 * The values are stored like this:
 * 'name';'beats';'bpm';'autosave';'beat sound';'sound';'uuid';'wave'|
 * */

public class MiscUtils {
	private final static char letter_start_capital = 'A';
	private final static String TRUE = "TRUE";
	private final static String FALSE = "FALSE";
	private final static String READ_LOG_TAG = "MiscUtils_readSavedData";
	public final static int WAVE_INDEX = 7;
	private final static int AUTOSAVE_LENGTH = 10;
	public final static char newline = '|';
	public final static char separator = ';';
	public final static int BEATS_INDEX = 1;
	public final static int BPM_INDEX = 2;
	public final static int BEAT_SOUND_INDEX = 4;
	public final static int SOUND_INDEX = 5;
	public final static int UUID_INDEX = 6;
	private final static int FIELDS = 9;

	public static String prepareForStorage(String name, int beats, int bpm, boolean autosave, double beatSound, double sound, String uuid, String wave) {
		if (autosave)
			return name + separator + beats + separator + bpm + separator + TRUE + separator + beatSound + separator + sound + separator + uuid + separator + wave + newline;
		return name + separator + beats + separator + bpm + separator + FALSE + separator + beatSound + separator + sound + separator + uuid + separator + wave + newline;
	}

	//prepare the saved data for a list display
	public static String[][] parseSaveDataList(String data) {
		int i = 0, j = 0, count = 1;
		StringBuffer stringBuffer;
		List<String> stringList = new ArrayList<String>();
		String[][] results;
		while (i < data.length()) {
			if (count == 9)
				count = 1;
			stringBuffer = new StringBuffer();
			while (data.charAt(i) != separator && data.charAt(i) != newline && i < data.length()) {
				stringBuffer.append(data.charAt(i));
				i++;
			}
			if (count == 1 || count == 7)
				stringList.add(stringBuffer.toString());
			count++;
			j++;
			i++;
		}
		results = new String[(stringList.size() / 2)][2];
		j = 0;
		for (i = 0; i < results.length; i++) {
			for (int k = 0; k < 2; k++) {
				results[i][k] = stringList.get(j);
				j++;
			}
		}
		return results;
	}

	//parse the saved data
	public static String[] parseSaveData(String data) {
		String[] parsedData = new String[FIELDS];
		int i, index = 0;
		//prep the string array
		for (i = 0; i < parsedData.length; i++) {
			parsedData[i] = "";
		}
		i = 0;
		while (i < data.length()) {
			while (i < data.length() && data.charAt(i) != separator) {
				parsedData[index] = parsedData[index] + data.charAt(i);
				i++;
			}
			i++;
			index++;
		}
		return parsedData;
	}

	//parse the saved data array
	public static String[][] parseSaveDataArrays(String data) {
		int i = 0, j, index;
		String[] tmpData;
		List<String> stringList = new ArrayList<String>();
		String[][] results;
		while (i < data.length()) {
			index = newlineIndex(data, i);
			tmpData = parseSaveData(data.substring(i, index));
			for (j = 0; j < FIELDS; j++)
				stringList.add(tmpData[j]);
			i = index;
			i++;
		}
		results = new String[(stringList.size() / FIELDS)][FIELDS];
		j = 0;
		for (i = 0; i < results.length; i++) {
			for (int k = 0; k < FIELDS; k++) {
				results[i][k] = stringList.get(j);
				j++;
			}
		}
		return results;
	}

	//filter the autosaves
	public static String removeAutoSave(String data, Context context) {
		if (data == null)
			return null;
		int i;
		for (i = 0; i < data.length(); i++) {
			if (data.charAt(i) == letter_start_capital)
				if (i + AUTOSAVE_LENGTH < data.length()) {
					String subString = data.substring(i, i + AUTOSAVE_LENGTH - 1);
					if (subString.contentEquals(context.getResources().getText(R.string.autosave_name).toString()))
						data = data.replace(data.substring(i, newlineIndex(data, i) + 1), "");
				}
		}
		//cleanup the dual-separator glitch
		String double_newline = newline + "" + newline;
		if (data.contains(double_newline))
			data = data.replace(double_newline, newline + "");
		if (data.contentEquals(double_newline) || data.contentEquals(newline+""))
			if (data.length() <= 2)
				data = null;
			else
				data = data.substring(1, data.length() - 1);
		return data;
	}

	//return the (first encounter of the) autosave data
	public static String getAutoSave(String data, Context context) {
		String result = null;
		boolean found = false;
		int i = 0;
		while (i < data.length() && !found) {
			if (data.charAt(i) == letter_start_capital && i + AUTOSAVE_LENGTH < data.length() && data.substring(i, i + AUTOSAVE_LENGTH).equals(context.getResources().getText(R.string.autosave_name).toString())) {
				result = data.substring(i, newlineIndex(data, i));
				found = true;
			}
			i++;
		}
		return result;
	}

	//return the index of the newline at the end
	private static int newlineIndex(String data, int index) {
		while (index != data.length() && data.charAt(index) != newline)
			index++;
		if (index == data.length())
			index = -1;
		return index;
	}

	//return the index of the previous newline
	private static int prevNewLineIndex(String data, int index) {
		while (index >= 0 && index != data.length() && data.charAt(index) != newline)
			index--;
		if (index == 0 || index == -1) return 0;
		if (index == data.length())
			index++;
		return index;
	}

	//remove line with specific uuid
	public static String removeByUUID(String data, String uuid) {
		int i, checkLength;
		for (i = 0; i < data.length(); i++) {
			checkLength = i + uuid.length();
			if (checkLength < data.length())
				if (data.substring(i, checkLength).equals(uuid)) {
					int start = prevNewLineIndex(data, i);
					int end = newlineIndex(data, i) + 1;
					String subString = data.substring(start, end);
					data = data.replace(subString, "");
				}
		}
		return data;
	}

	//read the data from the file
	public static String readSavedData(Context context) {
		StringBuffer stringBuffer = new StringBuffer();
		if (!fileExists(context, SaveDialogActivity.DATA_STORAGE_FILE_NAME))
			return null;
		try {
			FileInputStream fileInputStream = context.openFileInput(SaveDialogActivity.DATA_STORAGE_FILE_NAME);
			InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			String readString = bufferedReader.readLine();
			while (readString != null) {
				stringBuffer.append(readString);
				readString = bufferedReader.readLine();
			}
			inputStreamReader.close();
		} catch (IOException e) {
			Log.w(READ_LOG_TAG, "failed to read!\n" + e.toString());
			return null;
		}
		return stringBuffer.toString();
	}

	public static boolean fileExists(Context context, String filename) {
		File file = context.getFileStreamPath(filename);
		return (file != null && file.exists());
	}
}

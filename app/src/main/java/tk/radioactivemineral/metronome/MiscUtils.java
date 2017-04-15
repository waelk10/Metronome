/*
 * Copyright (c) 2017.
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

import java.util.ArrayList;
import java.util.List;

/**
 * misc utils for use throughout the app
 */

public class MiscUtils {
	private final static char separator = ';';
	private final static char newline = '\n';
	private final static char letter_start_capital = 'A';
	private final static String TRUE = "TRUE";
	private final static String FALSE = "FALSE";
	private final static int AUTOSAVE_LENGTH = 8;
	private final static int FIELDS = 7;
	public final static int BEATS_INDEX = 2;
	public final static int BPM_INDEX = 3;
	public final static int BEAT_SOUND_INDEX = 5;
	public final static int SOUND_INDEX = 6;
	public final static int UUID_INDEX = 7;

	public static String prepareForStorage(String name, int beats, int bpm, boolean autosave, double beatSound, double sound, String uuid){
		if (autosave)
			return name + separator + beats + separator + bpm + separator + TRUE + separator + beatSound + separator + sound + separator + uuid + newline;
		return name + separator + beats + separator + bpm + separator + FALSE + separator + beatSound + separator + sound + separator + uuid + newline;
	}

	//prepare the saved data for a list display
	public static String[][] parseSaveDataList(String data){
		int i=0, j=0, count = 1;
		String tmpData;
		List<String> stringList = new ArrayList<String>();
		String[][] results;
		while(i<data.length()){
			if(count == 8)
				count = 1;
			tmpData = "";
			while(data.charAt(i)!=separator && data.charAt(i)!=newline){
				tmpData = tmpData + data.charAt(i);
				i++;
			}
			if(count == 1 || count == 7)
				stringList.add(tmpData);
			count++;
			j++;
			i++;
		}
		results = new String[(stringList.size()/2)][2];
		j=0;
		for (i = 0; i < results.length; i++) {
			for (int k = 0; k < 2; k++) {
				results[i][k] = stringList.get(j);
				j++;
			}
		}
		return results;
	}
	//parse the saved data
	public static String[] parseSaveData(String data){
		String[] parsedData = new String[FIELDS];
		int i, index = 0;
		//prep the string array
		for (i = 0; i < parsedData.length; i++) {
			parsedData[i] = "";
		}
		i=0;
		while(i<data.length()){
			while(i<data.length() && data.charAt(i)!=separator){
				parsedData[index] = parsedData[index] + data.charAt(i);
				i++;
			}
			i++;
			index++;
		}
		return parsedData;
	}
	//parse the saved data array
	public static String[][] parseSaveDataArrays(String data){
		int i=0, j, index;
		String[] tmpData;
		List<String> stringList = new ArrayList<String>();
		String[][] results;
		while(i<data.length()){
			index = newlineIndex(data, i);
			tmpData = parseSaveData(data.substring(i,index));
			for (j = 0; j < FIELDS; j++)
				stringList.add(tmpData[j]);
			i=index;
			i++;
		}
		results = new String[(stringList.size()/FIELDS)][FIELDS];
		j=0;
		for (i = 0; i < results.length; i++) {
			for (int k = 0; k < 7; k++) {
				results[i][k] = stringList.get(j);
				j++;
			}
		}
		return results;
	}

	//filter the autosaves
	public static String removeAutoSave(String data, Context context){
		int i;
		for (i=0; i<data.length(); i++){
			if(data.charAt(i)==letter_start_capital)
				if(i+AUTOSAVE_LENGTH < data.length())
					if(data.substring(i, i+AUTOSAVE_LENGTH).equals(context.getResources().getText(R.string.autosave_name).toString()))
						data = data.replace(data.substring(i,newlineIndex(data,i)),"");
		}
		return data;
	}

	//return the (first encounter of the) autosave data
	public static String getAutoSave(String data, Context context){
		String result = null;
		boolean found = false;
		int i=0;
		while (i<data.length() && !found){
			if(data.charAt(i)==letter_start_capital)
				if(i+AUTOSAVE_LENGTH < data.length())
					if(data.substring(i, i+AUTOSAVE_LENGTH).equals(context.getResources().getText(R.string.autosave_name).toString())) {
						result = data.substring(i, newlineIndex(data, i));
						found = true;
					}
		}
		return result;
	}

	//return the index of the newline at the end
	private static int newlineIndex(String data, int index){
		while(index != data.length() && data.charAt(index) != newline)
			index++;
		if (index == data.length())
			index--;
		return  index;
	}

	//return the index of the previous newline
	private static int prevNewLineIndex(String data, int index){
		while(index != data.length() && data.charAt(index) != newline)
			index--;
		if(index == 0 || index == -1) return 0;
		if (index == data.length())
			index++;
		return  index;
	}

	//remove line with specific uuid
	public static String removeByUUID(String data, String uuid){
		int i, checkLength;
		for (i=0; i<data.length(); i++){
			checkLength = i + uuid.length();
			if(checkLength < data.length())
				if(data.substring(i, checkLength).equals(uuid)){
					data = data.replace(data.substring(prevNewLineIndex(data, i), newlineIndex(data,i)+1),"");
				}
		}
		return null;
	}
}

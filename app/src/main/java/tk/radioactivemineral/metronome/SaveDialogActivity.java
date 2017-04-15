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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;

public class SaveDialogActivity extends Activity {

	public final static String DATA_STORAGE_FILE_NAME = "presets";
	private final static String LOG_TAG = "SaveDialogActivity";

	EditText editText;
	Button buttonOK;
	Boolean success;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//remove the title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_save_dialog);
		//fill the screen on the horizontal access
		//Grab the window of the dialog, and change the width
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
		Window window = getWindow();
		lp.copyFrom(window.getAttributes());
//This makes the dialog take up the full width
		lp.width = WindowManager.LayoutParams.MATCH_PARENT;
		lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
		window.setAttributes(lp);
		//init
		final int beats = getIntent().getExtras().getInt(MetronomeActivity.DIALOG_SAVE_BEATS);
		final int bpm = getIntent().getExtras().getInt(MetronomeActivity.DIALOG_SAVE_BPM);
		final double beatSound = getIntent().getExtras().getDouble(MetronomeActivity.DIALOG_SAVE_BEAT_SOUND);
		final double sound = getIntent().getExtras().getDouble(MetronomeActivity.DIALOG_SAVE_SOUND);
		final String uniqueID = getIntent().getExtras().getString(MetronomeActivity.DIALOG_SAVE_ID);
		editText = (EditText) findViewById(R.id.editTextDialog);
		buttonOK = (Button) findViewById(R.id.buttonOK);

		//assume write will work
		success = true;

		//user clicked save, do so.
		buttonOK.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//check for a valid name
				if (!editText.getText().toString().equals("")) {
					FileOutputStream fileOutputStream;
					String data = MiscUtils.prepareForStorage(editText.getText().toString(), beats, bpm, MetronomeActivity.AUTO_SAVE_FLAG_FALSE, beatSound, sound, uniqueID);
					try{
						//save the data
						fileOutputStream = openFileOutput(DATA_STORAGE_FILE_NAME, Context.MODE_PRIVATE);
						fileOutputStream.write(data.getBytes());
						fileOutputStream.close();
						} catch (IOException ex){
							Log.w(LOG_TAG, "failed to save!\n" + ex.toString());
							Toast.makeText(SaveDialogActivity.this, getResources().getText(R.string.save_fail_toast), Toast.LENGTH_SHORT).show();
							//failed to save!!
							success = false;
						}


						//set the flag of a successful save
						SharedPreferences preferences = getSharedPreferences(MetronomeActivity.PREFS_NAME, 0);
						SharedPreferences.Editor editor = preferences.edit();
						editor.putBoolean(MetronomeActivity.DB_SAVE_EXISTS, true);
						//commit
						editor.apply();
						//set the result
						Intent resultIntent = new Intent();
						setResult(Activity.RESULT_OK, resultIntent);

					//finish the activity
					finish();
				} else
					Toast.makeText(SaveDialogActivity.this, "Empty name!", Toast.LENGTH_SHORT).show();
			}
		});
	}
}

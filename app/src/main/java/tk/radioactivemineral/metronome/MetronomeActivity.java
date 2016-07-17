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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.binaryfork.spanny.Spanny;
import com.nanotasks.BackgroundWork;
import com.nanotasks.Completion;
import com.nanotasks.Tasks;
import com.orm.query.Select;

import java.util.List;
import java.util.Locale;

/**
 * Useful links
 * http://satyan.github.io/sugar/index.html - SugarORM
 * https://github.com/fabiendevos/nanotasks - Nano Tasks
 * http://developer.android.com/training/scheduling/wakelock.html - Keeping the screen on
 * http://masterex.github.io/archive/2012/05/28/android-audio-synthesis.html - Metronome base code
 * http://developer.android.com/reference/android/app/Activity.html#onPause%28%29 - docs, onPause()
 * http://satyan.github.io/sugar/getting-started.html - SugarORM general
 * http://satyan.github.io/sugar/query.html - SugarORM query
 */

public class MetronomeActivity extends Activity {
	public final static int REQUEST_ID = 1;
	public final static String PREFS_NAME = "DbPrefsFile";
	public final static String DB_SAVE_EXISTS = "DB_EXISTS";
	public final static String DIALOG_SAVE_ID = "INTENT_ID_DATA";
	public final static String DIALOG_SAVE_BPM = "INTENT_BPM_DATA";
	public final static String DIALOG_SAVE_BEATS = "INTENT_BEATS_DATA";
	public final static boolean AUTO_SAVE_FLAG_FALSE = false;
	private final static String TAG = "MetronomeActivity";
	private final static String AUTO_SAVE = "AUTO_SAVE";
	private final static int BPM_INDEX = 0;
	private final static int BEATS_INDEX = 1;
	private final static int INIT_INTERVAL = 400;
	private final static int NORMAL_INTERVAL = 100;
	private final static double SOUND = 10000;
	private final static double BEAT_SOUND = 1000;
	private final static boolean AUTO_SAVE_FLAG_TRUE = true;

	TextView textViewBPM;
	TextView textViewBeats;

	Button startButton;
	Button bpmPlusOneButton;
	Button bpmPlusTenButton;
	Button bpmMinusOneButton;
	Button bpmMinusTenButton;
	Button beatsPlusOneButton;
	Button beatsMinusOneButton;
	Button beatsTapButton;
	Button beatsTapStopButton;
	Button deleteButton;
	Button saveButton;
	Button restoreButton;

	ToggleButton toggleButton;

	/*TapBarMenu tapBarMenu;*/

	/*ImageView imageViewSave;
	ImageView imageViewDelete;
	ImageView imageViewRestore;*/

	Metronome metronome;
	Metronome currentMetronome;

	Boolean flag;

	Long id;
	Long currentTime;
	Long oldTime;
	Long timeDeltaSum;
	Long totalTime;

	Context contextActivity;

	int bpm;
	int beats;
	int taps;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_metronome);

		//find view elements
		//TextViews
		textViewBPM = (TextView) findViewById(R.id.textViewBPM);
		textViewBeats = (TextView) findViewById(R.id.textViewBeats);
		//Buttons
		startButton = (Button) findViewById(R.id.buttonStart);
		bpmPlusOneButton = (Button) findViewById(R.id.buttonPlusOne);
		bpmPlusTenButton = (Button) findViewById(R.id.buttonPlusTen);
		bpmMinusOneButton = (Button) findViewById(R.id.buttonMinusOne);
		bpmMinusTenButton = (Button) findViewById(R.id.buttonMinusTen);
		beatsPlusOneButton = (Button) findViewById(R.id.buttonBeatPlusOne);
		beatsMinusOneButton = (Button) findViewById(R.id.buttonBeatMinusOne);
		deleteButton = (Button) findViewById(R.id.button_delete);
		saveButton = (Button) findViewById(R.id.button_save);
		restoreButton = (Button) findViewById(R.id.button_restore);
		//ToggleButton
		toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
		//Tap buttons
		beatsTapButton = (Button) findViewById(R.id.buttonTap);
		//Tap start button
		beatsTapStopButton = (Button) findViewById(R.id.buttonStopTap);
		//TapBarMenu
		/*tapBarMenu = (TapBarMenu) findViewById(R.id.tapBarMenu);*/
		//ImageViews
		/*imageViewDelete = (ImageView) findViewById(R.id.bar_image_delete);
		imageViewRestore = (ImageView) findViewById(R.id.bar_image_restore);
		imageViewSave = (ImageView) findViewById(R.id.bar_image_save);*/

		//set context and flag
		contextActivity = this;
		flag = false;

		//initialize the id
		id = -1L;
		currentTime = -1L;
		timeDeltaSum = 0L;
		oldTime = -1L;

		//initialize counters with default values
		bpm = 0;
		beats = 0;
		taps = -1;

		//restore previous autosave if applicable
		//check if saved or not
		SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
		boolean save = preferences.getBoolean(DB_SAVE_EXISTS, false);
		if (save) {
			List<Preset> presetList = Select.from(Preset.class).list();
			int[] valuesArray = getAutoSaveValues(presetList);
			if (valuesArray != null) {
				beats = valuesArray[BEATS_INDEX];
				bpm = valuesArray[BPM_INDEX];
			}
		}

		//initialize the TextViews
		textViewBPM.setText(String.format(Locale.US, "%d", bpm));
		textViewBeats.setText(String.format(Locale.US, "%d", beats));

		//initialize the metronome object
		metronome = new Metronome();
		metronome.setBeatSound(BEAT_SOUND);
		metronome.setSound(SOUND);
		metronome.setBpm(bpm);
		metronome.setBeat(beats);

		//copy
		currentMetronome = metronome.copyMetronome();

		//menu
	/*	tapBarMenu.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				tapBarMenu.toggle();
			}
		});*/

		//Bottom action button listeners
		deleteButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (id != -1L) {
					delete(id);
					Toast.makeText(contextActivity, getResources().getText(R.string.deleted), Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(contextActivity, getResources().getText(R.string.nothing_delete), Toast.LENGTH_SHORT).show();
			}
		});
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				saveDialog();
			}
		});
		restoreButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				restoreDialog();
			}
		});

	/*	//ImageView listeners
		imageViewDelete.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (id != -1L) {
					delete(id);
					Toast.makeText(contextActivity, getResources().getText(R.string.deleted), Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(contextActivity, getResources().getText(R.string.nothing_delete), Toast.LENGTH_SHORT).show();
			}
		});
		imageViewSave.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				saveDialog();
			}
		});
		imageViewRestore.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				restoreDialog();
			}
		});*/

		//start/stop
		startButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//sanity check for the values
				if (bpm > 0 && beats > 0)
					if (!flag) {
						//execute the metronome with current settings asynchronously
						Tasks.executeInBackground(contextActivity, new BackgroundWork<Boolean>() {
							@Override
							public Boolean doInBackground() throws Exception {
								return currentMetronome.playRes();
							}
						}, new Completion<Boolean>() {
							@Override
							public void onSuccess(Context context, Boolean result) {
								Log.i(TAG, "Gracefully terminated background metronome task.");
							}

							@Override
							public void onError(Context context, Exception e) {
								Log.e(TAG, "Error in background metronome task, exception:");
								Log.e(TAG, e.toString());
							}
						});
						flag = true;
					} else {
						//stop the metronome
						currentMetronome.stop();
						currentMetronome = null;
						currentMetronome = metronome.copyMetronome();
						flag = false;
					}
				else
					Toast.makeText(contextActivity, getResources().getText(R.string.values_set), Toast.LENGTH_SHORT).show();
			}
		});

		//number of the bea(s)ts per minute buttons
		bpmPlusOneButton.setOnTouchListener(new RepeatListener(INIT_INTERVAL, NORMAL_INTERVAL, bpmClickListener));
		bpmMinusOneButton.setOnTouchListener(new RepeatListener(INIT_INTERVAL, NORMAL_INTERVAL, bpmClickListener));
		bpmPlusTenButton.setOnTouchListener(new RepeatListener(INIT_INTERVAL, NORMAL_INTERVAL, bpmClickListener));
		bpmMinusTenButton.setOnTouchListener(new RepeatListener(INIT_INTERVAL, NORMAL_INTERVAL, bpmClickListener));

		//number of the bea(s)ts buttons
		beatsPlusOneButton.setOnTouchListener(new RepeatListener(INIT_INTERVAL, NORMAL_INTERVAL, beatsClickListener));
		beatsMinusOneButton.setOnTouchListener(new RepeatListener(INIT_INTERVAL, NORMAL_INTERVAL, beatsClickListener));

		//tap buttons listeners
		beatsTapButton.setOnClickListener(tapClickListener);

		//start after tap button listener
		beatsTapStopButton.setOnClickListener(stopTapClickListener);

		//toggle button, keep screen on
		toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked)
					getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				else
					getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			}
		});

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case REQUEST_ID:
				if (resultCode == Activity.RESULT_OK)
					id = data.getLongExtra(DIALOG_SAVE_ID, -1L);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		//autosave
		//delete previous entry
		List<Preset> presetList = Select.from(Preset.class).list();
		Preset preset = getAutoSave(presetList);
		if (preset != null)
			preset.delete();
		//save new entry
		preset = new Preset(AUTO_SAVE, beats, bpm, AUTO_SAVE_FLAG_TRUE);
		id = preset.save();
		//set the flag of a successful save
		SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean(DB_SAVE_EXISTS, true);
		//commit
		editor.apply();
	}

	//overflow menu setup
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.activity_metronome_menu, menu);
		return true;
	}

	//overflow menu logic
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_about:
				aboutDialog();
				return true;
			/*Deprecated
			case R.id.action_save:
				saveDialog();
				return true;
			case R.id.action_restore:
				restoreDialog();
				return true;
			case R.id.action_delete:
				if (id != -1L) {
					delete(id);
					Toast.makeText(contextActivity, getResources().getText(R.string.deleted), Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(contextActivity, getResources().getText(R.string.nothing_delete), Toast.LENGTH_SHORT).show();
				return true;*/
		}
		return super.onOptionsItemSelected(item);
	}

	//delete from db
	private void delete(Long ident) {
		Preset preset = Preset.findById(Preset.class, ident);
		preset.delete();
		//reset the id
		id = -1L;
	}

	//restore dialog
	private void restoreDialog() {
		//list of all the presets
		final List<Preset> presetList = Select.from(Preset.class).list();
		getPresets(presetList);
		if (presetList.size() != 0) {
			AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
			final String[] titles = getTitles(presetList);
			builder.setSingleChoiceItems(titles, 0, null)
					.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							//dismiss the dialog
							dialog.dismiss();
							//get selected entry/position/row
							if (((AlertDialog) dialog).getListView() != null) {
								int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
								//set the values, for the variables and the UI
								beats = 0;
								beats = presetList.get(selectedPosition).getBeats();
								bpm = presetList.get(selectedPosition).getBpm();
								id = presetList.get(selectedPosition).getId();
								textViewBPM.setText(String.format(Locale.US, "%d", bpm));
								textViewBeats.setText(String.format(Locale.US, "%d", beats));
							}
						}
					}).show();
		} else
			Toast.makeText(contextActivity, getResources().getText(R.string.no_saved_presets_toast), Toast.LENGTH_SHORT).show();

	}

	//save dialog
	private void saveDialog() {
		/**deprecated!!*/
	/*	LayoutInflater layoutInflater = (LayoutInflater) contextActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
		builder.setTitle(getString(R.string.save_preset));
		builder.setView(layoutInflater.inflate(R.layout.save_alert_dialog, null));
		builder.setNeutralButton(R.string.save, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//user clicked save, do so.
				EditText editText = (EditText) findViewById(R.id.editTextDialog);
				Preset preset = new Preset(editText.getText().toString(), beats, bpm, AUTO_SAVE_FLAG_FALSE);
				preset.save();
				//set the flag of a successful save
				SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
				SharedPreferences.Editor editor = preferences.edit();
				editor.putBoolean(DB_SAVE_EXISTS, true);
				//commit
				editor.apply();
			}
		});
		builder.show();*/

		//start the activity to do the dirty work for you
		Intent intent = new Intent(MetronomeActivity.this, SaveDialogActivity.class);
		intent.putExtra(DIALOG_SAVE_BPM, bpm);
		intent.putExtra(DIALOG_SAVE_BEATS, beats);
		startActivityForResult(intent, REQUEST_ID);
	}

	//about dialog
	private void aboutDialog() {
		//for version info extraction
		String version = "\n\nVersion Name: ";
		int versionCode = 0;
		try {
			PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			version += packageInfo.versionName;
			version += "\nVersion Code: " + packageInfo.versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			versionCode = -1;
		}
		if (versionCode == -1) {
			version = "";
		}
		//build the dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
		//build the text
		Spanny message = new Spanny(getString(R.string.app_name) + '\n', new UnderlineSpan()).append('\n' + getString(R.string.email)).append('\n' + getString(R.string.copyright)).append('\n'+version).append("\n\n\n\n\n" + getString(R.string.license));
		builder.setMessage(message).setTitle(getString(R.string.about));
		//add the icon
		builder.setIcon(getResources().getDrawable(R.mipmap.ic_launcher, getTheme()));
		//create the object
		AlertDialog dialog = builder.create();
		//build the layout
		final LinearLayout layout = new LinearLayout(this);
		ScrollView scrollView = new ScrollView(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		TextView textView = new TextView(this);
		textView.setText(message);
		scrollView.addView(textView);
		//layout.addView(scrollView);
		dialog.setView(layout);
		//display the dialog to the user
		dialog.show();
		//set the background color
		dialog.getWindow().setBackgroundDrawableResource(R.color.colorPrimaryDark);
	}

	//used for setting the number of the bea(s)ts per minute
	private View.OnClickListener bpmClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			//stop and reset the metronome on change of values
			currentMetronome.stop();
			currentMetronome = null;
			flag = false;
			//button logic
			switch (v.getId()) {
				case R.id.buttonPlusOne:
					bpm++;
					metronome.setBpm(bpm);
					textViewBPM.setText(String.format(Locale.US, "%d", bpm));
					break;
				case R.id.buttonMinusOne:
					//make sure not to go under zero
					if (bpm - 1 >= 0) {
						bpm--;
						metronome.setBpm(bpm);
						textViewBPM.setText(String.format(Locale.US, "%d", bpm));
					}
					break;
				case R.id.buttonPlusTen:
					bpm += 10;
					textViewBPM.setText(String.format(Locale.US, "%d", bpm));
					metronome.setBpm(bpm);
					break;
				case R.id.buttonMinusTen:
					//make sure not to go under zero
					if (bpm - 10 >= 0) {
						bpm -= 10;
						metronome.setBpm(bpm);
						textViewBPM.setText(String.format(Locale.US, "%d", bpm));
					}
					break;

			}
			//reset the metronome
			currentMetronome = metronome.copyMetronome();
			//reset the id
			id = -1L;
		}
	};

	//used for the tap tempo tap button
	private View.OnClickListener tapClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			//stop the metronome
			currentMetronome.stop();
			currentMetronome = null;
			currentMetronome = metronome.copyMetronome();
			flag = false;
			//check if initial tap
			if (taps == -1) {
				taps = 0;
				taps++;
				oldTime = System.currentTimeMillis();
				totalTime = 0L;
			} else {
				taps++;
				currentTime = System.currentTimeMillis();
				timeDeltaSum += (currentTime - oldTime);
				oldTime = currentTime;
				totalTime += currentTime;
			}
		}
	};
	private View.OnClickListener stopTapClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (taps == -1 || taps == 0 || totalTime == 0L) {
				Toast.makeText(MetronomeActivity.this, "Not enough taps, at least two needed!", Toast.LENGTH_SHORT).show();
				return;
			}
			//stop the metronome
			currentMetronome.stop();
			currentMetronome = null;
			currentMetronome = metronome.copyMetronome();
			flag = false;
			//bpm calculation
			TapTempoUtils tapTempoUtils = new TapTempoUtils(taps - 1, timeDeltaSum);
			bpm = tapTempoUtils.calculateBPM();
			if (!flag) {
				//stop the metronome
				currentMetronome.stop();
				currentMetronome = null;
				currentMetronome = metronome.copyMetronome();
				flag = false;
			}
			currentMetronome.setBpm(bpm);
			textViewBPM.setText(String.format(Locale.US, "%d", bpm));
			taps = -1;
			oldTime = 0L;
			currentTime = 0L;
			timeDeltaSum = 0L;
		}
	};

	//used for setting the number of beats
	private View.OnClickListener beatsClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			//stop and reset the metronome on change of values
			currentMetronome.stop();
			currentMetronome = null;
			flag = false;
			//button logic
			switch (v.getId()) {
				case R.id.buttonBeatPlusOne:
					beats++;
					metronome.setBeat(beats);
					textViewBeats.setText(String.format(Locale.US, "%d", beats));
					break;
				case R.id.buttonBeatMinusOne:
					//make sure not to go under zero
					if (beats - 1 >= 0) {
						beats--;
						metronome.setBeat(beats);
						textViewBeats.setText(String.format(Locale.US, "%d", beats));
					}
					break;
			}
			//reset the metronome
			currentMetronome = metronome.copyMetronome();
			//reset the id
			id = -1L;
		}
	};

	//used to find and return the values of the autosave preset
	private int[] getAutoSaveValues(List<Preset> presetList) {
		Preset preset = getAutoSave(presetList);
		if (preset == null)
			return null;
		int[] values = new int[2];
		values[BEATS_INDEX] = preset.getBeats();
		values[BPM_INDEX] = preset.getBpm();
		return values;
	}

	//used to find and return the object "Preset" of the autosave preset
	private Preset getAutoSave(List<Preset> presetList) {
		if (presetList == null || presetList.isEmpty())
			return null;
		int i = 0;
		int length = presetList.size();
		while (i < length) {
			if (presetList.get(i).isAutosaveFlag()) {
				return presetList.get(i);
			} else
				i++;
		}
		return null;
	}

	//used to find and return the presets (excluding auto-save)
	private void getPresets(List<Preset> presetList) {
		if (presetList == null || presetList.isEmpty())
			return;
		int i = 0;
		int length = presetList.size();
		while (i < length) {
			if (presetList.get(i).isAutosaveFlag()) {
				presetList.remove(i);
				return;
			} else
				i++;
		}
	}

	//used to generate a String array of titles from the presets
	private String[] getTitles(List<Preset> presetList) {
		if (presetList == null || presetList.isEmpty())
			return null;
		int i = 0;
		int length = presetList.size();
		String[] titles = new String[presetList.size()];
		while (i < length) {
			titles[i] = presetList.get(i).getTitle() + " - " + presetList.get(i).getBpm() + "/" + presetList.get(i).getBeats();
			i++;
		}
		return titles;
	}
}

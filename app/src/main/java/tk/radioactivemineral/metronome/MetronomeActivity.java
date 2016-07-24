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

import java.util.Arrays;
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
	private final static int BPM_INDEX = 0;
	private final static int BEATS_INDEX = 1;
	private final static int BEAT_SOUND_INDEX = 2;
	private final static int SOUND_INDEX = 3;
	private final static int AUTO_SAVE_VALUES_ARRAY_LENGTH = 4;
	private final static int INIT_INTERVAL = 400;
	private final static int NORMAL_INTERVAL = 100;
	private final static double SOUND = 880;
	private final static double BEAT_SOUND = 440;
	private final static boolean AUTO_SAVE_FLAG_TRUE = true;
	public final static boolean AUTO_SAVE_FLAG_FALSE = false;
	public final static int REQUEST_ID = 1;
	public final static String PREFS_NAME = "DbPrefsFile";
	public final static String DB_SAVE_EXISTS = "DB_EXISTS";
	public final static String DIALOG_SAVE_ID = "INTENT_ID_DATA";
	public final static String DIALOG_SAVE_BPM = "INTENT_BPM_DATA";
	public final static String DIALOG_SAVE_BEATS = "INTENT_BEATS_DATA";
	public final static String DIALOG_SAVE_BEAT_SOUND = "INTENT_BEAT_SOUND_DATA";
	public final static String DIALOG_SAVE_SOUND = "INTENT_SOUND_DATA";
	private final static String TAG = "MetronomeActivity";
	private final static String AUTO_SAVE = "AUTO_SAVE";

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
	Button roundUpButton;
	Button roundDownButton;
	Button toneButton;

	ToggleButton toggleButton;

	Metronome metronome;
	Metronome currentMetronome;

	PitchGenerator pitchGenerator;

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

	double beatSound;
	double sound;

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
		roundUpButton = (Button) findViewById(R.id.buttonRoundUp);
		roundDownButton = (Button) findViewById(R.id.buttonRoundDown);
		toneButton = (Button) findViewById(R.id.button_tone);
		//ToggleButton
		toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
		//Tap buttons
		beatsTapButton = (Button) findViewById(R.id.buttonTap);
		//Tap start button
		beatsTapStopButton = (Button) findViewById(R.id.buttonStopTap);

		//set context and flag
		contextActivity = this;
		flag = false;

		//initialize the id
		id = -1L;
		//initialize time counters
		currentTime = -1L;
		timeDeltaSum = 0L;
		oldTime = -1L;

		//initialize counters with default values
		bpm = 0;
		beats = 0;
		taps = -1;

		//initialize the sound frequency variables
		beatSound = BEAT_SOUND;
		sound = SOUND;

		//restore previous autosave if applicable
		//check if saved or not
		SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
		boolean save = preferences.getBoolean(DB_SAVE_EXISTS, false);
		if (save) {
			List<Preset> presetList = Select.from(Preset.class).list();
			double[] valuesArray = getAutoSaveValues(presetList);
			if (valuesArray != null) {
				beats = (int) valuesArray[BEATS_INDEX];
				bpm = (int) valuesArray[BPM_INDEX];
				beatSound = valuesArray[BEAT_SOUND_INDEX];
				sound = valuesArray[SOUND_INDEX];
			}
		}

		//initialize the TextViews
		textViewBPM.setText(String.format(Locale.US, "%d", bpm));
		textViewBeats.setText(String.format(Locale.US, "%d", beats));

		//initialize the PitchGenerator object
		pitchGenerator = new PitchGenerator();

		//initialize the metronome object
		metronome = new Metronome();
		metronome.setBeatSound(beatSound);
		metronome.setSound(sound);
		metronome.setBpm(bpm);
		metronome.setBeat(beats);

		//copy
		currentMetronome = metronome.copyMetronome();

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

		//start/stop
		startButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//sanity check for the values
				if (bpm > 0 && beats > 0)
					if (!flag) {
						//update the values
						metronome.setBeat(beats);
						metronome.setBpm(bpm);
						metronome.setBeatSound(beatSound);
						metronome.setSound(sound);
						//reset the current metronome and re-copy
						metronomeReset();
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
						metronomeStop();
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
		//BPM round buttons
		roundUpButton.setOnClickListener(roundUpListener);
		roundDownButton.setOnClickListener(roundDownListener);
		//long click
		roundUpButton.setOnLongClickListener(roundUpLongClickListener);
		roundDownButton.setOnLongClickListener(roundDownLongClickListener);

		//number of the bea(s)ts buttons
		beatsPlusOneButton.setOnTouchListener(new RepeatListener(INIT_INTERVAL, NORMAL_INTERVAL, beatsClickListener));
		beatsMinusOneButton.setOnTouchListener(new RepeatListener(INIT_INTERVAL, NORMAL_INTERVAL, beatsClickListener));

		//tap buttons listeners
		beatsTapButton.setOnClickListener(tapClickListener);

		//start after tap button listener
		beatsTapStopButton.setOnClickListener(stopTapClickListener);

		//tone selection menu button
		toneButton.setOnClickListener(toneClickListener);

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
		//auto-save
		//delete previous entry
		List<Preset> presetList = Select.from(Preset.class).list();
		Preset preset = getAutoSave(presetList);
		if (preset != null)
			preset.delete();
		//save new entry
		preset = new Preset(AUTO_SAVE, beats, bpm, AUTO_SAVE_FLAG_TRUE, beatSound, sound);
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
				metronomeStop();
				aboutDialog();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	//delete from db
	private void delete(Long ident) {
		//stop the metronome
		metronomeStop();
		//delete
		Preset preset = Preset.findById(Preset.class, ident);
		preset.delete();
		//reset the id
		id = -1L;
	}

	//NOTICE: I know that the tick and tock options are REVERSED, but for some reason if they aren't reversed in the code they get reversed somewhere else!
	//note selection dialog
	private void noteDialog() {
		//stop the metronome
		metronomeStop();
		//list the options
		final String[] notes = pitchGenerator.getNotes();
		AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
		builder.setSingleChoiceItems(notes, 0, null).setPositiveButton(R.string.tock, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//dismiss the dialog
				dialog.dismiss();
				//get selected entry/position/row
				if (((AlertDialog) dialog).getListView() != null) {
					int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
					double[] freqs = pitchGenerator.getFreqs();
					beatSound = freqs[selectedPosition];
				}
			}
		}).setNegativeButton(R.string.tick, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//dismiss the dialog
				dialog.dismiss();
				//get selected entry/position/row
				if (((AlertDialog) dialog).getListView() != null) {
					int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
					double[] freqs = pitchGenerator.getFreqs();
					sound = freqs[selectedPosition];
				}
			}
		}).setNeutralButton(R.string.reset, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//dismiss the dialog
				dialog.dismiss();
				//restore default values
				sound = SOUND;
				beatSound = BEAT_SOUND;
			}
		}).show();
	}

	//restore dialog
	private void restoreDialog() {
		//stop the metronome
		metronomeStop();
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
								beats = presetList.get(selectedPosition).getBeats();
								bpm = presetList.get(selectedPosition).getBpm();
								id = presetList.get(selectedPosition).getId();
								beatSound = presetList.get(selectedPosition).getBeatSound();
								sound = presetList.get(selectedPosition).getSound();
								textViewBPM.setText(String.format(Locale.US, "%d", bpm));
								textViewBeats.setText(String.format(Locale.US, "%d", beats));
								AudioGenerator audioGenerator = metronome.getAudioGenerator();
								metronome = null;
								metronome = new Metronome(audioGenerator);
								metronome.setBeatSound(beatSound);
								metronome.setSound(sound);
								metronome.setBpm(bpm);
								metronome.setBeat(beats);
								currentMetronome = metronome.copyMetronome();
							}
						}
					}).setNegativeButton(R.string.delete, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//dismiss the dialog
					dialog.dismiss();
					//get selected entry/position/row
					if (((AlertDialog) dialog).getListView() != null) {
						int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
						//delete the selection
						delete(presetList.get(selectedPosition).getId());
					}
				}
			}).show();
		} else
			Toast.makeText(contextActivity, getResources().getText(R.string.no_saved_presets_toast), Toast.LENGTH_SHORT).show();

	}

	//save dialog
	private void saveDialog() {
		//stop the metronome
		metronomeStop();
		//start the activity to do the dirty work for you
		Intent intent = new Intent(MetronomeActivity.this, SaveDialogActivity.class);
		intent.putExtra(DIALOG_SAVE_BPM, bpm);
		intent.putExtra(DIALOG_SAVE_BEATS, beats);
		intent.putExtra(DIALOG_SAVE_BEAT_SOUND, beatSound);
		intent.putExtra(DIALOG_SAVE_SOUND, sound);
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
		Spanny message = new Spanny(getString(R.string.app_name) + '\n', new UnderlineSpan()).append('\n' + getString(R.string.email)).append('\n' + getString(R.string.copyright)).append('\n' + version +'\n').append('\n' + getString(R.string.about_note)).append("\n\n\n\n\n" + getString(R.string.license));
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
		dialog.setView(layout);
		//display the dialog to the user
		dialog.show();
		//set the background color
		dialog.getWindow().setBackgroundDrawableResource(R.color.colorPrimaryDark);
	}

	//used for the tone menu button
	private View.OnClickListener toneClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			noteDialog();
		}
	};

	//used for setting the number of the bea(s)ts per minute
	private View.OnClickListener bpmClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			//stop and reset the metronome on change of values
			metronomeStop();
			//button logic
			switch (v.getId()) {
				case R.id.buttonPlusOne:
					bpm++;
					textViewBPM.setText(String.format(Locale.US, "%d", bpm));
					break;
				case R.id.buttonMinusOne:
					//make sure not to go under zero
					if (bpm - 1 >= 0) {
						bpm--;
						textViewBPM.setText(String.format(Locale.US, "%d", bpm));
					}
					break;
				case R.id.buttonPlusTen:
					bpm += 10;
					textViewBPM.setText(String.format(Locale.US, "%d", bpm));
					break;
				case R.id.buttonMinusTen:
					//make sure not to go under zero
					if (bpm - 10 >= 0) {
						bpm -= 10;
						textViewBPM.setText(String.format(Locale.US, "%d", bpm));
					}
					break;

			}
			//reset the id
			id = -1L;
		}
	};

	//used for the tap tempo tap button
	private View.OnClickListener tapClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			//stop the metronome
			metronomeStop();
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
			//check if there are enough taps
			if (taps == -1 || taps == 0 || totalTime == 0L) {
				Toast.makeText(MetronomeActivity.this, "Not enough taps, at least two needed!", Toast.LENGTH_SHORT).show();
				//reset
				//stop the metronome
				metronomeStop();
				taps = -1;
				oldTime = 0L;
				currentTime = 0L;
				timeDeltaSum = 0L;

				return;
			}
			//bpm calculation
			TapTempoUtils tapTempoUtils = new TapTempoUtils(taps - 1, timeDeltaSum);
			bpm = tapTempoUtils.calculateBPM();
			textViewBPM.setText(String.format(Locale.US, "%d", bpm));
			taps = -1;
			oldTime = 0L;
			currentTime = 0L;
			timeDeltaSum = 0L;
		}
	};

	//used for rounding the BPM value up
	private View.OnClickListener roundUpListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (bpm % 10 != 0)
				if (((bpm / 10 + 1) * 10) != bpm) {
					bpm = bpm / 10 + 1;
					bpm = bpm * 10;
					textViewBPM.setText(String.format(Locale.US, "%d", bpm));
				}
		}
	};

	//used for rounding the BPM value down
	private View.OnClickListener roundDownListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (bpm % 10 != 0)
				if ((bpm - 10) >= 0) {
					bpm = bpm / 10;
					bpm = bpm * 10;
					textViewBPM.setText(String.format(Locale.US, "%d", bpm));
				} else {
					bpm = 0;
					textViewBPM.setText(String.format(Locale.US, "%d", bpm));
				}
		}
	};

	//long click up button
	private View.OnLongClickListener roundUpLongClickListener = new View.OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			Toast.makeText(contextActivity, getResources().getText(R.string.round_up_toast), Toast.LENGTH_SHORT).show();
			return true;
		}
	};

	//long click down button
	private View.OnLongClickListener roundDownLongClickListener = new View.OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			Toast.makeText(contextActivity, getResources().getText(R.string.round_down_toast), Toast.LENGTH_SHORT).show();
			return true;
		}
	};

	//used for setting the number of beats
	private View.OnClickListener beatsClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			//stop and reset the metronome on change of values
			metronomeStop();
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

	//used to find and return the values of the auto-save preset
	private double[] getAutoSaveValues(List<Preset> presetList) {
		Preset preset = getAutoSave(presetList);
		if (preset == null)
			return null;
		double[] values = new double[AUTO_SAVE_VALUES_ARRAY_LENGTH];
		values[BEATS_INDEX] = preset.getBeats();
		values[BPM_INDEX] = preset.getBpm();
		values[BEAT_SOUND_INDEX] = preset.getBeatSound();
		values[SOUND_INDEX] = preset.getBeatSound();
		return values;
	}

	//used to find and return the object "Preset" of the auto-save preset
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
		double[] pitches = pitchGenerator.getFreqs();
		String[] notes = pitchGenerator.getNotes();
		int indexBeatSound;
		int indexSound;
		while (i < length) {
			//note that there is no need for sorting the array before using it since the values are already set in a ascending order!
			indexBeatSound = Arrays.binarySearch(pitches, presetList.get(i).getBeatSound());
			indexSound = Arrays.binarySearch(pitches, presetList.get(i).getSound());
			titles[i] = presetList.get(i).getTitle() + " - " + presetList.get(i).getBpm() + "/" + presetList.get(i).getBeats()+ " - " + notes[indexBeatSound] + "|" + notes[indexSound];
			i++;
		}
		return titles;
	}

	//helper function used to stop and reset the metronome and related variables
	private void metronomeStop(){
		//stop the metronome
		currentMetronome.stop();
		//delegate the rest to the other helper function
		metronomeReset();
	}
	//helper function used to reset the metronome and related variables
	private void metronomeReset(){
		//stop the metronome
		currentMetronome.setBeatSound(0.0);
		currentMetronome.setBeat(0);
		currentMetronome.setSound(0.0);
		currentMetronome.setBpm(0.0);
		currentMetronome = metronome.copyMetronome();
		flag = false;
	}
}

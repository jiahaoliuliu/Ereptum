/*
 * Copyright (C) 2012 Jiahao Liu <http://www.jiahaoliuliu.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jiahaoliuliu.android.ereptum;

import java.util.Timer;
import java.util.TimerTask;

import com.jiahaoliuliu.android.ereptum.R;

import kankan.wheel.widget.OnWheelChangedListener;
import kankan.wheel.widget.OnWheelClickedListener;
import kankan.wheel.widget.OnWheelScrollListener;
import kankan.wheel.widget.WheelView;
import kankan.wheel.widget.adapters.NumericWheelAdapter;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;

//TODO: Press a button more than x seconds, then it will activate a false
// call by itself
//TODO: Record voices and reproduce it when the user "receive" the call
//Use: 1. Escape from meetings
//     2. Find an excuse to go out and smoke
public class Ereptum extends Activity {
	
	private static final String LOG_TAG = "Ereptum";
	private static final String LOG_LIFECYCLE= "Ereptum";
	
	//The context of the application
	private Context context;
	
	//Used to decrease wheels
	private DecreaseWheels decreaseWheels;
	private static final long DECREASE_INTERVAL = 1000;

	private WheelView hoursWheel;
	//Use to check the wheels
	private CheckWheelsCountDown checkHoursWheelsCountDown = null;
	private static final int MAX_HOURS = 23;
	private static final int MIN_HOURS = 0;
	private int actualScheduledHours = MIN_HOURS;
	//Used for Reset
	private static final String LAST_HOURS = "lastScheduledHours";
	
	private WheelView minutesWheel;
	//Use to check the wheels
	private CheckWheelsCountDown checkMinutesWheelsCountDown = null;
	private static final int MAX_MINUTES = 59;
	private static final int MIN_MINUTES = 0;
	private int actualScheduledMinutes = MIN_MINUTES;
	//Used for Reset
	private static final String LAST_MINUTES = "lastScheduledMinutes";

	private WheelView secondsWheel;
	//Use to check the wheels
	private CheckWheelsCountDown checkSecondsWheelsCountDown = null;
	private static final int MAX_SECONDS = 59;
	private static final int MIN_SECONDS = 0;
	private int actualScheduledSeconds = MIN_SECONDS;
	//Used for Reset
	private static final String LAST_SECONDS = "lastScheduledSeconds";
	
	//The scheduled time
	private long scheduledTime = 0;
	
	//The absolute scheduled time based on the epoch
	private static final String ABSOLUTE_SCHEDULED_TIME = "scheduledTime";

	private Button startButton;
	//See if there is a call scheduled
	//private Boolean callScheduled;
	private static final String CALL_SCHEDULED = "callScheduled";
	private Button resetButton;
		
	//Toggle button
	//Vibrator
	private static final long DUR_VIB = 100;
	private String vibratorService;
	private Vibrator vibrator;
	private ToggleButton vibrationOnButton;
	private boolean isVibrationOn;
	private static final String SPVIBRATION = "vibration";
	
	//Volume control
	private ToggleButton volumeOnButton;
	private static final String SPVOLUME = "volume";
	private boolean isVolumeOn;
	
	//Alarms
	private AlarmManager alarms;
	private int alarmType;
    private Intent intentToFire;
    private PendingIntent pendingIntent;
    
    //Notification from InCallScreen
    private IntentFilter notifyIntentFilter;
    public static final String NOTIFY_INTENT_ACTION = "com.jiahaoliuliu.android.callmesoon.incallscreenactivated";

    //Shared preference
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private static final String SPNAME = "settings";
    
    private AlertDialog.Builder welcomeAlert;
    //To see if the user is using this application for the first time
    private static final String FIRST_TIME_USE = "firsttimeuse";
	private AlertDialog.Builder aboutUsAlert;
	private AlertDialog.Builder helpAlert;
		
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
       
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.time_layout);
        setContentView(R.layout.timer_layout);
        
        context = this;
        
        Log.i(LOG_TAG, "Ereptum created");
        Log.i(LOG_LIFECYCLE, "Ereptum created");
        //Lock the rotation
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        //Shared preferences
        sharedPreferences = getSharedPreferences(SPNAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        
        //Buttons
        startButton = (Button)findViewById(R.id.start);
        startButton.setOnClickListener(btnClick);
        
        resetButton = (Button)findViewById(R.id.reset);
        resetButton.setOnClickListener(btnClick);

        //Create alarm manager
        alarms = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        
        //It depends on the time passed since the device has booted
        alarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
        intentToFire = new Intent(this.getApplicationContext(), InCallScreen.class);
        intentToFire.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
		pendingIntent =
				PendingIntent.getActivity(this.getApplicationContext(),
										  0,
										  intentToFire,
										  PendingIntent.FLAG_CANCEL_CURRENT);
        
		//Register the broadcast receiver so when the incallscreen is on, this activity
		// can make changes
		notifyIntentFilter = new IntentFilter(NOTIFY_INTENT_ACTION);
		registerReceiver(receiver, notifyIntentFilter);
		
		//Volume
        volumeOnButton = (ToggleButton)findViewById(R.id.volumeButton);
        volumeOnButton.setOnClickListener(btnClick);
        
        isVolumeOn = sharedPreferences.getBoolean(SPVOLUME, true);
        volumeOnButton.setChecked(isVolumeOn);		
		
        //Vibration 
        vibrationOnButton = (ToggleButton)findViewById(R.id.vibrationButton);
        vibrationOnButton.setOnClickListener(btnClick);
        
        isVibrationOn = sharedPreferences.getBoolean(SPVIBRATION, true);
        vibrationOnButton.setChecked(isVibrationOn);
        
        vibratorService = Context.VIBRATOR_SERVICE;
        vibrator = (Vibrator)getSystemService (vibratorService);
                
        boolean firstTimeUsing = sharedPreferences.getBoolean(FIRST_TIME_USE, true);
        if (firstTimeUsing) {
        	Log.v(LOG_TAG, "The application is running for the first time");
        	createWelcomeAlert();
        } else {
        	Log.v(LOG_TAG, "The user has already used this application before");
        }
        
        editor.putBoolean(FIRST_TIME_USE, false);
        editor.commit();
        
        //Wheel
        hoursWheel = (WheelView) findViewById(R.id.hours);
        hoursWheel.setViewAdapter(new NumericWheelAdapter(this, MIN_HOURS, MAX_HOURS));
        
        minutesWheel = (WheelView) findViewById(R.id.minutes);
        // "%02d" is the format of the string
        minutesWheel.setViewAdapter(new NumericWheelAdapter(this, MIN_MINUTES, MAX_MINUTES, "%02d"));
        //minutes.setCyclic(true);

        secondsWheel = (WheelView) findViewById(R.id.seconds);
        // "%02d" is the format of the string
        secondsWheel.setViewAdapter(new NumericWheelAdapter(this, MIN_SECONDS, MAX_SECONDS, "%02d"));
        //seconds.setCyclic(true);
        
        //Get the values from the shared preferences.
        actualScheduledHours = sharedPreferences.getInt(LAST_HOURS, MIN_HOURS);
        actualScheduledMinutes = sharedPreferences.getInt(LAST_MINUTES, MIN_MINUTES);
        actualScheduledSeconds = sharedPreferences.getInt(LAST_SECONDS, MIN_SECONDS);
        
        setWheelValue(hoursWheel, actualScheduledHours, false);
        setWheelValue(minutesWheel, actualScheduledMinutes, false);
        setWheelValue(secondsWheel, actualScheduledSeconds, false);
        
        OnWheelScrollListener scrollListener = new OnWheelScrollListener() {
        	public void onScrollingStarted(WheelView wheel) {
        		//timeScrolled = true;
        		Log.v(LOG_TAG, "Wheel Scrolled");
        		//Cancel the CheckWheelsCountDown
        		if ((wheel == hoursWheel) && (checkHoursWheelsCountDown != null)) {
        			Log.v(LOG_TAG, "Canceling the hours check");
        			checkHoursWheelsCountDown.cancel();
        		}

        		if ((wheel == minutesWheel) && (checkMinutesWheelsCountDown != null)) {
        			Log.v(LOG_TAG, "Canceling the minutes check");
        			checkMinutesWheelsCountDown.cancel();
        		}

        		if ((wheel == secondsWheel) && (checkSecondsWheelsCountDown != null)) {
        			Log.v(LOG_TAG, "Canceling the seconds check");
        			checkSecondsWheelsCountDown.cancel();
        		}
        	}
        	
			public void onScrollingFinished(WheelView wheel) {
        		//timeScrolled = false;
        		//timeChanged = true;
        		actualScheduledHours = (hoursWheel.getCurrentItem());
        		actualScheduledMinutes = (minutesWheel.getCurrentItem());
        		actualScheduledSeconds = (secondsWheel.getCurrentItem());
        	}
        };
        
        hoursWheel.addScrollingListener(scrollListener);
        minutesWheel.addScrollingListener(scrollListener);
        secondsWheel.addScrollingListener(scrollListener);
    }
        
    private View.OnClickListener btnClick = new View.OnClickListener() {
		
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.start: {
				boolean callScheduled = sharedPreferences.getBoolean(CALL_SCHEDULED, false);
				if (!callScheduled){
					Log.v(LOG_TAG, "Start counting");
					startSchedulingACall();
					
				} else {
					Log.v(LOG_TAG, "Stop counting");
					Toast.makeText(context, context.getText(R.string.call_canceled), Toast.LENGTH_SHORT).show();
					restartValues();
				}
				break;
			}
			case R.id.reset:{
				Log.v(LOG_TAG, "Restarting the values");
				boolean callScheduled = sharedPreferences.getBoolean(CALL_SCHEDULED, false);
				if (callScheduled) {
					Toast.makeText(context, context.getText(R.string.call_canceled), Toast.LENGTH_SHORT).show();
				}
				restartValues();
				break;
			}
			case R.id.vibrationButton: {
					if (vibrationOnButton.isChecked()) {
						vibrator.vibrate(DUR_VIB);
					}
					editor.putBoolean(SPVIBRATION, vibrationOnButton.isChecked());
					editor.commit();
				break;
			}
			case R.id.volumeButton: {
				editor.putBoolean(SPVOLUME, volumeOnButton.isChecked());
				editor.commit();
				break;
			}
			default: {
				Log.e(LOG_TAG, "Error, Button not recognized");
			}
				
			}
		}
    };
    
    private boolean startSchedulingACall() {
    	Boolean result = true;
    	    	
    	//Cancel all the check wheels
    	cancelAllCheckWheels();
    	
		Log.v(LOG_TAG, "Call Scheduled in " + actualScheduledHours + " hours, " + 
				   actualScheduledMinutes + " minutes and " + 
				   actualScheduledSeconds + " seconds");
				
		//Set the time
		setWheelValue(hoursWheel, actualScheduledHours, true);
		setWheelValue(minutesWheel, actualScheduledMinutes, true);
		setWheelValue(secondsWheel, actualScheduledSeconds, true);
				
		startButton.setText(R.string.stop_button);
		
		editor.putBoolean(CALL_SCHEDULED, true);
		editor.commit();
		
		Toast.makeText(Ereptum.this, context.getText(R.string.call_scheduled_at) + " " + 
				actualScheduledHours + " " + context.getText(R.string.hour_text) + ", " +
				actualScheduledMinutes + " " + context.getText(R.string.minutes_text) + ", " +
				actualScheduledSeconds + " " + context.getText(R.string.seconds_text),
									     Toast.LENGTH_LONG).show();
		startAlarm();
		return result;
    }

	private Boolean startAlarm() {
    	Log.i(LOG_TAG, "Start alarm");
    	Boolean result = true;
    	
		//Save them to the shared preferences
		editor.putInt(LAST_HOURS, actualScheduledHours);
		editor.commit();
		
		editor.putInt(LAST_MINUTES, actualScheduledMinutes);
		editor.commit();
		
		editor.putInt(LAST_SECONDS, actualScheduledSeconds);
		editor.commit();

    	scheduledTime = new Long (
    			((((actualScheduledHours * 60) + //scheduledTime in minutes
    			actualScheduledMinutes) * 60)  + //scheduledTime in seconds
    			actualScheduledSeconds) * 1000); //scheduledTime in miliSeconds
    	
		long scheduledTimeRelative = SystemClock.elapsedRealtime() + scheduledTime;
		Log.i(LOG_TAG, "Alarm set " + scheduledTimeRelative );
		
		//Save it into shared preferences
		editor.putLong(ABSOLUTE_SCHEDULED_TIME, scheduledTimeRelative);
		editor.commit();
		
		alarms.set(alarmType, scheduledTimeRelative, pendingIntent);

    	//Stop the previous decreaseWheel, if any
    	if (decreaseWheels != null) {
    		decreaseWheels.cancel();
    		decreaseWheels = null;
    	}
    	
		//Decrease the wheels
		decreaseWheels = new DecreaseWheels(scheduledTime,
										    DECREASE_INTERVAL);
		decreaseWheels.start();
		
		//Disable the wheels
		hoursWheel.setEnabled(false);
		minutesWheel.setEnabled(false);
		secondsWheel.setEnabled(false);
		return result;
    }
    		    
    //Restart all the values. So the screen looks like the beginning
    private void restartValues() {
    	//Set the start button
		startButton.setText(R.string.start_button);
		
		//Set the scheduled status
		editor.putBoolean(CALL_SCHEDULED, false);
		editor.commit();
		
		//Set the scheduled time
		editor.putLong(ABSOLUTE_SCHEDULED_TIME, 0);
		editor.commit();
		
		if (alarms != null) {
			alarms.cancel(pendingIntent);
		}
		
		//Cancel the DecreaseCountDown
		if (decreaseWheels != null) {
			decreaseWheels.cancel();
			decreaseWheels = null;
		}
				
		//Restore the previous values
		actualScheduledHours = sharedPreferences.getInt(LAST_HOURS, MIN_HOURS);
		actualScheduledMinutes = sharedPreferences.getInt(LAST_MINUTES, MIN_MINUTES);
		actualScheduledSeconds = sharedPreferences.getInt(LAST_SECONDS, MIN_SECONDS);
		
		//Reset the timer
		setWheelValue(hoursWheel, actualScheduledHours, true);
		setWheelValue(minutesWheel, actualScheduledMinutes, true);
		setWheelValue(secondsWheel, actualScheduledSeconds, true);
		
		//Enable the wheels
		hoursWheel.setEnabled(true);
		minutesWheel.setEnabled(true);
		secondsWheel.setEnabled(true);
    }
    
    // -------------------------------------------------------- Menus ------------------------
    
    @Override
    public boolean onCreateOptionsMenu (Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.main_menu, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case R.id.menufeedback: {
    			//Open the facebook web page
    			Uri uri = Uri.parse("http://m.facebook.com/profile.php?id=235403849849928");
    			Intent launchBroserIntent = new Intent(Intent.ACTION_VIEW, uri);
    			startActivity(launchBroserIntent);
    			break;
    		}
    		case R.id.menunews: {
    			//Open the twitter web page
    			Uri uri = Uri.parse("https://mobile.twitter.com/#!/ereptum");
    			Intent launchBroserIntent = new Intent(Intent.ACTION_VIEW, uri);
    			startActivity(launchBroserIntent);
    			break;
    		}
    		case R.id.menuabout: {
    	        createAboutUsAlert();
    			aboutUsAlert.show();
    			break;
    		}
    		case R.id.menuhelp: {
    	        createHelpAlert();
    			helpAlert.show();
    		}
    	}
    	return true;
    }
    private void createWelcomeAlert() {
    	Context context = this;
    	String button1String = context.getString(R.string.welcomeButton);
    	
    	welcomeAlert = new AlertDialog.Builder(context);
    	welcomeAlert.setTitle(R.string.welcomeTitle);
    	welcomeAlert.setMessage(R.string.welcomeContent);
    	welcomeAlert.setPositiveButton(button1String, new DialogInterface.OnClickListener() {
    		public void onClick (DialogInterface dialog, int arg1) {
    			Log.v(LOG_TAG, "Positive button pressed");
    			//Do nothing
    		}
    	});
    	welcomeAlert.show();
    }
    
    private void createHelpAlert() {
    	Context context = this;
    	//Back button
    	String button1String = context.getString(R.string.helpBack);
    	//More info button
    	String button2String = context.getString(R.string.helpMore);
    	
    	helpAlert = new AlertDialog.Builder(context);
    	helpAlert.setTitle(R.string.helpTitle);
    	helpAlert.setMessage(R.string.helpContent);
    	helpAlert.setPositiveButton(button1String, new DialogInterface.OnClickListener() {
    		public void onClick (DialogInterface dialog, int arg1) {
    			Log.v(LOG_TAG, "Positive button pressed");
    			//Do nothing
    		}
    	});
    	
    	helpAlert.setNegativeButton (button2String, new DialogInterface.OnClickListener() {
    		public void onClick (DialogInterface dialog, int arg1) {
    			Log.v(LOG_TAG, "Negative button pressed");
    			Uri uri = Uri.parse("http://www.ereptum.com");
    			Intent launchBroserIntent = new Intent(Intent.ACTION_VIEW, uri);
    			startActivity(launchBroserIntent);
    		}
    	});
    	helpAlert.setCancelable(true);
    	helpAlert.setOnCancelListener(new DialogInterface.OnCancelListener() {
    		public void onCancel(DialogInterface dialog) {
    			//Do nothing
    		}
    	});
    }
    
    private void createAboutUsAlert() {
    	Context context = this;
    	//Back button
    	String button1String = context.getString(R.string.aboutUsBack);
    	//More info button
    	String button2String = context.getString(R.string.aboutUsMore);
    	
    	aboutUsAlert = new AlertDialog.Builder(context);
    	aboutUsAlert.setTitle(R.string.aboutUsTitle);
    	aboutUsAlert.setMessage(R.string.aboutUsContent);
    	aboutUsAlert.setPositiveButton(button1String, new DialogInterface.OnClickListener() {
    		public void onClick (DialogInterface dialog, int arg1) {
    			Log.v(LOG_TAG, "Positive button pressed");
    			//Do nothing
    		}
    	});
    	
    	aboutUsAlert.setNegativeButton (button2String, new DialogInterface.OnClickListener() {
    		public void onClick (DialogInterface dialog, int arg1) {
    			Log.v(LOG_TAG, "Negative button pressed");
    			Uri uri = Uri.parse("http://www.jiahaoliuliu.com/p/about-me.html");
    			Intent launchBroserIntent = new Intent(Intent.ACTION_VIEW, uri);
    			startActivity(launchBroserIntent);
    		}
    	});
    	aboutUsAlert.setCancelable(true);
    	aboutUsAlert.setOnCancelListener(new DialogInterface.OnCancelListener() {
    		public void onCancel(DialogInterface dialog) {
    			//Do nothing
    		}
    	});
    }
    
    // ---------------------------------------------- Activity Lifecycle ----------------------------
    @Override
    protected void onStart() {
    	super.onStart();
    	Log.i(LOG_LIFECYCLE, "Ereptum started");
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	Log.i(LOG_LIFECYCLE, "Ereptum resumed");
    	
    	//Set the right button
    	boolean callScheduled = sharedPreferences.getBoolean(CALL_SCHEDULED, false);
    	if (callScheduled) {
    		startButton.setText(R.string.stop_button);
    	} else {
    		startButton.setText(R.string.start_button);
    	}
    	
    	long scheduledTime = sharedPreferences.getLong(ABSOLUTE_SCHEDULED_TIME, 0);
    	Log.v(LOG_TAG, "The scheduled time is " + scheduledTime);
    	long actualTime = SystemClock.elapsedRealtime();
    	Log.v(LOG_TAG, "The actual time is " + actualTime);
    	long timeLeft = scheduledTime - actualTime;
    	Log.v(LOG_TAG, "The time left in ms is " + timeLeft);
    	
    	if (timeLeft > 0 ) {
    		//Get the seconds
    		int timeLeftSeconds = (int) timeLeft / 1000;
    		Log.v(LOG_TAG, "The time left in seconds is " + timeLeftSeconds);
    		
    		if (timeLeftSeconds > 86400) {
    			Log.w(LOG_TAG, "It has passed more than 24 hours. Get the rest");
    			timeLeftSeconds = timeLeftSeconds % 86400;
    		}
    		
    		int hours = timeLeftSeconds / 3600;
    		Log.v(LOG_TAG, "The hours are " + hours);
    		hoursWheel.setCurrentItem(hours, false);

    		int minutes = (timeLeftSeconds % 3600) / 60;
    		Log.v(LOG_TAG, "The minutes are " + minutes);
    		minutesWheel.setCurrentItem(minutes, false);

    		int seconds = timeLeftSeconds % 60;
    		Log.v(LOG_TAG, "The seconds are " + seconds);
    		//The seconds set at the beginning should add 1 because it starts
    		// decreasing by decreaseWheels
    		seconds++;
    		secondsWheel.setCurrentItem(seconds, false);
    		    		
    		//Start a new decrease wheel
    		long differenceMilliseconds = timeLeft % 1000;
    		Log.v(LOG_TAG, "The difference in milliseconds is " + differenceMilliseconds);
    		
    		//Cancel the previous decrease wheels
    		if (decreaseWheels != null) {
    			decreaseWheels.cancel();
    			decreaseWheels = null;
    		}
    		
    		decreaseWheels = new DecreaseWheels(scheduledTime, DECREASE_INTERVAL, differenceMilliseconds);
    		decreaseWheels.start();
    		
    	} else {
    		restartValues();
    	}
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	Log.i(LOG_LIFECYCLE, "Ereptum paused");
    	if (decreaseWheels != null) {
    		Log.v(LOG_TAG, "Canceling the wheel");
    		decreaseWheels.cancel();
    		decreaseWheels = null;
    	}
    	
    	cancelAllCheckWheels();
    }

    @Override
    protected void onStop() {
    	super.onStop();
    	Log.i(LOG_LIFECYCLE, "Ereptum stopped");
    }
    
    @Override
    protected void onRestart() {
    	super.onRestart();
    	Log.v(LOG_LIFECYCLE, "Erepturm restarted");
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	Log.i(LOG_TAG, "Destrolled. Unregistering the alarm receiver");
		Log.i(LOG_LIFECYCLE, "Ereptum destroyed");
		unregisterReceiver(receiver);
	    unbindDrawables(findViewById(R.id.timer_layout));
	    System.gc();
	}

    private void unbindDrawables(View view) {
        if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
            ((ViewGroup) view).removeAllViews();
        }
    }
    
    // ------------------------------------------------ Private Methods ------------------------
    
    //Set the value of the wheels and check it
    private void setWheelValue(WheelView wheelView,	int value, boolean isAnimated) {
    	int wheelValue = wheelView.getCurrentItem();
    	if (wheelValue != value) {
	    	wheelView.setCurrentItem(value, isAnimated);
	    	
	    	if (wheelView == hoursWheel) {
	    		Log.v(LOG_TAG, "Setting the hours wheel");
				checkHoursWheelsCountDown = new CheckWheelsCountDown(wheelView, value);
				checkHoursWheelsCountDown.start();
	    	} else if (wheelView == minutesWheel) {
	    		Log.v(LOG_TAG, "Setting the minutes wheel");
				checkMinutesWheelsCountDown = new CheckWheelsCountDown(wheelView, value);
				checkMinutesWheelsCountDown.start();
	    	} else if (wheelView == secondsWheel) {
	    		Log.v(LOG_TAG, "Setting the seconds wheel");
				checkSecondsWheelsCountDown = new CheckWheelsCountDown(wheelView, value);
				checkSecondsWheelsCountDown.start();
	    	}
    	}
	}
    
	
    public class DecreaseWheels extends CountDownTimer {
    	
    	//Do not decrease for the first tick
    	private boolean firstTick = true;
    	private long waitingMilliseconds = 0;
    	    	
    	public DecreaseWheels(long millisInFuture,
    						  long countDownInterval) {
    		super(millisInFuture, countDownInterval);
    	}
    	
    	public DecreaseWheels(long millisInFuture,
    						  long countDownInterval,
    						  long waitingMilliseconds) {
    		super(millisInFuture, countDownInterval);
    		this.waitingMilliseconds = waitingMilliseconds;
    	}
    	
    	@Override
    	public void onFinish() {
    		Log.i(LOG_TAG, "finished");
    		decreaseOneSecond();    			
    	}
    	
    	@Override
    	public void onTick(long millisUntilFinished) {
    		Log.i(LOG_TAG, "tick");
    		if (firstTick) {
    			firstTick = false;
    			if (waitingMilliseconds != 0) {
    				try {
						Thread.sleep(waitingMilliseconds);
					} catch (InterruptedException e) {
						Log.e(LOG_TAG, "Error waiting in DecreaseWheels", e);
					}
    			}
    		} else {
        		decreaseOneSecond();    			
    		}
    	}
    	
    	private void decreaseOneSecond(){
    		Log.i(LOG_TAG, "Decreasing one second");
    		int currentSecond = secondsWheel.getCurrentItem();
			int currentMinutes = minutesWheel.getCurrentItem();
			int currentHours = hoursWheel.getCurrentItem();

    		if (currentSecond > 0) {
    			Log.v(LOG_TAG, "Value bigger than 0 " + currentSecond);
    			currentSecond = currentSecond - 1;
    			//Because the wheel has been disabled, it is not necessary check it
    			secondsWheel.setCurrentItem(currentSecond, true);
    		} else if (currentSecond == 0) {
    			Log.v(LOG_TAG, "Value equal to zero. Restarting to " + MAX_SECONDS);
    			currentSecond = MAX_SECONDS;
    			secondsWheel.setCurrentItem(currentSecond, false);
    			//Decrease minutes
    			Log.v(LOG_TAG, "Decreasing one minutes");
    			if (currentMinutes > 0) {
        			Log.v(LOG_TAG, "Value bigger than 0 " + currentMinutes);
        			currentMinutes = currentMinutes - 1;
        			//Because the wheel has been disabled, it is not necessary check it
        			minutesWheel.setCurrentItem(currentMinutes, true);
        		} else if (currentMinutes == 0) {
        			Log.v(LOG_TAG, "Value equal to zero. Restarting to " + MAX_MINUTES);
        			currentMinutes = MAX_MINUTES;
        			minutesWheel.setCurrentItem(currentMinutes, false);
        			//Decrease hours
        			Log.v(LOG_TAG, "Decreasing one hour");
        			if (currentHours > 0) {
            			Log.v(LOG_TAG, "Value bigger than 0 " + currentHours);
            			currentHours = currentHours - 1;
            			//Because the wheel has been disabled, it is not necessary check it
            			hoursWheel.setCurrentItem(currentHours, true);
            		} //if current hours == 0 -> do nothing
    			}
    		}
    		Log.v(LOG_TAG, "The actual timer is " + currentHours + "h " + currentMinutes + "m " + currentSecond + "s");
	    }
    }

    private void cancelAllCheckWheels() {

    	if (checkHoursWheelsCountDown != null) {
    		checkHoursWheelsCountDown.cancel();
    	}
    	
    	if (checkMinutesWheelsCountDown != null) {
    		checkMinutesWheelsCountDown.cancel();
    	}
    	
    	if (checkSecondsWheelsCountDown != null) {
    		checkSecondsWheelsCountDown.cancel();
    	}
    }
    
	//The boradcast receiver for the alarm
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.e(LOG_TAG, "incallScreen activated received");
			restartValues();			
			finish();
		}
	};
}
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

import java.io.IOException;

import com.jiahaoliuliu.android.ereptum.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;

public class InCallScreen extends Activity {
	
	private static final String LOG_TAG = "Ereptum";
	private Context context;
	private Button answerButton;
	private Button rejectButton;
	private boolean vibrationOn;
	
	private SharedPreferences sharedPreferences;
    private static final String SPNAME = "settings";
	
	//Volume
	private static final String SPVOLUME = "volume";
	private boolean isVolumeOn;
	private MediaPlayer mMediaPlayer;

	//Vibrator
	private long[] vibrationPattern = {1000, 2000};
	private String vibratorService;
	private Vibrator vibrator;
	private static final String SPVIBRATION = "vibration";
	
	private Intent notifyIntent;
			
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.receive_call);
        
        this.context = this;
        //Lock the rotation
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        //Toast
        //Toast.makeText(context, "Incoming call", Toast.LENGTH_LONG).show();
        
        //shared preferences
        sharedPreferences = getSharedPreferences(SPNAME, MODE_PRIVATE);
        
        answerButton = (Button)findViewById(R.id.answer);
        answerButton.setOnClickListener(btnClick);

        rejectButton = (Button)findViewById(R.id.reject);
        rejectButton.setOnClickListener(btnClick);
        
        //Check the volume
        isVolumeOn = true;
        isVolumeOn = sharedPreferences.getBoolean(SPVOLUME, true);
        
        if (isVolumeOn) {
        	try {
				reproduceRingtone();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
        vibrationOn = true;
        vibrationOn = sharedPreferences.getBoolean(SPVIBRATION, true);
        //Vibrator
        vibratorService = Context.VIBRATOR_SERVICE;
        vibrator = (Vibrator)getSystemService (vibratorService);
        if (vibrationOn) {
        	vibrator.vibrate(vibrationPattern, 0);
        }

        //Disable windows
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        
        //Notify to the class CallMeSoon
        notifyIntent = new Intent(Ereptum.NOTIFY_INTENT_ACTION);
        context.sendBroadcast(notifyIntent);
    }
    
    private View.OnClickListener btnClick = new View.OnClickListener() {
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.answer: {
				answerButton.setBackgroundDrawable(
							getResources().getDrawable(R.drawable.answerbuttonpressed75));
				answerCall();
				break;
			}
			case R.id.reject:{
				rejectButton.setBackgroundDrawable(
							getResources().getDrawable(R.drawable.rejectbuttonpressed75));
				rejectCall();
				break;
			}
			
			default: {
				Log.e(LOG_TAG, "Error, Button not recognized");
			}
				
			}
		}
    };
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent ev) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_CALL:
            	answerCall();
                break;
            case KeyEvent.KEYCODE_ENDCALL:
            	rejectCall();
            	break;
            default:
                return false;
        }
        return super.onKeyDown(keyCode, ev);
    }
    
	@Override
	public void onBackPressed() {
		//TODO: Check the back key
		Log.v(LOG_TAG, "Back key pressed");
		rejectCall();
		return;
	}
    
    private void answerCall() {
		Intent answerCallIntent = new Intent(context, AnswerCallScreen.class);
		startActivity(answerCallIntent);
		terminate();
    }
    
    private void rejectCall(){
    	terminate();
    }
    
    //Reproduce the ringtone of the phone
    private void reproduceRingtone() throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {
    	Uri defaultRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
    	if (defaultRingtone == null) {
    		//Alert is null, using backup
    		defaultRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
    		if (defaultRingtone == null) {
    			//alert backup is null, using second backup
    			defaultRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
    		}
    	}
    	
    	mMediaPlayer = new MediaPlayer();
    	try {
			mMediaPlayer.setDataSource(this, defaultRingtone);
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
			mMediaPlayer.setLooping(true);
			mMediaPlayer.prepare();
			mMediaPlayer.start();
			
		} catch (Exception e) {
			Log.e(LOG_TAG, "Ringtone not found", e);
		}
    }
    
    private void terminate() {
    	//Vibrator
    	if (vibrationOn) {
    		vibrator.cancel();
    	}
    	
    	//Terminate the media player
    	if (mMediaPlayer != null) {
    		mMediaPlayer.release();
    	}
    	finish();
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	terminate();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
	    unbindDrawables(findViewById(R.id.receive_call));
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
}
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

import com.jiahaoliuliu.android.ereptum.R;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.SystemClock;

import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.Chronometer.OnChronometerTickListener;
import android.widget.TextView;

public class AnswerCallScreen extends Activity {
	
	private TextView timeCounter;
	private long startTime;
	private long countUp;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.answer_call);
        
        //Lock the rotation
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        timeCounter = (TextView)findViewById(R.id.timecounter);
        Chronometer stopWatch = (Chronometer) findViewById(R.id.chrono);
        startTime = SystemClock.elapsedRealtime();
        
        stopWatch.setOnChronometerTickListener(new OnChronometerTickListener() {
        	public void onChronometerTick(Chronometer chrono) {
        		countUp = (SystemClock.elapsedRealtime() - chrono.getBase()) / 1000;
        		long minutes = (countUp /60);
        		String minutesText = "";
        		if (minutes < 10) {
        			minutesText = "0" + minutes;
        		} else {
        			minutesText = String.valueOf(minutes);
        		}
        		long seconds = (countUp % 60);
        		String secondsText = "";
        		if (seconds < 10) {
        			secondsText = "0" + seconds;
        		} else {
        			secondsText = String.valueOf(seconds);
        		}
        		String asText = minutesText + ":" + secondsText;
        		timeCounter.setText(asText);
        	}
        });
        stopWatch.start();
        
        //Disable windows
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent ev) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_ENDCALL: {
            	finish();
            	return false;
            }
        }
        return super.onKeyDown(keyCode, ev);
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	finish();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
	    unbindDrawables(findViewById(R.id.answer_call));
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
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

import kankan.wheel.widget.WheelView;
import android.os.CountDownTimer;
import android.util.Log;

public class CheckWheelsCountDown extends CountDownTimer {
	private static final String LOG_TAG = "CountDownWheels";
	
	private static final long millisInFuture = 4000; //4s
	
	private static final long countDownInterval = 400; //0,1s
		    	
	private int wheelValue = 0;
	
	private WheelView wheel;
	
	private int rightValue = 0;

	public CheckWheelsCountDown(WheelView wheel,
								int rightValue) {
		super(millisInFuture, countDownInterval);
		this.rightValue = rightValue;
		this.wheel = wheel;
	}
	
	@Override
	public void onFinish() {
		Log.i(LOG_TAG, "finished");
		checkWheel();
	}
	
	@Override
	public void onTick(long millisUntilFinished) {
		Log.i(LOG_TAG, "tick");
		checkWheel();
	}
	
	private void checkWheel(){
		Log.i(LOG_TAG, "Checking wheels");
		wheelValue = wheel.getCurrentItem();
		if (wheelValue != rightValue) {
			Log.v(LOG_TAG, "The wheels is not right " + wheelValue + ". It should be " + rightValue);
			wheel.setCurrentItem(rightValue, true);
		} else {
			Log.v(LOG_TAG, "Value correct " + wheelValue);
		}
    }
}
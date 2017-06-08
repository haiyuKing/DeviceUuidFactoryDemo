package com.why.project.deviceuuidfactorydemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.why.project.deviceuuidfactorydemo.utils.DeviceUuidFactory;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		String deviceIdStr = new DeviceUuidFactory(this).getUuid().toString();
		Log.w("MainActivity","deviceIdStr="+deviceIdStr);
	}
}

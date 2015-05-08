package org.zywx.wbpalmstar.plugin.uexsocketmgr;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.util.Log;

/**
 * 用来申请多播系统锁的工具类，某些个别手机需要此权限，并且还需要添加 <uses-permission
 * android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" /> 权限
 */
public class MulticastMgr {
	private static final String TAG = "MulticastMgr";
	private static MulticastLock multicastLock;

	public static void allowMulticast(Context context) {
		Log.i(TAG, "allowMulticast");
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		multicastLock = wifiManager
				.createMulticastLock("multicast.uexSocketMgr");
		multicastLock.acquire();
	}

	public static void disableMulticast() {
		Log.i(TAG, "disableMulticast");
		if (multicastLock == null) {
			return;
		}
		multicastLock.release();
		multicastLock = null;
	}
}

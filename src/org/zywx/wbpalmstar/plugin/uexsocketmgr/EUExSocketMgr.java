package org.zywx.wbpalmstar.plugin.uexsocketmgr;

import java.util.HashMap;
import java.util.Iterator;

import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class EUExSocketMgr extends EUExBase {

	public static final String tag = "uexSocketMgr_";
	public static final String F_CALLBACK_NAME_SOCKETDATA = "uexSocketMgr.onData";
	static final String F_CALLBACK_NAME_CREATETCPSOCKET = "uexSocketMgr.cbCreateTCPSocket";
	static final String F_CALLBACK_NAME_CREATEUDPSOCKET = "uexSocketMgr.cbCreateUDPSocket";
	private static final String F_CALLBACK_NAME_SENDDATA = "uexSocketMgr.cbSendData";
	public static final String F_CALLBACK_NAME_CONNECTED = "uexSocketMgr.cbConnected";
	public static final String F_CALLBACK_NAME_DISCONNECTED = "uexSocketMgr.onDisconnected";

	public static final int F_TYEP_TCP = 0;
	public static final int F_TYEP_UDP = 1;

	private HashMap<Integer, EUExSocket> objectMap;

	public EUExSocketMgr(Context context, EBrowserView inParent) {
		super(context, inParent);
		objectMap = new HashMap<Integer, EUExSocket>();
	}

	/**
	 * 创建个UDPSocket
	 * 
	 * @param aPort
	 *            绑定本地的UDP端口
	 * @return Socket 对象
	 */
	public void createUDPSocket(String[] parm) {
		if (parm.length < 2) {
			return;
		}
		String inOpCode = parm[0], inPort = parm[1], dataType = "0";
		if (parm.length == 3) {
			dataType = parm[2];
		}
		if (!BUtility.isNumeric(inOpCode)) {
			return;
		}
		if (objectMap.containsKey(Integer.parseInt(inOpCode))
				|| !checkSetting()) {
			jsCallback(F_CALLBACK_NAME_CREATEUDPSOCKET,
					Integer.parseInt(inOpCode), EUExCallback.F_C_INT,
					EUExCallback.F_C_FAILED);
			return;
		}
		if (inPort == null || inPort.length() == 0) {
			inPort = "0";
		}
		objectMap.put(Integer.parseInt(inOpCode), new EUExSocket(F_TYEP_UDP,
				Integer.parseInt(inPort), this, Integer.parseInt(inOpCode),
				Integer.parseInt(dataType), mContext));
	}

	/**
	 * 创建个TCPSocket
	 * 
	 * @return Socket 对象
	 */
	public void createTCPSocket(String[] parm) {
		if (parm.length < 1) {
			return;
		}

		String inOpCode = parm[0], dataType = "0";
		if (!BUtility.isNumeric(inOpCode)) {
			return;
		}
		if (parm.length == 2) {
			dataType = parm[1];
		}
		if (objectMap.containsKey(Integer.parseInt(inOpCode))
				|| !checkSetting()) {
			jsCallback(F_CALLBACK_NAME_CREATETCPSOCKET,
					Integer.parseInt(inOpCode), EUExCallback.F_C_INT,
					EUExCallback.F_C_FAILED);
			return;
		}
		objectMap.put(Integer.parseInt(inOpCode), new EUExSocket(F_TYEP_TCP, 0,
				this, Integer.parseInt(inOpCode), Integer.parseInt(dataType)));

	}

	/**
	 * 关闭Socket
	 * 
	 * @return boolean
	 */
	public void closeSocket(String[] parm) {
		String inOpCode = parm[0];
		if (!BUtility.isNumeric(inOpCode)) {
			return;
		}
		EUExSocket object = objectMap.remove(Integer.parseInt(inOpCode));
		if (object != null) {
			object.onClose();
			object.close();
		}

	}

	/**
	 * 设置 Socket 超时
	 * 
	 * @param timeOut
	 * @return boolean
	 */
	public void setTimeOut(String[] parm) {
		if (parm.length != 2) {
			return;
		}
		String inOpCode = parm[0], inTimeOut = parm[1];
		if (!BUtility.isNumeric(inOpCode)) {
			return;
		}
		EUExSocket object = objectMap.get(Integer.parseInt(inOpCode));
		if (object != null) {
			object.setTimeOut(inTimeOut);

		}

	}

	/**
	 * 设置 对方的ip和端口
	 * 
	 * @param inRemoteAddress
	 *            ip
	 * @param inRemotePort
	 *            端口
	 * @return boolean
	 */
	public void setInetAddressAndPort(String[] parm) {
		if (parm.length != 3) {
			return;
		}
		String inOpCode = parm[0], inRemoteAddress = parm[1], inRemotePort = parm[2];
		if (!BUtility.isNumeric(inOpCode)) {
			return;
		}
		EUExSocket object = objectMap.get(Integer.parseInt(inOpCode));
		if (object != null && checkSetting()) {
			object.setInetAddressAndPort(inRemoteAddress, inRemotePort);

		} else {
			jsCallback(EUExSocketMgr.F_CALLBACK_NAME_CONNECTED,
					Integer.parseInt(inOpCode), EUExCallback.F_C_INT,
					EUExCallback.F_C_FAILED);
		}
	}

	/**
	 * 发送数据
	 * 
	 * @param msg
	 *            内容
	 * @return boolean
	 */
	public void sendData(String[] parm) {
		Log.i(tag, "sendData");
		if (parm.length != 2) {
			return;
		}
		final String inOpCode = parm[0], inMsg = parm[1];
		if (!BUtility.isNumeric(inOpCode)) {
			return;
		}
		final EUExSocket object = objectMap.get(Integer.parseInt(inOpCode));

		new Thread(new Runnable() {

			@Override
			public void run() {
				if (object != null) {
					boolean result = object.sendData(inMsg);
					if (checkSetting() && result) {
						jsCallback(F_CALLBACK_NAME_SENDDATA,
								Integer.parseInt(inOpCode),
								EUExCallback.F_C_INT, EUExCallback.F_C_SUCCESS);
					} else {
						jsCallback(F_CALLBACK_NAME_SENDDATA,
								Integer.parseInt(inOpCode),
								EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
					}
				} else {
					jsCallback(F_CALLBACK_NAME_SENDDATA,
							Integer.parseInt(inOpCode), EUExCallback.F_C_INT,
							EUExCallback.F_C_FAILED);
				}
			}
		}).start();

	}

	@Override
	public boolean clean() {
		Iterator<Integer> iterator = objectMap.keySet().iterator();
		while (iterator.hasNext()) {
			EUExSocket object = objectMap.get(iterator.next());
			object.onClose();
			object.close();
		}
		objectMap.clear();
		return true;
	}

	public boolean checkSetting() {
		try {
			ConnectivityManager cm = (ConnectivityManager) mContext
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfos = cm.getActiveNetworkInfo();
			if (networkInfos != null) {
				boolean net = networkInfos.getState() == NetworkInfo.State.CONNECTED;
				boolean wifi = networkInfos.getType() == ConnectivityManager.TYPE_WIFI;
				return net || wifi;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

}

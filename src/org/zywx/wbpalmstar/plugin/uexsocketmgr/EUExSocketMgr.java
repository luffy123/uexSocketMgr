package org.zywx.wbpalmstar.plugin.uexsocketmgr;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;

import java.util.HashMap;
import java.util.Iterator;

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
     * @return Socket 对象
     */
    public boolean createUDPSocket(String[] parm) {
        if (parm.length < 2) {
            return false;
        }
        String inOpCode = parm[0], inPort = parm[1], dataType = "0";
        if (parm.length == 3) {
            dataType = parm[2];
        }
        setCharset(dataType);
        if (!BUtility.isNumeric(inOpCode)) {
            return false;
        }
        if (objectMap.containsKey(Integer.parseInt(inOpCode))
                || !checkSetting()) {
            jsCallback(F_CALLBACK_NAME_CREATEUDPSOCKET,
                    Integer.parseInt(inOpCode), EUExCallback.F_C_INT,
                    EUExCallback.F_C_FAILED);
            return false;
        }
        if (inPort == null || inPort.length() == 0) {
            inPort = "0";
        }
        EUExSocket socket = new EUExSocket(F_TYEP_UDP,
                Integer.parseInt(inPort), this, Integer.parseInt(inOpCode),
                Integer.parseInt(dataType), mContext);
        if (null == socket.getUDPSocket()) {
            return false;
        }
        objectMap.put(Integer.parseInt(inOpCode), socket);
        return true;

    }

    /**
     * 创建个TCPSocket
     *
     * @return Socket 对象
     */
    public boolean createTCPSocket(String[] parm) {
        if (parm.length < 1) {
            return false;
        }

        String inOpCode = parm[0], dataType = "0";
        if (!BUtility.isNumeric(inOpCode)) {
            return false;
        }
        if (parm.length == 2) {
            dataType = parm[1];
        }
        setCharset(dataType);
        if (objectMap.containsKey(Integer.parseInt(inOpCode))
                || !checkSetting()) {
            jsCallback(F_CALLBACK_NAME_CREATETCPSOCKET,
                    Integer.parseInt(inOpCode), EUExCallback.F_C_INT,
                    EUExCallback.F_C_FAILED);
            return false;
        }
        EUExSocket socket = new EUExSocket(F_TYEP_TCP, 0,
                this, Integer.parseInt(inOpCode), Integer.parseInt(dataType));
        if (null == socket.getTCPsocket()) {
            return false;
        }
        objectMap.put(Integer.parseInt(inOpCode), socket);
        return true;
    }

    private void setCharset(String dataType){
        if ("2".equals(dataType)){
            EUExSocket.charset="gbk";
        }
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
     * @return boolean
     */
    public void setInetAddressAndPort(String[] parm) {
        if (parm.length < 3) {
            return;
        }
        final String inOpCode = parm[0], inRemoteAddress = parm[1], inRemotePort = parm[2];
        if (!BUtility.isNumeric(inOpCode)) {
            return;
        }
        String funcId = null;
        if (parm.length == 4) {
            funcId = parm[3];
        }
        boolean flag = false;
        if (null != funcId) {
            flag = true;
        }
        final boolean hasCallbackFun = flag;
        final String funcIdTemp = funcId;
        final EUExSocket object = objectMap.get(Integer.parseInt(inOpCode));
        if (object != null && checkSetting()) {
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    boolean flag = object.setInetAddressAndPort(inRemoteAddress, inRemotePort, hasCallbackFun);
                    if (hasCallbackFun) {
                        callbackToJs(Integer.parseInt(funcIdTemp), false, flag ? EUExCallback.F_C_SUCCESS : EUExCallback.F_C_FAILED);
                    }
                }
            }.start();
        } else {
            if (hasCallbackFun) {
                callbackToJs(Integer.parseInt(funcId), false, EUExCallback.F_C_FAILED);
            } else {
                jsCallback(EUExSocketMgr.F_CALLBACK_NAME_CONNECTED,
                        Integer.parseInt(inOpCode), EUExCallback.F_C_INT,
                        EUExCallback.F_C_FAILED);
            }
        }
    }

    /**
     * 设置编码
     *
     * @return boolean
     */
    public void setCharset(String[] params) {
        if (params!=null&&params.length>0){
            EUExSocket.charset=params[0];
        }
    }


    /**
     * 发送数据
     *
     * @return boolean
     */
    public void sendData(String[] parm) {
        Log.i(tag, "sendData");
        if (parm.length < 2) {
            return;
        }
        final String inOpCode = parm[0], inMsg = parm[1];
        if (!BUtility.isNumeric(inOpCode)) {
            return;
        }
        String funcTemp = null;
        if (parm.length == 3) {
            funcTemp = parm[2];
        }
        final String funcId = funcTemp;
        final EUExSocket object = objectMap.get(Integer.parseInt(inOpCode));

        new Thread(new Runnable() {

            @Override
            public void run() {
                if (object != null) {
                    boolean result = object.sendData(inMsg);
                    if (checkSetting() && result) {
                        if (null != funcId) {
                            callbackToJs(Integer.parseInt(funcId), false, EUExCallback.F_C_SUCCESS);
                        } else {
                            jsCallback(F_CALLBACK_NAME_SENDDATA,
                                    Integer.parseInt(inOpCode),
                                    EUExCallback.F_C_INT, EUExCallback.F_C_SUCCESS);
                        }
                    } else {
                        if (null != funcId) {
                            callbackToJs(Integer.parseInt(funcId), false, EUExCallback.F_C_FAILED);
                        } else {
                            jsCallback(F_CALLBACK_NAME_SENDDATA,
                                    Integer.parseInt(inOpCode),
                                    EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
                        }
                    }
                } else {
                    if (null != funcId) {
                        callbackToJs(Integer.parseInt(funcId), false, EUExCallback.F_C_FAILED);
                    } else {
                        jsCallback(F_CALLBACK_NAME_SENDDATA,
                                Integer.parseInt(inOpCode), EUExCallback.F_C_INT,
                                EUExCallback.F_C_FAILED);
                    }
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

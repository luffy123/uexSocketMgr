package org.zywx.wbpalmstar.plugin.uexsocketmgr;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

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

    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String DATA = "data";
    public static final String TIMEOUT = "timeout";


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
     * 4.0接口
     */
    public String createUDP(String[] parm) {
        int port = 0;
        int dataType = 0;

        try {
            JSONObject jsonObject = new JSONObject(parm[0]);
            port = jsonObject.getInt("port");
            dataType = jsonObject.optInt("dataType", 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //创建udp 必须要加端口号
        if (port == 0) {
            return null;
        }
        String udpOnDataCallback = parm[1];
        setCharset(String.valueOf(dataType));
        int inOpCode = getRandomId();
        if (objectMap.containsKey(inOpCode)
                || !checkSetting()) {
            jsCallback(F_CALLBACK_NAME_CREATEUDPSOCKET,
                    inOpCode, EUExCallback.F_C_INT,
                    EUExCallback.F_C_FAILED);
            return null;
        }
        //端口号验证
        Set<Integer> keySet = objectMap.keySet();
        for(Integer integer : keySet) {
            EUExSocket euExSocket = objectMap.get(integer);
            MulticastSocket socket =  euExSocket.getUDPSocket();
            if(socket != null && socket.getLocalPort() == port) {
                return null;
            }
        }

        EUExSocket socket = new EUExSocket(F_TYEP_UDP,
                port, this,inOpCode,
                dataType, mContext);
        if (null == socket.getUDPSocket()) {
            return null;
        }
        socket.udpOnDataCallback = udpOnDataCallback;
        objectMap.put(inOpCode, socket);
        return String.valueOf(inOpCode);
    }

    /**
     * 发送UDP信息
     * @param params
     */
    public void send(String [] params) {
        if (params.length < 3) {
            return;
        }
        final String funcId = params[2];
        try {
            int id = Integer.parseInt(params[0]);
            JSONObject config = new JSONObject(params[1]);
            String host = config.getString(HOST);
            String port = config.getString(PORT);
            final String data = config.getString(DATA);
            String timeout = config.optString(TIMEOUT, "0");
            final EUExSocket socket = objectMap.get(id);
            socket.setTimeOut(timeout);
            socket.setInetAddressAndPort(host, port, false);
            new Thread(new Runnable() {

                @Override
                public void run() {
                    if (socket != null) {
                        boolean result = socket.sendData(data);
                        if (checkSetting() && result) {
                            callbackToJs(Integer.parseInt(funcId), false, EUExCallback.F_C_SUCCESS);
                        } else {
                            callbackToJs(Integer.parseInt(funcId), false, EUExCallback.F_C_FAILED);
                        }
                    } else {
                        callbackToJs(Integer.parseInt(funcId), false, EUExCallback.F_C_FAILED);
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                callbackToJs(Integer.parseInt(funcId), false, EUExCallback.F_C_FAILED);
            } catch (NumberFormatException e2) {
                e2.printStackTrace();
            }
        }
    }

    /**
     * TCP对象连接服务器
     *
     * @param params
     */
    public void connect(String [] params) {
        if (params.length < 3) {
            return;
        }

            final String funcId = params[2];
            try {
                int id = Integer.parseInt(params[0]);
                JSONObject config = new JSONObject(params[1]);
                final String host = config.getString(HOST);
                final String port = config.getString(PORT);
                final int timeout = config.optInt(TIMEOUT, 30 * 1000);
                final EUExSocket socket = objectMap.get(id);
                //对于TCP在设置address和 port的时候会连接
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            socket.getTCPsocket().connect(new InetSocketAddress(host, Integer.parseInt(port)), timeout);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (socket.getTCPsocket().isConnected()) {
                            if (BUtility.isNumeric(socket.tcpOnDataCallback)) {
                                callbackToJs(Integer.parseInt(socket.tcpOnDataCallback), true, EUExCallback.F_C_SUCCESS);
                            }
                            if (BUtility.isNumeric(funcId)) {
                                callbackToJs(Integer.parseInt(funcId), false, EUExCallback.F_C_SUCCESS);
                            }
                            socket.onMessage(1);//对于TCP服务 onMessage 的 type 为1
                        } else {
                            if (BUtility.isNumeric(socket.tcpOnStatusCallback)) {
                                callbackToJs(Integer.parseInt(socket.tcpOnStatusCallback), false, EUExCallback.F_C_FAILED);
                            }
                            if (BUtility.isNumeric(funcId)) {
                                callbackToJs(Integer.parseInt(funcId), false, EUExCallback.F_C_FAILED);
                            }
                        }
                    }
                }.start();
            } catch (JSONException e) {
                e.printStackTrace();
                callbackToJs(Integer.parseInt(funcId), false, EUExCallback.F_C_FAILED);
            }

    }

    public void write(String [] params) {
        if(params.length < 3) {
            return;
        }
        final String funcId = params[2];

        try {
            int id = Integer.parseInt(params[0]);
            JSONObject config = new JSONObject(params[1]);
            final String data = config.getString(DATA);
            final String timeout = config.optString(TIMEOUT, String.valueOf(50 * 1000));
            final EUExSocket socket = objectMap.get(id);
            new Thread(new Runnable() {

                @Override
                public void run() {
                if (socket != null) {
                    try {
                        socket.getTCPsocket().setSoTimeout(Integer.parseInt(timeout));
                        if (!socket.getTCPsocket().isConnected()) {
                            if (BUtility.isNumeric(socket.tcpOnStatusCallback)) {
                                callbackToJs(Integer.parseInt(socket.tcpOnStatusCallback), false, 1);//连接已断开
                            }
                            return;
                        }
                        boolean result = socket.sendData(data);
                        if (checkSetting() && result) {
                            if (BUtility.isNumeric(funcId)) {
                                callbackToJs(Integer.parseInt(funcId), false, EUExCallback.F_C_SUCCESS);
                            }
                        } else {
                            if (BUtility.isNumeric(funcId)) {
                                callbackToJs(Integer.parseInt(funcId), false, EUExCallback.F_C_FAILED);
                            }
                        }
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (BUtility.isNumeric(funcId)) {
                        callbackToJs(Integer.parseInt(funcId), false, EUExCallback.F_C_FAILED);
                    }
                }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                callbackToJs(Integer.parseInt(funcId), false, EUExCallback.F_C_FAILED);
            } catch (NumberFormatException e2) {
                e2.printStackTrace();
            }
        }

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

    /**
     * 4.0接口
     */
    public String createTCP(String[] parm) {
        if (parm.length < 3) {
            return null;
        }
        int dataType = 0;
        try {
            JSONObject jsonObject = new JSONObject(parm[0]);
            dataType = jsonObject.optInt("dataType", 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String tcpOnStatusCallback = parm[1];
        String tcpOnDataCallback = parm[2];

        setCharset(String.valueOf(dataType));
        int inOpCode = getRandomId();
        if (objectMap.containsKey(inOpCode)
                || !checkSetting()) {
            jsCallback(F_CALLBACK_NAME_CREATETCPSOCKET,
                    inOpCode, EUExCallback.F_C_INT,
                    EUExCallback.F_C_FAILED);
            return null;
        }
        EUExSocket socket = new EUExSocket(F_TYEP_TCP, 0,
                this, inOpCode, dataType);
        if (null == socket.getTCPsocket()) {
            return null;
        }
        socket.tcpOnStatusCallback = tcpOnStatusCallback;
        socket.tcpOnDataCallback = tcpOnDataCallback;
        objectMap.put(inOpCode, socket);
        return String.valueOf(inOpCode);
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

    public void close(String []params) {
        if (params.length < 1) {
            return;
        }
        String inOpCode = params[0];
        if (!BUtility.isNumeric(inOpCode)) {
            return;
        }
        int callbackFuncId = -1;
        int flag = 0;
        if (params.length == 2) {
            //如果第二个参数是回调
            if (BUtility.isNumeric(params[1])) {
                callbackFuncId = Integer.parseInt(params[1]);
            } else {
                try {
                    JSONObject jsonObject = new JSONObject(params[1]);
                    flag = jsonObject.optInt("flag", 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (params.length == 3) {
            try {
                JSONObject jsonObject = new JSONObject(params[1]);
                flag = jsonObject.optInt("flag", 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            callbackFuncId = Integer.parseInt(params[2]);
        }
        EUExSocket object = objectMap.remove(Integer.parseInt(inOpCode));
        if (object != null) {
            if(flag == 0) {
                object.onClose();
                if(object.close()) {
                    callbackToJs(callbackFuncId, false, EUExCallback.F_C_SUCCESS);
                } else {
                    callbackToJs(callbackFuncId, false, EUExCallback.F_C_FAILED);
                }
            } else {
                object.onClose();
                callbackToJs(callbackFuncId, false, EUExCallback.F_C_SUCCESS);
            }
        } else {
            callbackToJs(callbackFuncId, false, EUExCallback.F_C_FAILED);
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

    public static int getRandomId() {
        return (int)(Math.random() * 100000);
    }

}

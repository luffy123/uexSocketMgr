package org.zywx.wbpalmstar.plugin.uexsocketmgr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.SocketException;

import org.apache.http.util.EncodingUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

public class EUExSocket {
	private int m_type;
	private MulticastSocket m_UDPSocket;
	private Socket m_TCPsocket;
	private InetAddress m_UDPRemoteAddress;
	private int m_UDPRemotePort;
	private InputStream m_inputStream;
	private ByteArrayOutputStream m_bytestream;
	private OutputStream m_bufferedWriter;

	private EUExSocketMgr m_socketMgr;
	private CheckSocketTask m_checkSocketTask;
	private boolean m_stop = false;
	private int m_opCode;
	static final String SCRIPT_HEADER = "javascript:";
	private int mTimeOut = -1;
	private int dataType = 0;
	private static final String HOST_TAG = "host";
	private static final String PORT_TAG = "port";
	private static final String DATA_TAG = "data";

	public EUExSocket(int inType, int inLocalPort, EUExSocketMgr socketMgr,
			int inOpCode, int inDataType) {
		m_type = inType;
		m_opCode = inOpCode;
		m_socketMgr = socketMgr;
		dataType = inDataType;
		if (m_type == EUExSocketMgr.F_TYEP_TCP) {

			try {
				m_TCPsocket = new Socket();
				m_TCPsocket.setKeepAlive(true);
				m_socketMgr.jsCallback(
						EUExSocketMgr.F_CALLBACK_NAME_CREATETCPSOCKET,
						m_opCode, EUExCallback.F_C_INT,
						EUExCallback.F_C_SUCCESS);
			} catch (SocketException e) {
				m_socketMgr
						.jsCallback(
								EUExSocketMgr.F_CALLBACK_NAME_CREATETCPSOCKET,
								m_opCode, EUExCallback.F_C_INT,
								EUExCallback.F_C_FAILED);
				e.printStackTrace();
			}
		} else if (m_type == EUExSocketMgr.F_TYEP_UDP) {
			try {
				if (inLocalPort == 0) {
					m_UDPSocket = new MulticastSocket();
				} else {
					m_UDPSocket = new MulticastSocket(inLocalPort);
				}
				m_socketMgr.jsCallback(
						EUExSocketMgr.F_CALLBACK_NAME_CREATEUDPSOCKET,
						m_opCode, EUExCallback.F_C_INT,
						EUExCallback.F_C_SUCCESS);

			} catch (SocketException e) {
				m_socketMgr
						.jsCallback(
								EUExSocketMgr.F_CALLBACK_NAME_CREATEUDPSOCKET,
								m_opCode, EUExCallback.F_C_INT,
								EUExCallback.F_C_FAILED);
				e.printStackTrace();
			} catch (IOException e) {
				m_socketMgr
						.jsCallback(
								EUExSocketMgr.F_CALLBACK_NAME_CREATEUDPSOCKET,
								m_opCode, EUExCallback.F_C_INT,
								EUExCallback.F_C_FAILED);
				e.printStackTrace();
			}
			onMessage(0);
		} else {

		}

	}

	public EUExSocket(int inType, int inLocalPort, EUExSocketMgr socketMgr,
			int inOpCode, int inDataType, Context context) {
		this(inType, inLocalPort, socketMgr, inOpCode, inDataType);
	}

	/**
	 * 关闭Socket
	 * 
	 * @return boolean
	 */
	protected boolean close() {
		if (m_type == EUExSocketMgr.F_TYEP_TCP) {
			if (m_TCPsocket != null) {
				try {
					m_TCPsocket.shutdownInput();
					m_TCPsocket.close();
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}
				m_TCPsocket = null;
			}
			if (m_bufferedWriter != null) {
				try {
					m_bufferedWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				m_bufferedWriter = null;
			}
			if (m_inputStream != null) {
				try {
					m_inputStream.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				m_inputStream = null;
			}
			if (m_bytestream != null) {
				try {
					m_bytestream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				m_bytestream = null;
			}
		} else if (m_type == EUExSocketMgr.F_TYEP_UDP) {
			if (m_UDPSocket != null) {
				m_UDPSocket.close();
				m_UDPSocket = null;
			}
		}

		return true;
	}

	/**
	 * 设置 Socket 超时
	 * 
	 * @param timeOut
	 * @return boolean
	 */
	protected boolean setTimeOut(String timeOut) {
		mTimeOut = Integer.parseInt(timeOut);
		return true;
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
	protected boolean setInetAddressAndPort(String inRemoteAddress,
			String inRemotePort) {
		try {
			m_UDPRemoteAddress = InetAddress.getByName(inRemoteAddress);
			m_UDPRemotePort = Integer.parseInt(inRemotePort);

			if (m_type == EUExSocketMgr.F_TYEP_TCP) {
				if (mTimeOut == -1) {
					m_TCPsocket.connect(new InetSocketAddress(
							m_UDPRemoteAddress.getHostAddress(),
							m_UDPRemotePort));
				} else {
					m_TCPsocket.connect(new InetSocketAddress(
							m_UDPRemoteAddress.getHostAddress(),
							m_UDPRemotePort), mTimeOut);
				}

				m_socketMgr.jsCallback(EUExSocketMgr.F_CALLBACK_NAME_CONNECTED,
						m_opCode, EUExCallback.F_C_INT,
						EUExCallback.F_C_SUCCESS);
			} else if (m_socketMgr.checkSetting()) {
				m_socketMgr.jsCallback(EUExSocketMgr.F_CALLBACK_NAME_CONNECTED,
						m_opCode, EUExCallback.F_C_INT,
						EUExCallback.F_C_SUCCESS);
			} else {
				m_socketMgr
						.jsCallback(EUExSocketMgr.F_CALLBACK_NAME_CONNECTED,
								m_opCode, EUExCallback.F_C_INT,
								EUExCallback.F_C_FAILED);
			}

		} catch (Exception e) {
			onClose();
			close();
			m_socketMgr.jsCallback(EUExSocketMgr.F_CALLBACK_NAME_CONNECTED,
					m_opCode, EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
			return false;
		}
		onMessage(1);
		return true;
	}

	/**
	 * 发送数据
	 * 
	 * @param msg
	 *            内容
	 * @return boolean
	 */
	protected boolean sendData(String msg) {
		if (m_type == EUExSocketMgr.F_TYEP_TCP) {

			try {
				if (m_bufferedWriter == null) {
					m_bufferedWriter = m_TCPsocket.getOutputStream();
				}
				byte[] data = null;
				if (dataType == 0) {
					data = msg.getBytes("utf-8");
				} else {
					data = Base64.decode(msg);
				}
				m_bufferedWriter.write(data);

			} catch (Exception e) {

				onClose();
				close();
				e.printStackTrace();
				return false;
			}
		} else {
			byte[] data;
			try {
				if (dataType == 0) {
					data = msg.getBytes("utf-8");
				} else {
					data = Base64.decode(msg);
				}
				if (m_UDPRemoteAddress != null) {
					if (m_UDPRemoteAddress.isMulticastAddress()
							&& m_UDPSocket != null) {
						Log.i("Socket", "it is a multicastAddress "
								+ m_UDPRemoteAddress.getHostAddress());
						m_UDPSocket.setTimeToLive(254);
						m_UDPSocket.setLoopbackMode(true);
						m_UDPSocket.joinGroup(m_UDPRemoteAddress);
					}
					DatagramPacket sendPacket = new DatagramPacket(data,
							data.length, m_UDPRemoteAddress, m_UDPRemotePort);
					Log.i("socket",
							"ip: " + m_UDPRemoteAddress.getHostAddress()
									+ " port: " + m_UDPRemotePort);
					m_UDPSocket.send(sendPacket);
				}
			} catch (Exception e) {
				onClose();
				close();
				e.printStackTrace();
				return false;
			}

		}

		return true;
	}

	/**
	 * 得到 对方的数据
	 * 
	 * @param enc
	 *            编码
	 * @return 内容
	 */
	protected String getData(String enc) {

		try {
			if (m_TCPsocket != null) {
				if (m_TCPsocket.isClosed()) {
					return null;
				}
				if (m_inputStream == null) {
					m_inputStream = m_TCPsocket.getInputStream();
					m_bytestream = new ByteArrayOutputStream();
				}

				int ch;

				while ((ch = m_inputStream.read()) != -1) {
					m_bytestream.write(ch);
					if (m_inputStream.available() == 0) {
						break;
					}

				}
				byte[] getByte = m_bytestream.toByteArray();
				m_bytestream.reset();
				String s = "";
				if (dataType == 0) {
					s = EncodingUtils.getString(getByte, "UTF-8");
				} else {

					s = Base64.encode(getByte);
				}
				return generateJsonData("", "", s);

			} else {
				if (m_UDPSocket != null) {
					byte[] buf = new byte[10240];
					DatagramPacket rePacket = new DatagramPacket(buf, 10240);
					m_UDPSocket.receive(rePacket);
	                String host = rePacket.getAddress().toString();
	                if(host.contains("/")){
	                    host = host.replace("/", "");
	                }
	                String port = String.valueOf(rePacket.getPort());
					String getData = null;
					byte[] b = new byte[1];
					if (dataType == 0) {
						getData = new String(rePacket.getData(), enc)
								.replaceAll(String.valueOf(((char) b[0])), "");
					} else {
						getData = Base64.encode(rePacket.getData()).replaceAll(
								String.valueOf(((char) b[0])), "");
					}
					String s = getData;
					return generateJsonData(host, port, s);
				}
			}
		} catch (Exception e) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException el) {
				el.printStackTrace();
			}
			String js = SCRIPT_HEADER + "if("
					+ EUExSocketMgr.F_CALLBACK_NAME_DISCONNECTED + "){"
					+ EUExSocketMgr.F_CALLBACK_NAME_DISCONNECTED + "("
					+ m_opCode + ")}";
			m_socketMgr.onCallback(js);
			onClose();
			close();
			e.printStackTrace();
		}
		return null;
	}

	private String generateJsonData(String host, String port, String s) {
	    JSONObject json = new JSONObject();
	    try {
            json.put(HOST_TAG, host);
            json.put(PORT_TAG, port);
            json.put(DATA_TAG, s);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        return json.toString();
    }

    protected void onMessage(int type) {
		m_stop = false;
		if (m_checkSocketTask == null) {

			m_checkSocketTask = new CheckSocketTask();
			if (m_type == EUExSocketMgr.F_TYEP_TCP) {
				if (type == 1) {
					m_checkSocketTask.execute();
				}
			} else {
				if (type == 0) {
					m_checkSocketTask.execute();
				}

			}

		}

	}

	protected void onClose() {
		m_stop = true;
		if (m_checkSocketTask != null) {
			m_checkSocketTask.cancel(true);
			m_checkSocketTask = null;
		}

	}

	protected class CheckSocketTask extends AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub

			Log.i("socket", "start receive thread");
			while (!m_stop) {
				Log.i("socket", "start receive");
				String data = getData("utf-8");
				Log.i("socket", "received==" + data);
				if (!TextUtils.isEmpty(data)) {
					String js = SCRIPT_HEADER + "if("
							+ EUExSocketMgr.F_CALLBACK_NAME_SOCKETDATA + "){"
							+ EUExSocketMgr.F_CALLBACK_NAME_SOCKETDATA + "("
							+ m_opCode + ",'" + BUtility.transcoding(data)
							+ "')}";
					m_socketMgr.onCallback(js);
				} else {
					String js = SCRIPT_HEADER + "if("
							+ EUExSocketMgr.F_CALLBACK_NAME_DISCONNECTED + "){"
							+ EUExSocketMgr.F_CALLBACK_NAME_DISCONNECTED + "("
							+ m_opCode + ")}";
					// getData方法中捕捉到异常时已经回调了
					onClose();
					close();
					return null;
				}
			}

			return null;
		}

	}

}

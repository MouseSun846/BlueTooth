package com.example.bluetooth.BlueToothManage.BluetoothChat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bluetooth.R;

/**
 * @description:此类为蓝牙调试助手主界面 接收发送蓝牙消息、建立蓝牙通信连接、打印蓝牙消息
 * @author：zzq
 * @time: 2016-8-5 上午11:23:29
 */
public class BluetoothChatActivity extends Activity implements OnClickListener {

	// 从BluetoothChatService发送处理程序的消息类型
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;
	private static boolean flag=true;

	private TextView mTitle;
	private SeekBar mSlide;
	private Button mPlusButton;
	private Button mMinusButton;
	private Button mStart;
	private Button mEnd;
	private Button mSetting;
	private RadioGroup radioGroup;
	private TextView mtvValue;
	private int stepValues;
	private int valueDegree=0;

	// 连接设备的名称
	private String mConnectedDeviceName = null;

	// 本地蓝牙适配器
	private BluetoothAdapter mBluetoothAdapter = null;
	// 成员对象聊天服务
	private BluetoothChatService mChatService = null;
	private Button btn_connect, btn_discover;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		radioGroup = findViewById(R.id.stepValue);
		mTitle = (TextView) findViewById(R.id.title_left_text);
		mTitle.setText(R.string.app_name);
		mTitle = (TextView) findViewById(R.id.title_right_text);
		btn_connect = (Button) findViewById(R.id.btn_connect);
		btn_discover = (Button) findViewById(R.id.btn_discover);
		btn_connect.setOnClickListener(this);
		btn_discover.setOnClickListener(this);
		mtvValue=findViewById(R.id.tv_value);
		// 获取本地蓝牙适配器
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// 判断蓝牙是否可用
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "蓝牙是不可用的", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				RadioButton radioButton = findViewById(checkedId);
				stepValues = Integer.parseInt((String) radioButton.getText());
			}
		});
		mSlide = findViewById(R.id.slideValue);
		mSlide.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				valueDegree = progress;
				sendMessage(valueDegree);
				mtvValue.setText(""+progress);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});
		mStart = findViewById(R.id.button_start);
		mEnd = findViewById(R.id.button_end);
		mStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mtvValue.setText("1");
				sendMessage(500);
				sendMessage(1);
				mSlide.setProgress(1);
				valueDegree=1;
			}
		});
		mEnd.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SharedPreferences sp = getApplicationContext().getSharedPreferences("setting", Context.MODE_PRIVATE);
				valueDegree=sp.getInt("setvalue",0);
				mSlide.setProgress(valueDegree);
				mtvValue.setText(""+valueDegree);
				sendMessage(valueDegree);
			}
		});
		mSetting = findViewById(R.id.button_set);
		mSetting.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//默认的操作 方式
				SharedPreferences sp = getApplicationContext().getSharedPreferences("setting", Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = sp.edit();
				editor.putInt("setvalue",valueDegree);
				editor.commit();
			}
		});
	}

	@Override
	public void onStart() {
		super.onStart();
		// 判断蓝牙是否打开，，没打开则弹出蓝牙提示打开蓝牙对话框
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		} else {
			if (mChatService == null) {
				LogUtils.getInstance().e(getClass(), "----进行蓝牙相关设置---");
				setupChat();
			}
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		LogUtils.getInstance().e(getClass(), "----onResume()");
		if (mChatService != null) {
			if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
				mChatService.startChat();
			}
		}
	}

	/**
	 * 聊天需要的一些设置
	 */
	private void setupChat() {
		mPlusButton = (Button) findViewById(R.id.button_send);
		mMinusButton = (Button) findViewById(R.id.button_clear);
		mPlusButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (valueDegree>2500){
					valueDegree = 2500;
				}else {
					valueDegree+=stepValues;
					if (valueDegree>2500){
						valueDegree=2500;
					}
				}
				mSlide.setProgress(valueDegree);
				mtvValue.setText(""+valueDegree);
				sendMessage(valueDegree);
			}
		});
		mMinusButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (valueDegree<1){
					valueDegree=1;
				}else {
					valueDegree-=stepValues;
					if (valueDegree<1){
						valueDegree=1;
					}
				}
				mSlide.setProgress(valueDegree);
				sendMessage(valueDegree);
			}
		});
		// 初始化BluetoothChatService进行蓝牙连接
		mChatService = new BluetoothChatService(this, mHandler);
	}

	//获取输入框十六进制格式
	private String getHexString(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (('0' <= c && c <= '9') || ('a' <= c && c <= 'f') ||
					('A' <= c && c <= 'F')) {
				sb.append(c);
			}
		}
		if ((sb.length() % 2) != 0) {
			sb.deleteCharAt(sb.length());
		}
		return sb.toString();
	}
	private byte[] stringToBytes(String s) {
		byte[] buf = new byte[s.length() / 2];
		for (int i = 0; i < buf.length; i++) {
			try {
				buf[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		return buf;
	}
	/**
	 * 从字节数组到十六进制字符串转换
	 */
	public static String bytes2Hex(byte[] b) {
		String ret = "";
		for (int i = 0; i < b.length; i++) {
			String hex = Integer.toHexString(b[i] & 0xFF);
			if (hex.length() == 1) {
				hex = '0' + hex;
			}
			ret += hex.toUpperCase();
		}
		return ret;
	}
	/**
	 * 发送消息
	 *
	 *            发送的内容
	 */
	private void sendMessage(int value) {
		if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		}
		String str;
		str="55 55 08 03 01 e8 03 01"+" "+String.format("%02x",value%256)+" "+String.format("%02x",value/256);
		mChatService.write(stringToBytes(getHexString(str)));

	}

	// 此Handler处理BluetoothChatService传来的消息
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				LogUtils.getInstance().e(getClass(),
						"MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED:
					mTitle.setText(R.string.devoice_connected_to);
					mTitle.append(mConnectedDeviceName);
					break;
				case BluetoothChatService.STATE_CONNECTING:
					mTitle.setText(R.string.devoice_connecting);
					break;
				case BluetoothChatService.STATE_LISTEN:
				case BluetoothChatService.STATE_NONE:
					mTitle.setText(R.string.devoice_not_connected);
					break;
				}
				break;
			case MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				String writeMessage = new String(writeBuf);

				break;
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;

				break;
			case MESSAGE_DEVICE_NAME:
				// 保存连接设备的名字
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"连接到" + mConnectedDeviceName, Toast.LENGTH_SHORT)
						.show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		LogUtils.getInstance().e(getClass(), "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// 当DeviceListActivity返回与设备连接的消息
			if (resultCode == Activity.RESULT_OK) {
				// 连接设备的MAC地址
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// 得到蓝牙对象
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(address);
				// 开始连接设备
				mChatService.connect(device);
			}
			break;
		case REQUEST_ENABLE_BT:
			// 判断蓝牙是否启用
			if (resultCode == Activity.RESULT_OK) {
				// 建立连接
				setupChat();
			} else {
				LogUtils.getInstance().e(getClass(), "蓝牙未启用");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_connect:
			// 连接设备
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			break;
		case R.id.btn_discover:
			// 允许被发现设备
			ensureDiscoverable();
			break;

		default:
			break;
		}
	}

	/**
	 * 允许设备被搜索
	 */
	private void ensureDiscoverable() {
		LogUtils.getInstance().e(getClass(), "----允许被搜索");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
		LogUtils.getInstance().e(getClass(), "----onPause()");
	}

	@Override
	public void onStop() {
		super.onStop();
		LogUtils.getInstance().e(getClass(), "----onStop()");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// 停止蓝牙
		if (mChatService != null)
			mChatService.stop();
		LogUtils.getInstance().e(getClass(), "----onDestroy()");
	}

}
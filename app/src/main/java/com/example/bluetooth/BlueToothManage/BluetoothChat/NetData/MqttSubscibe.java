package com.example.bluetooth.BlueToothManage.BluetoothChat.NetData;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

//通过MQTT订阅消息
public class MqttSubscibe {
    private Context context;
    MqttAndroidClient mqttAndroidClient;
    final String serverUri = "tcp://192.168.0.103:1883";
    String clientId = "robot";
    final String subscriptionTopic = "/joint_states_real";
    public RosInfo rosInfo; //Ros信息接收处理
    public MqttSubscibe(Context context) {
        this.context = context;
    }

    /**
     * 初始化MQTT
     */
    public void InitMqttInfo() {
        //新建client实例
        clientId = clientId + System.currentTimeMillis();
        mqttAndroidClient = new MqttAndroidClient(context, serverUri, clientId);
        //设置回调函数
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
            }

            @Override
            public void connectionLost(Throwable cause) {
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String res = new String(message.getPayload());
                rosInfo.getMessage(res);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });
        //连接设置，是否重连，是否清理session
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);

        try {
            //连接mqtt服务器
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    makeToast("连接成功");
                    Log.i("mouse","连接成功");
                    //订阅topic
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    makeToast("连接失败");
                    Log.i("mouse","连接失败！！！"+exception.getMessage());
                }
            });
        } catch (MqttException ex) {
            ex.printStackTrace();
        }
    }
    //订阅主题
    private void subscribeToTopic() {
        try {
            //开始订阅
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    makeToast("成功订阅到" + subscriptionTopic);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    makeToast("订阅失败" + subscriptionTopic);
                }
            });

        } catch (MqttException ex) {
            ex.printStackTrace();
        }
    }

    private void makeToast(String str) {
        Toast.makeText(context, str, Toast.LENGTH_LONG).show();
    }

}

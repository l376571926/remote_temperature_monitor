package com.huawei.dht11_monitor;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import com.huawei.dht11_monitor.databinding.ActivityMainBinding;
import com.socks.library.KLog;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    static final SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd hh:mm:ss", Locale.getDefault());

    private MqttAndroidClient mClient;
    private com.huawei.dht11_monitor.databinding.ActivityMainBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        MqttAndroidClient client1 = connectMqtt();
        client1.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                KLog.e();
                if (cause != null) {
                    Toast.makeText(MainActivity.this, "connectionLost: " + cause.toString(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String json = new String(message.getPayload());
                //topic = [outTopic],message = [{"temperature":27.60000038,"humidity":46}]
                KLog.e("topic = [" + topic + "],message = [" + json + "]");

                //dht11
                if ("outTopic".equals(topic)) {
                    //topic = [outTopic],message = [{"temperature":25.79999924,"humidity":84}]
                    JSONObject object = new JSONObject(json);
                    double temperature = object.getDouble("temperature");
                    double humidity = object.getDouble("humidity");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mBinding.temperatureTv.setText(temperature + "℃");
                            mBinding.humidityTv.setText(humidity + "%");
                            String dateTimeStr = format.format(Calendar.getInstance().getTime());
                            mBinding.updateTimeTv.setText(dateTimeStr);
                        }
                    });
                } else if ("outTopic/ds18b20_1".equals(topic)) {
                    JSONObject object = new JSONObject(json);
                    double temperature = object.getDouble("temperature");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mBinding.temperatureTv2.setText(temperature + "℃");
                            String dateTimeStr = format.format(Calendar.getInstance().getTime());
                            mBinding.updateTimeTv2.setText(dateTimeStr);
                        }
                    });
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });
    }

    private MqttAndroidClient connectMqtt() {
        String serverURI = "tcp://" + MqttUserInfo.HOST_IP + ":" + MqttUserInfo.PORT;
        String clientId = MqttUserInfo.CLIENT_ID;//设备id
        mClient = new MqttAndroidClient(this, serverURI, clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setUserName(MqttUserInfo.USERNAME);//产品id
        options.setPassword(MqttUserInfo.PASSWORD.toCharArray());//设备id鉴权信息

        try {
            IMqttToken token = mClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    KLog.e("mqtt server connect success");
                    try {
                        if (mClient != null) {
                            mClient.subscribe("outTopic", 0);
                            mClient.subscribe("outTopic/ds18b20_1", 0);
                            mClient.subscribe("outTopic/hello", 0);
                        }
                    } catch (MqttException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    KLog.e(exception.toString());
                }
            });
            KLog.e("connect token: " + token.toString());
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
        return mClient;
    }
}
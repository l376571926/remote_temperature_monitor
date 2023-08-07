package com.huawei.dht11_monitor;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import com.socks.library.KLog;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private MqttAndroidClient mClient;
    TextView temperatureTv;
    TextView humidityTv;
    TextView updateTimeTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        temperatureTv = (TextView) findViewById(R.id.temperature_tv);
        humidityTv = (TextView) findViewById(R.id.humidity_tv);
        updateTimeTv = (TextView) findViewById(R.id.update_time_tv);

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
                JSONObject object = new JSONObject(json);
                double temperature = object.getDouble("temperature");
                double humidity = object.getDouble("humidity");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        temperatureTv.setText(temperature + "℃");
                        humidityTv.setText(humidity + "%");
                        SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd hh:mm:ss", Locale.getDefault());
                        String dateTimeStr = format.format(Calendar.getInstance().getTime());
                        updateTimeTv.setText(dateTimeStr);
                    }
                });
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
            IMqttToken token = mClient.connect(options, null, connectListener);
            KLog.e("connect token: " + token.toString());
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
        return mClient;
    }

    private IMqttActionListener connectListener = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            KLog.e("mqtt server connect success");
            try {
                if (mClient != null) {
                    IMqttToken subscribe = mClient.subscribe("outTopic", 0);
                    KLog.e("subscribe response ret: " + subscribe.getMessageId());
                }
            } catch (MqttException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            KLog.e(exception.toString());
        }
    };
}
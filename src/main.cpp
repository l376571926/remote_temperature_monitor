/**
 * 本demo基于
 * PubSubClient
 * DHT11
 * ArduinoJson
 *
 */
#include <Arduino.h>
#include <mqtt_device_info.h>
#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include <DHT.h>
#include <OneWire.h>
#include <DallasTemperature.h>

const char *ssid = "HUAWEI_57E0";
const char *password = "lyw97461476";

const char *mqtt_server = "183.230.40.39";

WiFiClient espClient;
PubSubClient client(espClient);
unsigned long lastMsg = 0;
#define MSG_BUFFER_SIZE    (50)
char msg[MSG_BUFFER_SIZE];

#define DHTPIN 2
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);

#define ONE_WIRE_BUS 2
OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature sensors(&oneWire);
DeviceAddress insideThermometer;

void setup_wifi() {

    delay(10);
    // We start by connecting to a WiFi network
    Serial.println();
    Serial.print("Connecting to ");
    Serial.println(ssid);

    WiFi.mode(WIFI_STA);
    WiFi.begin(ssid, password);

    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }

    randomSeed(micros());

    Serial.println("");
    Serial.println("WiFi connected");
    Serial.println("IP address: ");
    Serial.println(WiFi.localIP());
}

void reconnect() {
    // Loop until we're reconnected
    while (!client.connected()) {
        Serial.print("Attempting MQTT connection...");
        String clientId = DEVICE_INFO_CLIENT_ID;
        String user = DEVICE_INFO_USER;
        String pass = DEVICE_INFO_PASSWORD;
        // Attempt to connect
        if (client.connect(clientId.c_str(), user.c_str(), pass.c_str())) {
            Serial.println("connected");
            // Once connected, publish an announcement...
//            client.publish("outTopic", "hello world");
        } else {
            Serial.print("failed, rc=");
            Serial.print(client.state());
            Serial.println(" try again in 5 seconds");
            // Wait 5 seconds before retrying
            delay(5000);
        }
    }
}

void setup() {
    delay(5000);
    // write your initialization code here
    Serial.begin(115200);
    setup_wifi();
    client.setServer(mqtt_server, 6002);

    if (IOT_DEVICE_TYPE == IOT_DEVICE_TYPE_DHT11) {
        dht.begin();
    } else if (IOT_DEVICE_TYPE == IOT_DEVICE_TYPE_DS18B20) {
        sensors.begin();
        if (!sensors.getAddress(insideThermometer, 0)) Serial.println("Unable to find address for Device 0");
        // resolution: 9~12
        sensors.setResolution(insideThermometer, 12);
    }
}

void loop() {
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("WiFi.status() WiFi not connected!");
        delay(5000);
        ESP.restart();
        return;
    }

    // write your code here
    if (!client.connected()) {
        reconnect();
    }
    client.loop();

    unsigned long now = millis();
    if (now - lastMsg > 2000) {
        lastMsg = now;

        if (IOT_DEVICE_TYPE == IOT_DEVICE_TYPE_DHT11) {
            float h = dht.readHumidity();
            float t = dht.readTemperature();
            if (!isnan(h) && !isnan(t)) {
                StaticJsonDocument<200> doc;
                doc["temperature"] = t;
                doc["humidity"] = h;

                String output;
                serializeJson(doc, output);

                Serial.print("Publish message: ");
                Serial.println(output);
                client.publish(DEVICE_INFO_TOPIC, output.c_str());
            }
        } else if (IOT_DEVICE_TYPE == IOT_DEVICE_TYPE_DS18B20) {
            sensors.requestTemperatures();
            float tempC = sensors.getTempC(insideThermometer);
            if (tempC != DEVICE_DISCONNECTED_C) {
                StaticJsonDocument<200> doc;
                doc["temperature"] = tempC;

                String output;
                serializeJson(doc, output);

                Serial.print("Publish message: ");
                Serial.println(output);
                client.publish("outTopic/ds18b20_1", output.c_str());
            }
        }
    }
}
/**
 * 本demo基于
 * PubSubClient
 * DHT11
 * ArduinoJson
 *
 */
#include <Arduino.h>
#include <ESP8266WiFiMulti.h>
#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <DHT.h>
#include <ArduinoJson.h>

#define DHTPIN 2
#define DHTTYPE DHT11

// Update these with values suitable for your network.

const char *mqtt_server = "183.230.40.39";

ESP8266WiFiMulti wifiMulti;
// WiFi connect timeout per AP. Increase when connecting takes longer.
const uint32_t connectTimeoutMs = 2000;

WiFiClient espClient;
PubSubClient client(espClient);
unsigned long lastMsg = 0;
#define MSG_BUFFER_SIZE    (50)
char msg[MSG_BUFFER_SIZE];

DHT dht(DHTPIN, DHTTYPE);

void setup_wifi() {
    // Don't save WiFi configuration in flash - optional
    WiFi.persistent(false);

    WiFi.mode(WIFI_STA);

    // Register multi WiFi networks
    wifiMulti.addAP("liyiwei_test", "88888888");
    wifiMulti.addAP("HUAWEI P30 Pro", "88888888");
    wifiMulti.addAP("HUAWEI_57E0", "88888888");
    wifiMulti.addAP("TP-LINK_74DE", "88888888");
    // More is possible

    // Maintain WiFi connection
    if (wifiMulti.run(connectTimeoutMs) == WL_CONNECTED) {
        Serial.print("WiFi connected: ");
        Serial.print(WiFi.SSID());
        Serial.print(" IP address: ");
        Serial.println(WiFi.localIP());
    } else {
        Serial.print(".");
    }

    randomSeed(micros());
}

void reconnect() {
    // Loop until we're reconnected
    while (!client.connected()) {
        Serial.print("Attempting MQTT connection...");
        // Create a random client ID
        String clientId = "1120624586";
        // String clientId = "ESP8266Client-";
        // clientId += String(random(0xffff), HEX);
        String user = "604861";
        String pass = "123456dht11";
        // Attempt to connect
        if (client.connect(clientId.c_str(), user.c_str(), pass.c_str())) {
            Serial.println("connected");
            // Once connected, publish an announcement...
            client.publish("outTopic", "hello world");
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
    // write your initialization code here
    Serial.begin(115200);
    setup_wifi();
    client.setServer(mqtt_server, 6002);

    dht.begin();
}

boolean shouldShow = false;

void loop() {
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("WiFi.status() WiFi not connected!");
        wifiMulti.run(connectTimeoutMs);
        shouldShow = true;
        return;
    }
    if (shouldShow) {
        shouldShow = false;
        Serial.print("WiFi connected: ");
        Serial.print(WiFi.SSID());
        Serial.print(" ");
        Serial.println(WiFi.localIP());
    }

    // write your code here
    if (!client.connected()) {
        reconnect();
    }
    client.loop();


    unsigned long now = millis();
    if (now - lastMsg > 2000) {
        lastMsg = now;

        // Reading temperature or humidity takes about 250 milliseconds!
        // Sensor readings may also be up to 2 seconds 'old' (its a very slow sensor)
        float h = dht.readHumidity();
        // Read temperature as Celsius (the default)
        float t = dht.readTemperature();

        // Check if any reads failed and exit early (to try again).
        if (isnan(h) || isnan(t)) {
            Serial.println(F("Failed to read from DHT sensor!"));
            return;
        }

        StaticJsonDocument<200> doc;
        doc["temperature"] = t;
        doc["humidity"] = h;

        String output;
        serializeJson(doc, output);

        Serial.print("Publish message: ");
        Serial.println(output);
        client.publish("outTopic", output.c_str());
    }
}
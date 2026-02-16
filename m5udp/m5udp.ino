#include "M5CoreS3.h"
#include <WiFi.h>
#include <WiFiUdp.h>
#include <esp_wifi.h> 


const char* ssid     = "user";
const char* password = "wifi psw"; 
const char* dropletIP = "Droplet IP";
const int   port      = 1234;

WiFiUDP udp;

void setup() {
    auto cfg = M5.config();
    CoreS3.begin(cfg);
    
    
    WiFi.begin(ssid, password);
    CoreS3.Display.print("Connecting to WiFi");
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        CoreS3.Display.print(".");
    }
    CoreS3.Display.println("\nWiFi Connected!");

   
    WiFi.setSleep(false); 
    esp_wifi_set_max_tx_power(78); 
    CoreS3.Display.println("WiFi Turbo Mode: ON (Max Power)");

    
    if (CoreS3.Camera.begin()) {
        CoreS3.Camera.sensor->set_pixformat(CoreS3.Camera.sensor, PIXFORMAT_RGB565);
        CoreS3.Camera.sensor->set_framesize(CoreS3.Camera.sensor, FRAMESIZE_QVGA); // 320x240
        CoreS3.Display.println("Camera Ready: 320x240 RGB565");
    } else {
        CoreS3.Display.println("Camera Init Failed!");
    }
}

void loop() {
    if (CoreS3.Camera.get()) {
        uint8_t* fb_buf = CoreS3.Camera.fb->buf;
        size_t fb_len = CoreS3.Camera.fb->len; // ควรจะได้ 153,600 bytes

        
        uint8_t sync[] = {0xAA, 0xBB, 0xCC};
        udp.beginPacket(dropletIP, port);
        udp.write(sync, 3);
        udp.endPacket();

    
        for (size_t i = 0; i < fb_len; i += 1440) {
            size_t size = (fb_len - i < 1440) ? fb_len - i : 1440;
            udp.beginPacket(dropletIP, port);
            udp.write(fb_buf + i, size);
            udp.endPacket();
            
            
            delayMicroseconds(20);
        }

        CoreS3.Camera.free(); 
    }
}
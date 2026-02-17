# M5Stack-CoreS3-camera-streaming-an-Android
This project features a high-performance UDP video streaming system. It transmits raw GC0308 camera data (320x240 RGB565) from an M5Stack CoreS3 to an Android app via a Node.js relay . Optimized with bitwise color conversion , WiFi Turbo Mode , and frame synchronization  to ensure ultra-low latency.


🚀 M5Stack CoreS3 to Android UDP Streaming
A high-performance, ultra-low latency video streaming system that transmits raw camera data from an M5Stack CoreS3 to an Android device via a Node.js relay server.

🏗️ System Architecture
The system utilizes a three-tier architecture to bypass NAT issues and ensure the fastest possible delivery:
Sender (M5Stack CoreS3): Captures 320x240 RGB565 frames using the GC0308 sensor. Data is fragmented into 1440-byte packets to match standard MTU sizes and sent via UDP.
Relay (Node.js Server): A lightweight UDP proxy hosted on DigitalOcean. It listens for an ANDROID_READY heartbeat to map the destination and forwards incoming M5 data packets instantly.
Receiver (Android App): Reassembles packets using a synchronization header, performs bitwise color conversion, and renders the frame onto a SurfaceView .

🛠️ Key Configurations
Hardware & Connectivity
WiFi Turbo Mode: Disables WiFi sleep and sets maximum TX power on the ESP32 to prevent packet loss and frame artifacts.
Heartbeat Mechanism: The Android client sends a "READY" signal every 2 seconds to maintain the relay connection.
Performance Optimizations
Bitwise Conversion: Converts 16-bit RGB565 data to 32-bit ARGB8888 using direct bit-shifting to maximize FPS on the mobile side .
Extreme Buffering: Sets a 20MB receive buffer at the OS level on Android to handle high-burst UDP traffic without dropping frames.
Packet Syncing: Uses a unique 3-byte header (0xAA, 0xBB, 0xCC) to ensure the receiver always aligns with the start of a new frame.

🚀 Quick Start
Relay: Deploy relay.js to your DigitalOcean droplet and ensure port 1234 is open.
M5Stack: Update dropletIP with your server address and flash the firmware.
Android: Open the project in Android Studio, update the serverAddr, and run the app.

Cloud Setup Guide (DigitalOcean & UDP Relay)

# Installation Steps
**1. Execute these commands to set up the Node.js environment:**

```bash
sudo apt update
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs
```
**2. Firewall Setup**  
Run these commands to allow UDP traffic for the relay:

```bash
sudo ufw allow 1234/udp
sudo ufw enable
```
**3. Running the Relay Server**  
Use the relay.js file provided in the project folder:

**Create or edit the relay file**
```bash
nano relay.js
```

**Start the relay server**
```bash
node relay.js
```
**4. Troubleshooting**  
Port Conflict (EADDRINUSE): Occurs if another process is already using the port. Resolve by killing existing node processes:

```bash
sudo pkill -9 node
```
**Connection Timeout:**  
Ensure the dropletIP in the M5Stack code and Android app matches your server's public IP.

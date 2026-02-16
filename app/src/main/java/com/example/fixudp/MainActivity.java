package com.example.fixudp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {
    private SurfaceView surfaceView;
    private TextView statusText;
    private volatile boolean isRunning = true;
    private final int WIDTH = 320;
    private final int HEIGHT = 240;
    private final int TOTAL_BYTES = WIDTH * HEIGHT * 2;

    private int frameCount = 0;
    private long lastFpsTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surfaceView = findViewById(R.id.surfaceView);
        statusText = findViewById(R.id.statusText);
        startRawUdpReceiver();
    }

    private void startRawUdpReceiver() {
        new Thread(() -> {
            byte[] rawImageBuffer = new byte[TOTAL_BYTES];
            int[] pixels = new int[WIDTH * HEIGHT];
            Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
            int currentOffset = 0;
            boolean isSyncing = false;

            try (DatagramSocket socket = new DatagramSocket()) {
                // ขยาย Buffer รับข้อมูลให้ใหญ่ที่สุด
                socket.setReceiveBufferSize(25 * 1024 * 1024);
                InetAddress serverAddr = InetAddress.getByName("Droplet IP");

                // Heartbeat to Relay Server
                new Thread(() -> {
                    while (isRunning) {
                        try {
                            byte[] ready = "ANDROID_READY".getBytes();
                            socket.send(new DatagramPacket(ready, ready.length, serverAddr, 1234));
                            Thread.sleep(2000);
                        } catch (Exception e) { break; }
                    }
                }).start();

                byte[] packetData = new byte[8192];
                while (isRunning) {
                    DatagramPacket packet = new DatagramPacket(packetData, packetData.length);
                    socket.receive(packet);
                    int len = packet.getLength();


                    if (len == 3 && packetData[0] == (byte)0xAA && packetData[1] == (byte)0xBB && packetData[2] == (byte)0xCC) {
                        currentOffset = 0;
                        isSyncing = true;
                        continue;
                    }

                    if (isSyncing && currentOffset + len <= TOTAL_BYTES) {
                        System.arraycopy(packetData, 0, rawImageBuffer, currentOffset, len);
                        currentOffset += len;

                        if (currentOffset >= TOTAL_BYTES) {
                            
                            for (int i = 0; i < pixels.length; i++) {
                                int base = i << 1;
                                int b1 = rawImageBuffer[base] & 0xFF;
                                int b2 = rawImageBuffer[base + 1] & 0xFF;
                                int rgb = (b1 << 8) | b2;
                                int r = (rgb & 0xF800) >> 8;
                                int g = (rgb & 0x07E0) >> 3;
                                int b = (rgb & 0x001F) << 3;
                                pixels[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
                            }
                            bitmap.setPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT);
                            drawToSurface(bitmap);
                            updateFPS();
                            isSyncing = false;
                            currentOffset = 0;
                        }
                    }
                }
            } catch (Exception e) {
                runOnUiThread(() -> statusText.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    private void updateFPS() {
        frameCount++;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFpsTime >= 1000) {
            double fps = (frameCount * 1000.0) / (currentTime - lastFpsTime);
            runOnUiThread(() -> statusText.setText(String.format("FPS: %.1f | Turbo Mode", fps)));
            frameCount = 0;
            lastFpsTime = currentTime;
        }
    }

    private void drawToSurface(Bitmap bitmap) {
        SurfaceHolder holder = surfaceView.getHolder();
        Canvas canvas = holder.lockCanvas();
        if (canvas != null) {
            try {
                float scale = Math.min((float)canvas.getWidth()/WIDTH, (float)canvas.getHeight()/HEIGHT);
                Matrix matrix = new Matrix();
                matrix.postScale(scale, scale);
                matrix.postTranslate((canvas.getWidth() - WIDTH * scale) / 2, (canvas.getHeight() - HEIGHT * scale) / 2);
                canvas.drawBitmap(bitmap, matrix, null);
            } finally {
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
    }
}
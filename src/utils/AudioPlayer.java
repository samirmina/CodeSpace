package utils;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.*;

public class AudioPlayer {
    private static final String NOTIFICATION_FILE = "notification.wav";
    private static boolean wavGenerated = false;
    private static Thread audioThread;

    public static synchronized void playNotification() {
        if (audioThread != null && audioThread.isAlive()) return;
        audioThread = new Thread(() -> {
            try {
                File wav = new File(NOTIFICATION_FILE);
                if (!wav.exists()) {
                    generateNotificationWav(wav);
                }
                AudioInputStream ais = AudioSystem.getAudioInputStream(wav);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, ais.getFormat());
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(ais.getFormat());
                line.start();
                byte[] buffer = new byte[4096];
                int read;
                while ((read = ais.read(buffer)) != -1) {
                    line.write(buffer, 0, read);
                }
                line.drain();
                line.close();
                ais.close();
            } catch (Exception e) {
                try {
                    java.awt.Toolkit.getDefaultToolkit().beep();
                } catch (Exception ignored) {}
            }
        }, "audio-player");
        audioThread.setDaemon(true);
        audioThread.start();
    }

    private static void generateNotificationWav(File file) {
        try {
            float sampleRate = 44100;
            int durationMs = 500;
            int numSamples = (int)(sampleRate * durationMs / 1000);
            byte[] buffer = new byte[numSamples];
            for (int i = 0; i < numSamples; i++) {
                double t = (double)i / sampleRate;
                double freq = 880;
                double envelope = Math.exp(-t * 5);
                buffer[i] = (byte)(Math.sin(2 * Math.PI * freq * t) * envelope * 120);
            }
            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(buffer), format, buffer.length)) {
                byte[] wavHeader = createWavHeader(format, buffer.length);
                baos.write(wavHeader);
                byte[] audioBytes = new byte[buffer.length * 2];
                for (int i = 0; i < buffer.length; i++) {
                    int val = buffer[i] * 256;
                    audioBytes[i * 2] = (byte)(val & 0xFF);
                    audioBytes[i * 2 + 1] = (byte)((val >> 8) & 0xFF);
                }
                baos.write(audioBytes);
                Files.write(file.toPath(), baos.toByteArray());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] createWavHeader(AudioFormat format, int dataLength) {
        try {
            ByteArrayOutputStream header = new ByteArrayOutputStream();
            int byteRate = (int)(format.getSampleRate() * format.getSampleSizeInBits() * format.getChannels() / 8);
            int blockAlign = format.getChannels() * format.getSampleSizeInBits() / 8;
            header.write(stringToBytes("RIFF"));
            header.write(intToBytes(36 + dataLength * 2));
            header.write(stringToBytes("WAVE"));
            header.write(stringToBytes("fmt "));
            header.write(intToBytes(16));
            header.write(shortToBytes((short)1));
            header.write(shortToBytes((short)format.getChannels()));
            header.write(intToBytes((int)format.getSampleRate()));
            header.write(intToBytes(byteRate));
            header.write(shortToBytes((short)blockAlign));
            header.write(shortToBytes((short)format.getSampleSizeInBits()));
            header.write(stringToBytes("data"));
            header.write(intToBytes(dataLength * 2));
            return header.toByteArray();
        } catch (Exception e) {
            return new byte[44];
        }
    }

    private static byte[] stringToBytes(String s) {
        byte[] b = new byte[4];
        for (int i = 0; i < s.length(); i++) b[i] = (byte)s.charAt(i);
        return b;
    }

    private static byte[] intToBytes(int i) {
        byte[] b = new byte[4];
        b[0] = (byte)(i & 0xFF);
        b[1] = (byte)((i >> 8) & 0xFF);
        b[2] = (byte)((i >> 16) & 0xFF);
        b[3] = (byte)((i >> 24) & 0xFF);
        return b;
    }

    private static byte[] shortToBytes(short s) {
        byte[] b = new byte[2];
        b[0] = (byte)(s & 0xFF);
        b[1] = (byte)((s >> 8) & 0xFF);
        return b;
    }
}

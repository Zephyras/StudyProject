package com.fundroid.offstand.data.remote;

import android.util.Log;

import com.fundroid.offstand.data.model.Attendee;
import com.fundroid.offstand.utils.rx.RxEventBus;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ServerThread implements Runnable {
    private Socket socket;
    private DataInputStream streamByClient = null;
    private DataOutputStream streamToClient = null;
    private Attendee attendee;

    public DataInputStream getStreamByClient() {
        return streamByClient;
    }

    public DataOutputStream getStreamToClient() {
        return streamToClient;
    }

    public Attendee getAttendee() {
        return attendee;
    }

    public void setAttendee(Attendee attendee) {
        this.attendee = attendee;
    }

    public ServerThread(Socket socket) {
        Log.i("lsc", "ServerThread constructor " + Thread.currentThread().getName());
        this.socket = socket;
    }

    @Override
    public void run() {
        Log.d("lsc", "ServerThread 1 " + Thread.currentThread().getName());
        try {
            while (true) {
                Log.d("lsc", "ServerThread 2 " + Thread.currentThread().getName());
                streamByClient = new DataInputStream(socket.getInputStream());
                Log.d("lsc", "ServerThread 3 " + Thread.currentThread().getName());
                streamToClient = new DataOutputStream(socket.getOutputStream());
                String message = streamByClient.readUTF();
                RxEventBus.getInstance().sendEvent(message);
                Log.d("lsc", "ServerThread 4 " + message);
            }
        } catch (IOException e) {
            Log.e("lsc", "ServerThread e " + e.getMessage() + ", " + Thread.currentThread().getName());
        }

    }

}
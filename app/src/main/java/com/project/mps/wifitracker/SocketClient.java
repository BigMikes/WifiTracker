package com.project.mps.wifitracker;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

/**
 * Created by Giulio on 19/04/2016.
 */
public class SocketClient {
    private static final String TAG = "SOCKET_CLIENT";
    Socket client;
    String address;
    int port;
    private BufferedReader input;
    private PrintWriter output;

    public SocketClient(String address, int port) {
        if(address == null || address.isEmpty())
            return;
        if(port <= 0)
            return;
        this.address = address;
        this.port = port;
    }

    public boolean SocketConnect(){
        try {
            this.client = new Socket(address,port);
            output = new PrintWriter(client.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(client.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void closeSocket(){
        try{
            output.flush();
            output.close();
            input.close();
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String readLine(){
        String ret;
        if(input == null)
            return null;
        try {
            ret = input.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return ret;
    }

    public boolean sendLine(String line){
        if(line == null || line.isEmpty()){
            Log.e(TAG, "sendLine: invalid parameters");
            return false;
        }
        if(output == null){
            Log.e(TAG, "sendLine: some problem in the output stream");
            return false;
        }
        try {
            output.println(line);
        } catch(Exception e){
            Log.e(TAG, e.toString());
            return false;
        }
        return true;
    }

    //The data outcoming format will be "Mac1=power1,Mac2=power2,Mac3=power3"
    public boolean sendQuery(List<WifiInfo> samples){
        if(samples == null || samples.isEmpty()) {
            Log.e(TAG, "sendQuery: invalid parameters");
            return false;
        }
        String toSend = "";
        int count = 0;
        for(WifiInfo entry : samples){
            toSend += entry.getBssid() + "=" + entry.getRssi();
            if(count != samples.size() - 1)
                toSend += ",";
            count++;
        }
        return sendLine(toSend);
    }
}

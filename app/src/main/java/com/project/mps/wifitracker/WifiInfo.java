package com.project.mps.wifitracker;

/**
 * Created by Giulio on 06/04/2016.
 */
public class WifiInfo {
    private String Bssid;
    private String Ssid;
    private String Frequency;
    private String Rssi;

    public WifiInfo(String bssid, String ssid, String frequency, String rssi) {
        Bssid = bssid;
        Ssid = ssid;
        Frequency = frequency;
        Rssi = rssi;
    }

    public String getBssid() {
        return Bssid;
    }

    public void setBssid(String bssid) {
        Bssid = bssid;
    }

    public String getSsid() {
        return Ssid;
    }

    public void setSsid(String ssid) {
        Ssid = ssid;
    }

    public String getFrequency() {
        return Frequency;
    }

    public void setFrequency(String frequency) {
        Frequency = frequency;
    }

    public String getRssi() {
        return Rssi;
    }

    public void setRssi(String rssi) {
        Rssi = rssi;
    }

    @Override
    public String toString() {
        String ret = "";
        ret += Bssid + "," +  Ssid + "," + Frequency + "," + Rssi;
        return ret;
    }
}

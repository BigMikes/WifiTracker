package com.project.mps.wifitracker;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Giulio on 06/04/2016.
 */
public class Measurement {
    private String Building;
    private String Floor;
    private String Room;
    private List<WifiInfo> Samples;

    public Measurement(String building, String floor, String room, List<WifiInfo> samples) {
        Building = building;
        Floor = floor;
        Room = room;
        Samples = samples;
    }

    public String getBuilding() {
        return Building;
    }

    public void setBuilding(String building) {
        Building = building;
    }

    public String getFloor() {
        return Floor;
    }

    public void setFloor(String floor) {
        Floor = floor;
    }

    public String getRoom() {
        return Room;
    }

    public void setRoom(String room) {
        Room = room;
    }

    public List<WifiInfo> getSamples() {
        return Samples;
    }

    public void setSamples(List<WifiInfo> samples) {
        Samples = samples;
    }

    @Override
    public String toString() {
        String ret = "";
        ret += Building + "," + Floor + "," + Room;
        return ret;
    }

    public String[] print(){
        String[] result = new String[Samples.size()];
        String base = this.toString();
        int i = 0;
        for (WifiInfo elem : Samples) {
            result[i] = base + elem.toString();
            i++;
        }
        return result;
    }

    public void addSample(WifiInfo toAdd){
        if(Samples == null){
            Samples = new ArrayList<WifiInfo>();
        }
        Samples.add(toAdd);
    }

    public void deleteSamples() {
        Samples = null;
    }
}

package com.seilent.rpfanctl;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class Preset {
    private String name;
    private String uuid;
    private List<TempPoint> points;

    public static class TempPoint {
        int temperature;
        int fanPercent;

        public TempPoint(int temp, int fan) {
            this.temperature = temp;
            this.fanPercent = fan;
        }

        public int getTemperature() { return temperature; }
        public int getFanPercent() { return fanPercent; }
        public int getDuty() { return (fanPercent * 50000) / 100; }
    }

    public Preset(String name, List<TempPoint> points) {
        this.name = name;
        this.points = points;
        this.uuid = UUID.randomUUID().toString();
        Collections.sort(this.points, Comparator.comparingInt(TempPoint::getTemperature));
    }

    private Preset(String name, List<TempPoint> points, String uuid) {
        this.name = name;
        this.points = points;
        this.uuid = uuid;
        Collections.sort(this.points, Comparator.comparingInt(TempPoint::getTemperature));
    }

    public static Preset createDefault() {
        List<TempPoint> points = new ArrayList<>();
        points.add(new TempPoint(20, 0));
        points.add(new TempPoint(50, 10));
        points.add(new TempPoint(70, 15));
        points.add(new TempPoint(80, 20));
        return new Preset("Default", points, "550e8400-e29b-41d4-a716-446655440000");
    }

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }

    public List<TempPoint> getPoints() {
        return points;
    }

    public int getDutyForTemp(int tempMillis) {
        int temp = tempMillis / 1000;

        int duty = 5000;
        for (TempPoint point : points) {
            if (temp >= point.temperature) {
                duty = point.getDuty();
            } else {
                break;
            }
        }
        return duty;
    }

    public static int dutyToPercent(int duty) {
        return (duty * 100) / 50000;
    }

    public static int percentToDuty(int percent) {
        return (percent * 50000) / 100;
    }

    public String toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("uuid", uuid);
            json.put("name", name);
            JSONArray array = new JSONArray();
            for (TempPoint point : points) {
                JSONObject p = new JSONObject();
                p.put("temp", point.temperature);
                p.put("fan", point.fanPercent);
                array.put(p);
            }
            json.put("points", array);
            return json.toString();
        } catch (JSONException e) {
            return "";
        }
    }

    public static Preset fromJson(String jsonStr) {
        try {
            JSONObject json = new JSONObject(jsonStr);
            String name = json.getString("name");

            String uuid;
            if (json.has("uuid")) {
                uuid = json.getString("uuid");
            } else {
                uuid = UUID.randomUUID().toString();
            }
            JSONArray array = json.getJSONArray("points");
            List<TempPoint> points = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject p = array.getJSONObject(i);
                points.add(new TempPoint(p.getInt("temp"), p.getInt("fan")));
            }
            return new Preset(name, points, uuid);
        } catch (JSONException e) {
            return createDefault();
        }
    }

    @Override
    public String toString() {
        int maxFan = 0;
        for (TempPoint point : points) {
            if (point.fanPercent > maxFan) {
                maxFan = point.fanPercent;
            }
        }
        return name + " (max " + maxFan + "%)";
    }

    public String getCurveDetails() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) sb.append("  ");
            TempPoint point = points.get(i);
            sb.append(point.temperature).append("°C/").append(point.fanPercent).append("%");
        }
        return sb.toString();
    }

    public static Preset fromUserInput(String name, int t1, int f1, int t2, int f2, int t3, int f3, int t4, int f4) {
        List<TempPoint> points = new ArrayList<>();
        if (t1 > 0 && f1 > 0) points.add(new TempPoint(t1, f1));
        if (t2 > 0 && f2 > 0) points.add(new TempPoint(t2, f2));
        if (t3 > 0 && f3 > 0) points.add(new TempPoint(t3, f3));
        if (t4 > 0 && f4 > 0) points.add(new TempPoint(t4, f4));
        return new Preset(name, points, UUID.randomUUID().toString());
    }
}

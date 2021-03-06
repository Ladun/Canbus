package com.ssusp.canbus.tflite;

import android.graphics.RectF;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class BusInformation {

    private RectF busLocation;

    private List<String> numbers;

    private RectF frontDoor;
    private RectF backDoor;

    public BusInformation(RectF busLocation){
        this.busLocation = busLocation;
        this.numbers = new ArrayList<>();
    }

    public float locationFitness(Classifier.Recognition r){
        // 낮을수록 좋은 값
        if(r.getDetectedClass() == 5){
            float dn_x = r.getLocation().left + (r.getLocation().right - r.getLocation().left) / 2;
            float dn_y = r.getLocation().bottom + (r.getLocation().top - r.getLocation().bottom) / 2;
            float fd_x = r.getLocation().left + (r.getLocation().right - r.getLocation().left) / 2;
            float fd_y = r.getLocation().bottom + (r.getLocation().top - r.getLocation().bottom) / 2;

            float dx = dn_x - fd_x;
            float dy = dn_y - fd_y;
            return dx * dx + dy * dy;
        }
        else{
            return 1 - getLocationAreaByBus(r.getLocation());
        }
    }

    public float getLocationAreaByBus(RectF location){
        // location 변수가 현재 버스의 사각형에 얼마나 포함되어 있는 넓이의 비율을 반환
        float r = Math.min(location.right, busLocation.right);
        float l = Math.max(location.left, busLocation.left);
        float t = Math.min(location.top, busLocation.top);
        float b = Math.max(location.bottom, busLocation.bottom);

        float w= Math.max(r - l, 0);
        float h= Math.max(t - b, 0);
        return (w * h) / ((location.right - location.left) * (location.top - location.bottom));
    }

    public void addDoor(Classifier.Recognition r) {

        if(r.getDetectedClass() == 1)
            frontDoor = r.getLocation();
        else
            backDoor = r.getLocation();
    }
    public void addNumber(String number){
        numbers.add(number);
    }

    // Bus information speech functions
    public String getBusNumber(){
        for(String num :numbers)
            Log.d("BusInformation", num);

        if(numbers.size() < 1)
            return "";

        return numbers.get(0);
    }

    public boolean hasDoor(){
        return frontDoor != null || backDoor != null;
    }

    public String getBusDoorSpeech(){
        if(!hasDoor())
            return "";

        String door = "뒷문";

        if(frontDoor != null && backDoor != null){
            if(getLocationArea(frontDoor) > getLocationArea(backDoor)){
                door = "앞문";
            }
        }
        else {
            if(frontDoor == null)
                door = "뒷문";
            else
                door = "앞문";
        }

        return getBusNumber() + " 버스 " + door + "이 더 가까이 있습니다.";
    }

    public float getBusLocationArea(){
        return getLocationArea(busLocation);
    }

    private float getLocationArea(RectF location){
        return (location.right - location.left) * (location.top - location.bottom);
    }
}

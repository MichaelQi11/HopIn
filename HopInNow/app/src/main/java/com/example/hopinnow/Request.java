package com.example.hopinnow;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.sql.Time;
import java.util.Date;

public class Request extends Ride {
    private Double estimatedFare;

    public Request(){}

    // constructor
    public Request (Driver driver, Rider rider, LatLng pickUpLoc, LatLng dropOffLoc, String pickUpLocName, String dropOffLocName,  Date pickUpDateTime,
                    Car car, Double estimatedFare){
        super(driver,rider,pickUpLoc,dropOffLoc,pickUpLocName, dropOffLocName,pickUpDateTime,car);
        this.estimatedFare = estimatedFare;
    }

    //getter
    public Double getEstimatedFare() {
        return estimatedFare;
    }

    //setter
    public void setEstimatedFare(Double estimatedFare) {
        this.estimatedFare = estimatedFare;
    }

}
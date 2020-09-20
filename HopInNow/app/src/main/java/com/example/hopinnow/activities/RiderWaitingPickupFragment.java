package com.example.hopinnow.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hopinnow.R;
import com.example.hopinnow.entities.Car;
import com.example.hopinnow.entities.Driver;
import com.example.hopinnow.entities.Request;

import java.util.Objects;

/**
 * Authoer: Tianyu Bai
 * Version: 1.0.2
 * This class defines the fargment while rider is waiting for driver pickup.
 */
public class RiderWaitingPickupFragment extends Fragment {
    private Driver driver;

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rider_waiting_pickup, container,
                false);

        // set local variables
        Request curRequest = ((RiderMapActivity) Objects.requireNonNull(getActivity()))
                .retrieveCurrentRequestLocal();
        driver = ((RiderMapActivity) Objects.requireNonNull(getActivity())).retrieveOfferedDriver();
        // ui mock up
        if ((curRequest==null)||(driver==null)){
            Car car = new Car("Auburn","Speedster","Cream","111111");
            driver = new Driver("111@gmail.com", "12345678", "Lupin the Third",
                    "12345678", 10.0,  null, car, null);
        }


        if (view != null) {

            //set driver name
            TextView driverName = view.findViewById(R.id.rider_waiting_driver_name);
            driverName.setText(driver.getName());
            driverName.setOnClickListener(v ->
                    ((RiderMapActivity) Objects.requireNonNull(getActivity())).showDriverInfo());

            //set pick up location
            TextView pickUpLoc = view.findViewById(R.id.rider_waiting_pickUp);
            pickUpLoc.setText(Objects.requireNonNull(curRequest).getPickUpLocName());

            //set drop off location
            TextView dropOffLoc = view.findViewById(R.id.rider_waiting_dropOff);
            dropOffLoc.setText(curRequest.getDropOffLocName());

            //set estimated fare
            TextView estimatedFare = view.findViewById(R.id.rider_waiting_fare);
            estimatedFare.setText(Double.toString(curRequest.getEstimatedFare()));

            // Click this button to call driver
            Button callBtn = view.findViewById(R.id.rider_waiting_call_button);
            callBtn.setOnClickListener(v -> ((RiderMapActivity) Objects.requireNonNull(getActivity()))
                    .callNumber(driver.getPhoneNumber()));

            // Click this button to email driver
            Button emailBtn = view.findViewById(R.id.rider_waiting_email_button);
            emailBtn.setOnClickListener(v -> ((RiderMapActivity) Objects.requireNonNull(getActivity()))
                    .emailDriver(driver.getEmail()));

            // Click this button to cancel request
            // Change fragment
            Button cancelBtn = view.findViewById(R.id.rider_waiting_cancel_button);
            cancelBtn.setOnClickListener(v -> {
                //change fragment
                ((RiderMapActivity) Objects.requireNonNull(getActivity())).cancelRequestLocal();

            });

            //for ui test
            Button nextBtn = view.findViewById(R.id.rider_waiting_next_button);
            nextBtn.setOnClickListener(v -> ((RiderMapActivity) Objects.requireNonNull(getActivity())).
                    switchFragment(R.layout.fragment_rider_pickedup));

        }
        return view;
    }
}






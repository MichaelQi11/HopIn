package com.example.hopinnow.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.hopinnow.R;
import com.example.hopinnow.database.DriverDatabaseAccessor;
import com.example.hopinnow.database.DriverRequestDatabaseAccessor;
import com.example.hopinnow.entities.Driver;
import com.example.hopinnow.entities.Request;
import com.example.hopinnow.entities.Trip;
import com.example.hopinnow.statuslisteners.AvailRequestListListener;
import com.example.hopinnow.statuslisteners.DriverProfileStatusListener;
import com.example.hopinnow.statuslisteners.DriverRequestListener;
import com.example.hopinnow.statuslisteners.RequestAddDeleteListener;
import com.google.zxing.Result;
import com.tbruyelle.rxpermissions2.RxPermissions;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

/**
 * Author: Tianyu Bai
 * This class deals with the QR code and the payment process:
 */
public class DriverScanPaymentActivity extends AppCompatActivity
        implements ZXingScannerView.ResultHandler, DriverRequestListener,
        DriverProfileStatusListener, AvailRequestListListener, RequestAddDeleteListener {
    private static final String TAG = "DriverScanPaymentA";
    private ZXingScannerView cameraView;
    private Driver driver;
    private Request curRequest;
    private String qrPayment;
    private RxPermissions rxPermissions;
    private int permissionCount = 0;
    private TextView permissionMsg;
    private DriverRequestDatabaseAccessor driverRequestDatabaseAccessor = new DriverRequestDatabaseAccessor();
    private DriverDatabaseAccessor driverDatabaseAccessor = new DriverDatabaseAccessor();

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_scanning);
        driver = (Driver) getIntent().getSerializableExtra("Driver");
        //curRequest
        curRequest = Objects.requireNonNull(driver).getCurRequest();

        rxPermissions = new RxPermissions(DriverScanPaymentActivity.this);
        cameraView = findViewById(R.id.camera_scan_view);
        permissionMsg = findViewById(R.id.permission_camera_textView);

        if (ActivityCompat.checkSelfPermission(DriverScanPaymentActivity.this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            permissionMsg.setVisibility(TextView.INVISIBLE);
            cameraView.setResultHandler(this);
            cameraView.startCamera();
        } else {
            cameraPermission();
        }

        cameraView.setOnClickListener(v -> cameraPermission());

    }


    /**
     * This class handles the qr code result and transform it into readable string.
     * @param rawResult
     *     raw result from camera
     */
    @Override
    public void handleResult(Result rawResult){
        String encoded = rawResult.getText();
        String qrDriverEmail = StringUtils.substringBetween(encoded,"driverEmail"
                ,"DriverEmail");
        this.qrPayment = StringUtils.substringBetween(encoded,"totalPayment"
                ,"TotalPayment");

        // checks if the correct driver is scanning the payment
        if (driver.getEmail().equals(qrDriverEmail)){
            Log.v(TAG, "scan success");
            Log.v(TAG, "handleResult, driverCompleteRequest() is called...");
            driverRequestDatabaseAccessor.driverCompleteRequest(curRequest,this);
        } else {
            Toast.makeText(this, "This QR code does not belong to the trip " +
                    "that you have completed.", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Calculates new average rating for driver.
     * @param r
     *      the new rating
     */
    private void setNewDriverRating(double r){
        Log.v(TAG,"starts new rating");
        Double prevRating = this.driver.getRating();
        int counts = this.driver.getRatingCounts();
        double newRating =((prevRating*counts) + r)/(counts+1);
        newRating = Math.round(newRating * 100.0) / 100.0;
        this.driver.setRatingCounts(counts+1);
        this.driver.setRating(newRating);
        Log.v(TAG,"done new rating");
    }


    /**
     * This class handles permission of camera before scanning.
     */
    @SuppressLint("CheckResult")
    private void cameraPermission(){
        if (ActivityCompat.checkSelfPermission(DriverScanPaymentActivity.this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            rxPermissions
                    .request(Manifest.permission.CAMERA)
                    .subscribe(granted -> {
                        if (granted) {
                            cameraView.setResultHandler(this);
                            cameraView.startCamera();
                            permissionMsg.setVisibility(TextView.INVISIBLE);
                        } else {
                            Toast.makeText(DriverScanPaymentActivity.this,
                                    "You must enable camera to receive your payment.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
            permissionCount += 1;

            // if driver has denied camera permission three times
            if ((ActivityCompat.checkSelfPermission(DriverScanPaymentActivity.this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) &&
                    (permissionCount > 3)) {
                Toast.makeText(DriverScanPaymentActivity.this, "Guess you " +
                                "decided to donate your earning to the HopInNow Team, thanks! :D"
                        , Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this.getApplicationContext(), DriverMapActivity.class);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onDriverRequestAccept() {

    }

    @Override
    public void onDriverRequestTimeoutOrFail() {

    }

    @Override
    public void onRequestAlreadyTaken() {

    }

    @Override
    public void onRequestInfoChange(Request request) {

    }

    @Override
    public void onRequestAcceptedByRider(Request request) {

    }

    @Override
    public void onRequestDeclinedByRider() {

    }

    @Override
    public void onDriverPickupSuccess() {

    }

    @Override
    public void onDriverPickupFail() {

    }

    @Override
    public void onDriverDropoffSuccess(Request request) {

    }

    @Override
    public void onDriverDropoffFail() {

    }

    @Override
    public void onDriverRequestCompleteSuccess() {
        // waits for rider rating
        Toast.makeText(this, "You have successfully received " + this.qrPayment +
                " QR bucks for your completed ride!", Toast.LENGTH_SHORT).show();
        driverRequestDatabaseAccessor.driverWaitOnRating(this.curRequest, this);
        Log.v(TAG,"starts waiting rating");
    }

    @Override
    public void onDriverRequestCompleteFailure() {

    }

    @Override
    public void onWaitOnRatingSuccess(Request request) {
        // means the rider has finished rating
        this.curRequest = request;
        driverDatabaseAccessor.getDriverProfile(this);
        Log.v(TAG, "Rider has rated the trip!!!!");
        Log.v(TAG, "now to get driver profile...");
    }

    @Override
    public void onWaitOnRatingError() {
        driverRequestDatabaseAccessor.driverWaitOnRating(this.curRequest, this);
    }

    @Override
    public void onDriverProfileRetrieveSuccess(Driver driver) {
        this.driver = driver;
        double prevDeposit = this.driver.getDeposit();
        this.driver.setDeposit(prevDeposit + Double.parseDouble(this.qrPayment));
        if (this.curRequest.getRating()!=0){
            setNewDriverRating(this.curRequest.getRating());
            Toast.makeText(DriverScanPaymentActivity.this,"You were rated" +
                            this.curRequest.getRating() + "stars!", Toast.LENGTH_SHORT).show();
        }
        Date current_time = new Date();
        Log.v(TAG, "trying to add in the new trip...");
        ArrayList<Trip> driverTripList = this.driver.getDriverTripList();
        if(driverTripList == null){
            Log.v(TAG, "driverTripList is null");
            driverTripList = new ArrayList<>();
        }
        driverTripList.add(new Trip(curRequest.getDriverEmail(),curRequest.getRiderEmail(),
                curRequest.getPickUpLoc(),curRequest.getDropOffLoc(),
                curRequest.getPickUpLocName(),curRequest.getDropOffLocName(),
                curRequest.getPickUpDateTime(), current_time,
                (int)Math.abs(current_time.getTime() -
                        curRequest.getPickUpDateTime().getTime()),
                curRequest.getCar(),curRequest.getEstimatedFare(),curRequest.getRating()));
        Log.v(TAG, "driver trip added!!!!!!!");
        this.driver.setDriverTripList(driverTripList);
        Log.v(TAG, "driver current request is now deleted.");
        this.driver.setCurRequest(null);
        driverDatabaseAccessor.updateDriverProfile(this.driver,this);
        Log.v(TAG, "driver profile retrieved.");
        Log.v(TAG, "now to update driver profile...");
        driverDatabaseAccessor.updateDriverProfile(this.driver,DriverScanPaymentActivity.this);
    }

    @Override
    public void onDriverProfileRetrieveFailure() {

    }

    @Override
    public void onDriverProfileUpdateSuccess(Driver driver) {
        /*if (rated){
            rated = false;
            Log.v(TAG, "driver profile updated.");
            Log.v(TAG, "now to go to driver map activity...");
            Log.v(TAG, "request DELETE HERE::::::");
            driverRequestDatabaseAccessor.deleteRequest(this);
        } else {
            Log.v(TAG, "driver request completed.");
            Log.v(TAG, "now driver is WAITING ON RATING!!!!");
            driverRequestDatabaseAccessor.driverWaitOnRating(curRequest,this);
        }*/
        // driver profile updated, go back to map activity
        Log.v(TAG, "driver profile updated.");
        Intent intent = new Intent(this.getApplicationContext(), DriverMapActivity.class);
        startActivity(intent);
        finish();
        Log.v(TAG, "now to go to driver map activity...");
    }

    @Override
    public void onDriverProfileUpdateFailure() {

    }

    @Override
    public void onRequestAddedSuccess() {

    }

    @Override
    public void onRequestAddedFailure() {

    }

    @Override
    public void onRequestDeleteSuccess() {
    }

    @Override
    public void onRequestDeleteFailure() {

    }

    @Override
    public void onGetRequiredRequestsSuccess(ArrayList<Request> requests) {

    }

    @Override
    public void onGetRequiredRequestsFailure() {

    }

    @Override
    public void onAllRequestsUpdateSuccess(ArrayList<Request> requests) {

    }

    @Override
    public void onAllRequestsUpdateError() {

    }
}
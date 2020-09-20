package com.example.hopinnow.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.hopinnow.R;
import com.example.hopinnow.database.DriverDatabaseAccessor;
import com.example.hopinnow.database.RiderDatabaseAccessor;
import com.example.hopinnow.database.RiderRequestDatabaseAccessor;
import com.example.hopinnow.entities.Car;
import com.example.hopinnow.entities.Driver;
import com.example.hopinnow.entities.LatLong;
import com.example.hopinnow.entities.Request;
import com.example.hopinnow.entities.Rider;
import com.example.hopinnow.entities.Trip;
import com.example.hopinnow.helperclasses.QRCodeHelper;
import com.example.hopinnow.statuslisteners.DriverObjectRetreieveListener;
import com.example.hopinnow.statuslisteners.DriverProfileStatusListener;
import com.example.hopinnow.statuslisteners.RequestAddDeleteListener;
import com.example.hopinnow.statuslisteners.RiderProfileStatusListener;
import com.example.hopinnow.statuslisteners.RiderRequestListener;
import com.google.gson.Gson;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Author: Tianyu Bai
 * This class defines the activity for rider payment after confirming arrival at drop off location.
 */
public class RiderPaymentActivity extends AppCompatActivity implements RiderProfileStatusListener,
        DriverObjectRetreieveListener, RiderRequestListener, RequestAddDeleteListener,
        DriverProfileStatusListener {
    public static final String TAG = "RiderPaymentA";
    private Request curRequest;
    private Driver driver;
    private Rider rider;
    private Double totalPayment;
    private Double myTip = 0.00;
    private Double baseFare;
    private ImageView qrImage;
    private Boolean other = false;
    private boolean driverRatingUpdated = false;
    private TextView totalPaymentTextView;
    private Date dropOffDateTime;
    private Double myRating;
    private RiderDatabaseAccessor riderDatabaseAccessor;
    private RiderRequestDatabaseAccessor riderRequestDatabaseAccessor;
    private ProgressDialog progressDialog;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_payment);

        SharedPreferences mPrefs = getSharedPreferences("LocalRequest", MODE_PRIVATE);
        Gson gsonRequest = new Gson();
        String json = mPrefs.getString("CurrentRequest", "");
        curRequest = gsonRequest.fromJson(json, Request.class);
        progressDialog = new ProgressDialog(this);

        // for ui testing
        if (driver == null){
            Car car = new Car("Auburn","Speedster","Cream","111111");
            driver = new Driver("111@gmail.com", "12345678", "Lupin the Third",
                    "12345678", 10.0,  null, car, null);
        }
        this.riderDatabaseAccessor = new RiderDatabaseAccessor();
        this.riderDatabaseAccessor.getRiderProfile(this);
        DriverDatabaseAccessor driverDatabaseAccessor = new DriverDatabaseAccessor();
        driverDatabaseAccessor.getDriverObject(curRequest.getDriverEmail(),this);
        this.riderRequestDatabaseAccessor = new RiderRequestDatabaseAccessor();

        // set local variables
        baseFare = curRequest.getEstimatedFare();
        dropOffDateTime = Calendar.getInstance().getTime();
        totalPayment = baseFare;

        //set initial total payment
        totalPaymentTextView = findViewById(R.id.rider_payment_total);
        totalPaymentTextView.setText(Double.toString(baseFare));

        //show total payment calculation
        Button showTotalBtn = findViewById(R.id.rider_payment_calculate);
        showTotalBtn.setOnClickListener(v -> {
            setMyTip();
            totalPayment = formatTotalPayment();
            totalPaymentTextView.setText(Double.toString(totalPayment));
        });

        // creates QR code on button confirm, QR contains total payment amount
        qrImage = findViewById(R.id.rider_payment_qr);
        final Button confirmPaymentBtn = findViewById(R.id.rider_payment_submit_tips);
        confirmPaymentBtn.setOnClickListener(v -> {

            setMyTip();
            totalPayment = formatTotalPayment();

            //checks available deposit for payment, if enough then QR code is generated
            if (totalPayment > rider.getDeposit()){
                String msg = "There is insufficient deposit in your account!";
                Toast.makeText(RiderPaymentActivity.this,msg,Toast.LENGTH_SHORT).show();
            } else {
                curRequest.setEstimatedFare(totalPayment);
                Gson gsonPay = new Gson();
                String encodedMsg= "driverEmail" + curRequest.getDriverEmail() + "DriverEmail" +
                        "totalPayment" + totalPayment + "TotalPayment";
                gsonPay.toJson(encodedMsg);
                Bitmap bitmap = QRCodeHelper
                        .newInstance(RiderPaymentActivity.this)
                        .setContent(encodedMsg)
                        .setMargin()
                        .generateQR();
                qrImage.setImageBitmap(bitmap);
                qrImage.setBackgroundResource(R.color.ColorBlack);
                confirmPaymentBtn.setVisibility(View.GONE);
                showTotalBtn.setEnabled(false);
                riderRequestDatabaseAccessor.riderWaitForRequestComplete(this);
            }
        });


    }


    /**
     * Shows dialog that prompts rider to rate the driver of corresponding trip.
     */
    public void showRatingDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_rider_rating);

        // set driver name
        final TextView driverName= dialog.findViewById(R.id.dialog_rider_rating_driver);
        driverName.setText(driver.getName());
        driverName.setOnClickListener(v -> showDriverInfo(driver));

        //submit rating and complete request
        final RatingBar ratingBar = dialog.findViewById(R.id.dialog_rating_bar);
        Button submitBtn= dialog.findViewById(R.id.dialog_rating_submit);
        submitBtn.setOnClickListener(v -> {
            myRating = (double) ratingBar.getRating();
            if (myRating!= -1.0){
                progressDialog.show();
                completeRequest(myRating);
                //dialog.dismiss();
            } else {
                Toast.makeText(RiderPaymentActivity.this, "Please select your " +
                        "rating or press CANCEL to complete your ride.", Toast.LENGTH_SHORT)
                        .show();
            }
        });
        dialog.show();
        dialog.setCanceledOnTouchOutside(false);
    }


    /**
     * Completes current request and returns rider to the new request prompt page.
     */
    private void completeRequest(double rating){
        String msg = "Your trip is completed!";
        Toast.makeText(RiderPaymentActivity.this, msg, Toast.LENGTH_LONG).show();
        this.curRequest.setRating(rating);
        this.curRequest.setEstimatedFare(totalPayment);
        this.riderRequestDatabaseAccessor.riderRateRequest(curRequest,this);
    }


    /**
     * Determines the rider selected tip amount.
     * @param view
     *      current view
     */
    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.rider_payment_ten:
                if (checked)
                    myTip = 0.1;
                other = false;
                break;
            case R.id.rider_payment_fifteen:
                if (checked)
                    myTip = 0.15;
                other = false;
                break;
            case R.id.rider_payment_twenty:
                if (checked)
                    myTip = 0.2;
                other = false;
                break;
            case R.id.rider_payment_else:
                if (checked)
                    other = true;
                myTip = 0.0;
                break;
        }
    }


    /**
     * Completes payment transaction and pops up rating dialog.
     * This method is trigger by driver finishing the scanning of the QR.
     */
    public void onScanningCompleted(){

        double newDepositAmount = rider.getDeposit()-totalPayment;
        this.rider.setDeposit(newDepositAmount);
        //riderDatabaseAccessor.updateRiderProfile(rider,RiderPaymentActivity.this);
        if (!driverRatingUpdated){
            String msg = "Your payment of " + totalPayment + " QR bucks is successful!";
            driverRatingUpdated = true;
            Toast.makeText(RiderPaymentActivity.this, msg, Toast.LENGTH_LONG).show();
        }
        if (!((RiderPaymentActivity.this).isFinishing())){
            showRatingDialog();
        }

    }


    /**
     * Get rider's customized tip amount.
     */
    private void setMyTip(){
        if (other){
            EditText otherTip = findViewById(R.id.rider_payment_other_editText);
            if (!otherTip.getText().toString().isEmpty()) {
                myTip = (Double.parseDouble(otherTip.getText().toString())) / 100;
            }
        }
    }


    /**
     * Format total payment to double with two decimals.
     */
    private Double formatTotalPayment(){
        return Double.parseDouble(new DecimalFormat("##.##")
                .format((1 + myTip) * baseFare));
    }


    /**
     * Change current request from class Request to class Trip.
     */
    private Trip toTrip(){
        int duration = (int) (curRequest.getPickUpDateTime().getTime() - dropOffDateTime.getTime());
        LatLong mpickUpLoc = curRequest.getPickUpLoc();
        LatLong mdropOffLoc = curRequest.getDropOffLoc();
        String dropOffName = curRequest.getDropOffLocName();
        String pickUpName = curRequest.getPickUpLocName();
        Date pickUpTime = curRequest.getPickUpDateTime();
        Car car = driver.getCar();
        return new Trip(driver.getEmail(),rider.getEmail(),mpickUpLoc,mdropOffLoc,pickUpName,
                dropOffName,pickUpTime, dropOffDateTime, duration, car,totalPayment,myRating);
    }

    /**
     * Shows driver information and contact means on a dialog
     */
    public void showDriverInfo(Driver myDriver){
        final Driver d = myDriver;
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_driver_info);

        //set driver name
        TextView driverName= dialog.findViewById(R.id.dialog_driver_name);
        driverName.setText(d.getName());

        //set driver rating
        TextView driverRating = dialog.findViewById(R.id.dialog_driver_rating);
        String rating;
        if (d.getRating()==0){
            rating = "not yet rated";
        } else {
            rating = Double.toString(d.getRating());
        }
        driverRating.setText(rating);

        //set driver car
        TextView driverCar = dialog.findViewById(R.id.dialog_driver_car);
        String carInfo = d.getCar().getColor() + " " + d.getCar().getMake() + " " + d.getCar().getModel();
        driverCar.setText(carInfo);

        //set driver license
        TextView driverLicense = dialog.findViewById(R.id.dialog_driver_plate);
        driverLicense.setText(d.getCar().getPlateNumber());

        //call driver
        Button callBtn= dialog.findViewById(R.id.dialog_call_button);
        callBtn.setOnClickListener(v -> callNumber(d.getPhoneNumber()));

        //email driver
        Button emailBtn= dialog.findViewById(R.id.dialog_email_button);
        emailBtn.setOnClickListener(v -> emailDriver(d.getEmail()));

        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    /**
     * Starts phone calling.
     * @param phoneNumber
     *      the phone number to be called
     */
    @SuppressLint("CheckResult")
    public void callNumber(String phoneNumber){
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:"+phoneNumber));

        if (ActivityCompat.checkSelfPermission(RiderPaymentActivity.this,
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            RxPermissions rxPermissions = new RxPermissions(this);
            rxPermissions
                    .request(Manifest.permission.CALL_PHONE)
                    .subscribe(granted -> {
                        if (granted) {
                            startActivity(callIntent);
                        } else {
                            String driverNumber = driver.getPhoneNumber();
                            Toast.makeText(this,"Driver's Phone Number: " + driverNumber,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            startActivity(callIntent);
        }
    }


    /**
     * Prompts email app selection and directs to email drafting page with auto0filled email address
     * of the driver.
     * @param email
     *      the driver's email address
     */
    public void emailDriver(String email){
        //Stackoverflow post by Dira
        //https://stackoverflow.com/questions/8701634/send-email-intent
        //Answer by Dira (code from the question itself)
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/html");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {email});

        startActivity(Intent.createChooser(intent, "Send Email"));
    }


    @Override
    public void onRiderProfileRetrieveSuccess(Rider rider) { this.rider = rider;}

    @Override
    public void onRiderProfileRetrieveFailure() {}

    @Override
    public void onRiderProfileUpdateSuccess(Rider rider) {
        progressDialog.dismiss();
        //change activity
        Intent intent = new Intent(RiderPaymentActivity.this,RiderMapActivity.class);
        intent.putExtra("Current_Request_To_Null", "cancel");
        startActivity(intent);
        finish();
    }

    @Override
    public void onRiderProfileUpdateFailure() {}

    @Override
    public void onBackPressed(){}

    @Override
    public void onDriverObjRetrieveSuccess(Driver driver) {
        this.driver = driver;
    }

    @Override
    public void onDriverObjRetrieveFailure() {}

    @Override
    public void onRiderRequestAcceptedNotify(Request request) {}

    @Override
    public void onRiderRequestTimeoutOrFail() {}

    @Override
    public void onRiderAcceptDriverRequest() {

    }

    @Override
    public void onRiderDeclineDriverRequest() {

    }

    @Override
    public void onRiderPickedupSuccess(Request request) {}

    @Override
    public void onRiderPickedupTimeoutOrFail() {}

    @Override
    public void onRiderDropoffSuccess(Request request) {

    }

    @Override
    public void onRiderDropoffFail() {

    }

    @Override
    public void onRiderRequestComplete() {
        onScanningCompleted();
    }

    @Override
    public void onRiderRequestCompletionError() {
        riderRequestDatabaseAccessor.riderWaitForRequestComplete(this);
    }

    @Override
    public void onRequestRatedSuccess() {
        Toast.makeText(RiderPaymentActivity.this, "Rating successful!", Toast.LENGTH_SHORT)
                .show();
        curRequest.setAcceptStatus(1);
        curRequest.setArrivedAtDest(true);
        curRequest.setComplete(true);
        riderRequestDatabaseAccessor.addUpdateRequest(curRequest,RiderPaymentActivity.this);
        Trip newTrip = toTrip();
        ArrayList<Trip> riderTripList = this.rider.getRiderTripList();
        if (riderTripList==null){
            riderTripList = new ArrayList<>();
        }
        riderTripList.add(newTrip);
        this.rider.setRiderTripList(riderTripList);
        Log.v(TAG, "rider's current request is now deleted.");
        this.rider.setCurRequest(null);
        riderDatabaseAccessor.updateRiderProfile(this.rider,this);
    }

    @Override
    public void onRequestRatedError() {}

    @Override
    public void onDriverProfileRetrieveSuccess(Driver driver) {
    }

    @Override
    public void onDriverProfileRetrieveFailure() {}

    @Override
    public void onDriverProfileUpdateSuccess(Driver driver){
    }

    @Override
    public void onDriverProfileUpdateFailure() {}

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
}
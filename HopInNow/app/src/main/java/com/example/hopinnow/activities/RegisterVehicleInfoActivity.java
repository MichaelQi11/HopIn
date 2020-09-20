package com.example.hopinnow.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.hopinnow.R;
import com.example.hopinnow.database.UserDatabaseAccessor;
import com.example.hopinnow.entities.Car;
import com.example.hopinnow.entities.Driver;
import com.example.hopinnow.entities.User;
import com.example.hopinnow.statuslisteners.LoginStatusListener;
import com.example.hopinnow.statuslisteners.RegisterStatusListener;
import com.example.hopinnow.statuslisteners.UserProfileStatusListener;

/**
 * Author: Shway Wang
 * Version: 1.0.3
 * This activity class lets the driver enter the vehicle information and store it in database.
 * The information can be left null and be filled later.
 */
public class RegisterVehicleInfoActivity extends AppCompatActivity implements LoginStatusListener,
        RegisterStatusListener, UserProfileStatusListener {
    private UserDatabaseAccessor userDatabaseAccessor;
    // the user object past:
    private Driver driver;
    // the four part information about the vehicle:
    private EditText make;
    private EditText model;
    private EditText color;
    private EditText plateNumber;
    // finish registration button:
    private Button finishBtn;
    // alert progress dialog:
    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle_info);
        // init user database accessor:
        this.userDatabaseAccessor = new UserDatabaseAccessor();
        // get the information already filled:
        this.driver = (Driver)getIntent().getSerializableExtra("DriverObject");
        // connect UI components:
        this.make = findViewById(R.id.vehMakeEt);
        this.model = findViewById(R.id.vehModelEt);
        this.color = findViewById(R.id.vehColorEt);
        this.plateNumber = findViewById(R.id.vehPlateEt);
        this.finishBtn = findViewById(R.id.vehicleToFinishBtn);
        // init progress bar:
        this.progressDialog = new ProgressDialog(this);
        this.progressDialog.setContentView(R.layout.custom_progress_bar);
    }
    @Override
    protected void onStart() {
        super.onStart();
        this.finishBtn.setOnClickListener(view -> {
            progressDialog.show();
            String makeData = make.getText().toString();
            String modelData = model.getText().toString();
            String colorData = color.getText().toString();
            String plateNumberData = plateNumber.getText().toString();
            Car car = new Car(makeData, modelData, colorData, plateNumberData);
            driver.setCar(car);
            userDatabaseAccessor.registerUser(driver, RegisterVehicleInfoActivity.this);
        });
    }

    @Override
    public void onLoginSuccess() {
        // first dismiss the progress bar:
        this.progressDialog.dismiss();
        // initialize intent to go to the ProfileActivity:
        Intent intent = new Intent(getApplicationContext(), DriverMapActivity.class);
        Bundle bundle = new Bundle();
        // put the driver object into the bundle, Profile activity can access directly:
        bundle.putSerializable("UserObject", this.driver);
        intent.putExtras(bundle);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
        // show success message here:
        Toast.makeText(getApplicationContext(),
                "Driver logged in successfully!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLoginFailure() {
        this.progressDialog.dismiss();
        Toast.makeText(getApplicationContext(),
                "Driver login failed, try again later!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRegisterSuccess() {
        Toast.makeText(getApplicationContext(),
                "Driver registered successfully!", Toast.LENGTH_SHORT).show();
        this.userDatabaseAccessor.createUserProfile(this.driver, this);
    }

    @Override
    public void onRegisterFailure() {
        this.progressDialog.dismiss();
        Toast.makeText(getApplicationContext(),
                "Register failed, try again later!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onWeakPassword() {

    }

    @Override
    public void onInvalidEmail() {

    }

    @Override
    public void onUserAlreadyExist() {

    }

    @Override
    public void onProfileStoreSuccess() {
        // after the profile is stored should the driver be logged in:
        this.userDatabaseAccessor.loginUser(this.driver, this);
    }

    @Override
    public void onProfileStoreFailure() {
        this.progressDialog.dismiss();
    }

    @Override
    public void onProfileRetrieveSuccess(User user) {
        this.progressDialog.dismiss();
    }

    @Override
    public void onProfileRetrieveFailure() {
        this.progressDialog.dismiss();
    }

    @Override
    public void onProfileUpdateSuccess(User user) {
        this.progressDialog.dismiss();
    }

    @Override
    public void onProfileUpdateFailure() {
        this.progressDialog.dismiss();
    }
}

package com.example.hopinnow.activities;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hopinnow.R;
import com.example.hopinnow.database.DriverDatabaseAccessor;
import com.example.hopinnow.database.UserDatabaseAccessor;
import com.example.hopinnow.entities.Driver;
import com.example.hopinnow.entities.User;
import com.example.hopinnow.statuslisteners.DriverProfileStatusListener;
import com.example.hopinnow.statuslisteners.UserProfileStatusListener;

import java.util.Locale;
import java.util.Objects;

/**
 * Author: Shway Wang
 * Co-author: Zhiqi Zhou
 * Version: 1.0.0
 * show and edit user profile for both rider and driver
 */
public class ProfileActivity extends AppCompatActivity implements UserProfileStatusListener,
        DriverProfileStatusListener {
    // declare database accessor:
    private UserDatabaseAccessor userDatabaseAccessor;
    // Global User object:
    private User currentUser;
    // UI Components:
    private EditText name;
    private EditText phoneNumber;
    private TextView email;
    private TextView deposit;
    private TextView userType;
    private TextView rating;
    private LinearLayout ratingLayout;
    private String driverRating;
    private Button editBtn;
    private Button updateBtn;
    private Button logoutButton;
    // alert progress dialog:
    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        // init the userDatabaseAccessor:
        this.userDatabaseAccessor = new UserDatabaseAccessor();
        this.getIntent();
        DriverDatabaseAccessor driverDatabaseAccessor = new DriverDatabaseAccessor();
        driverDatabaseAccessor.getDriverProfile(this);
        // check the login status:
        if (!this.userDatabaseAccessor.isLoggedin()) {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        }
        // UI init, button listeners are written in the onStart() method
        this.name = findViewById(R.id.proNameET);
        this.name.setEnabled(false);
        this.email = findViewById(R.id.proEmailTxt);
        this.phoneNumber = findViewById(R.id.proPhoneET);
        this.phoneNumber.setEnabled(false);
        this.deposit = findViewById(R.id.proDeposit);
        this.rating = findViewById(R.id.proRating);
        this.ratingLayout = findViewById(R.id.proRatingLayout);
        this.userType = findViewById(R.id.proUserType);
        this.editBtn = findViewById(R.id.editProfileBtn);
        this.updateBtn = findViewById(R.id.proUpdateBtn);
        this.updateBtn.setEnabled(false);
        this.updateBtn.setVisibility(View.INVISIBLE);
        this.logoutButton = findViewById(R.id.proLogoutBtn);
        // alert progress dialog:
        progressDialog = new ProgressDialog(ProfileActivity.this);
        progressDialog.setContentView(R.layout.custom_progress_bar);
        progressDialog.show();
        // retrieve the current user information
        Intent intent = this.getIntent();
        this.currentUser = (User)intent.getSerializableExtra("UserObject");
        if (this.currentUser == null) {
            this.userDatabaseAccessor.getUserProfile(this);
        } else {
            this.fillUserInfo(this.currentUser);
        }
    }

    // wrapper function to fill in the text fields the information from the user object:
    @SuppressLint("SetTextI18n")
    private void fillUserInfo(User user) {
        if (user == null) {
            this.name.setText("");
            this.email.setText("");
            this.phoneNumber.setText("");
            this.deposit.setText("");
            this.userType.setText("");
        } else {
            // set all text fields according to the retrieved user object:
            this.name.setText(Objects.requireNonNull(currentUser).getName());
            this.email.setText(currentUser.getEmail());
            this.phoneNumber.setText(currentUser.getPhoneNumber());
            this.deposit.setText(
                    String.format(Locale.CANADA, "%.2f", currentUser.getDeposit()));
            if (this.currentUser.isUserType()) {    // if true, then the user is driver
                this.userType.setText(R.string.usertype_driver);
                if (this.driverRating!=null){
                    this.rating.setText(driverRating);
                } else {
                    this.rating.setText(" yet been rated");
                }
            } else {    // or else, the user is a rider
                this.userType.setText(R.string.usertype_rider);
                this.ratingLayout.setVisibility(View.GONE);
            }
        }
        this.progressDialog.dismiss();
    }
    @Override
    protected void onStart() {
        super.onStart();
        // actions when edit button is clicked:
        this.editBtn.setOnClickListener(view -> {
            name.setEnabled(true);
            phoneNumber.setEnabled(true);
            editBtn.setEnabled(false);
            editBtn.setVisibility(View.INVISIBLE);
            updateBtn.setEnabled(true);
            updateBtn.setVisibility(View.VISIBLE);
        });
        // actions when update button is clicked:
        this.updateBtn.setOnClickListener(view -> {
            // alert progress dialog:
            progressDialog = new ProgressDialog(ProfileActivity.this);
            progressDialog.setContentView(R.layout.custom_progress_bar);
            progressDialog.show();
            // access database:
            currentUser.setName(name.getText().toString());
            currentUser.setPhoneNumber(phoneNumber.getText().toString());
            userDatabaseAccessor.updateUserProfile(currentUser, ProfileActivity.this);
        });
        // actions when logout button is clicked:
        logoutButton.setOnClickListener(view -> {
            userDatabaseAccessor.logoutUser();
            // go to the login activity again:
            Toast.makeText(getApplicationContext(),
                    "You are Logged out!", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            finish();
        });

    }



    @Override
    public void onProfileStoreSuccess() {

    }

    @Override
    public void onProfileStoreFailure() {
        this.progressDialog.dismiss();
    }

    @Override
    public void onProfileRetrieveSuccess(User user) {
        this.currentUser = user;
        this.fillUserInfo(this.currentUser);
    }

    @Override
    public void onProfileRetrieveFailure() {
        this.progressDialog.dismiss();
        Toast.makeText(getApplicationContext(),
                "Info retrieve failed, check network connection.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProfileUpdateSuccess(User user) {
        this.currentUser = user;
        this.fillUserInfo(this.currentUser);
        // set all text fields according to the retreived user object:
        name.setEnabled(false);
        phoneNumber.setEnabled(false);
        editBtn.setEnabled(true);
        editBtn.setVisibility(View.VISIBLE);
        updateBtn.setEnabled(false);
        updateBtn.setVisibility(View.INVISIBLE);
        this.progressDialog.dismiss();
        Toast.makeText(getApplicationContext(),
                "Your info is updated!", Toast.LENGTH_LONG).show();

    }

    @Override
    public void onProfileUpdateFailure() {
        this.progressDialog.dismiss();
        Toast.makeText(getApplicationContext(),
                "Update failed, check network connection.", Toast.LENGTH_LONG).show();
    }


    @Override
    public void onDriverProfileRetrieveSuccess(Driver driver) {
        this.driverRating = driver.getRating().toString();
    }

    @Override
    public void onDriverProfileRetrieveFailure() {

    }

    @Override
    public void onDriverProfileUpdateSuccess(Driver driver) {

    }

    @Override
    public void onDriverProfileUpdateFailure() {

    }
}
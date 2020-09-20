package com.example.hopinnow;

import android.app.Activity;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;


import android.widget.EditText;
import android.widget.TextView;

import com.example.hopinnow.activities.LoginActivity;
import com.example.hopinnow.activities.RiderMapActivity;

import com.robotium.solo.Solo;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;


/**
 * Author: Tianyu Bai
 * UI tests on rider side activities. Robotium test framework is used.
 *
 * Before running the test, please ensure rider2@rider.com has deposit over 5 QR bucks. Please open
 * permission of location and call. Please set location of emulator/testing device to Edmonton, AB.
 */
@RunWith(AndroidJUnit4.class)
public class Test6_RiderActivityTest {
    private Solo solo;
    private String address;


    @Rule
    public ActivityTestRule<LoginActivity> rule =
            new ActivityTestRule<>(LoginActivity.class, true, true);


    /**
     * Runs before all tests and creates solo instance.
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception{
        solo = new Solo(getInstrumentation(),
                rule.getActivity());
    }

    /**
     * Gets the Activity
     * @throws Exception
     *      throws all exception
     */
    @Test
    public void start() throws Exception{
        Activity activity = rule.getActivity();
    }


    /**
     * Logs in user.
     * @throws InterruptedException
     *      throws exception if thread is interrupted
     */
    private void loginUser() throws InterruptedException {
        // Log in To Activity
        String userEmail = "test@rider.com";
        solo.enterText((EditText)solo.getView(R.id.loginEmailEditText), userEmail);
        String userPassword = "1111111";
        solo.enterText((EditText)solo.getView(R.id.loginPassword), userPassword);
        solo.goBack();
        solo.clickOnView(solo.getView(R.id.loginButton));

        Thread.sleep(2000);
    }

    /**
     * Logs out user.
     * @throws InterruptedException
     *      throws exception if thread is interrupted
     */
    private void logoutUser() throws InterruptedException {
        solo.clickOnView(solo.getView(R.id.riderMenuBtn));
        solo.clickOnMenuItem("Log Out");
        Thread.sleep(2000);
    }


    /**
     * Add new request.
     * @throws InterruptedException
     *      throws exception if thread is interrupted
     */
    private void addNewRequest() throws InterruptedException {
        if (address==null){
            solo.clickOnButton("Pick Up at My Location");
            address = solo.getView(R.id.pick_up_auto_complete).toString();
        }

        solo.enterText((EditText) solo.getView(R.id.mock_pickUp), address);
        solo.enterText((EditText) solo.getView(R.id.mock_dropOff), address);
        solo.clickOnButton("HOP IN NOW!");
        Thread.sleep(2000);
    }


    /**
     * Checks all tips option.
     * @throws InterruptedException
     *      throws exception if thread is interrupted
     */
    private  void checkPayment() throws InterruptedException {
        solo.clickOnButton("10%");
        solo.clickOnView(solo.getView(R.id.rider_payment_calculate));
        assertTrue(solo.waitForText("3.85", 1, 2000));

        solo.clickOnButton("15%");
        solo.clickOnView(solo.getView(R.id.rider_payment_calculate));
        assertTrue(solo.waitForText("4.02", 1, 2000));

        solo.clickOnButton("20%");
        solo.clickOnView(solo.getView(R.id.rider_payment_calculate));
        assertTrue(solo.waitForText("4.2", 1, 2000));

        solo.clickOnView(solo.getView(R.id.rider_payment_else));
        solo.enterText((EditText)solo.getView(R.id.rider_payment_other_editText),"10");
        solo.clickOnView(solo.getView(R.id.rider_payment_calculate));
        assertTrue(solo.waitForText("3.85", 1, 3000));

        solo.clickOnButton("CONFIRM");

        Thread.sleep(2000);
    }




    /**
     * Checks menu button.
     * Auto complete fragment testing is currently in question and mocks are used.
     * @throws InterruptedException
     *      throws exception if thread is interrupted
     */
    @Test
    public void Case1() throws InterruptedException {
        loginUser();
        solo.assertCurrentActivity("Wrong Activity", RiderMapActivity.class);

        solo.clickOnButton("Pick Up at My Location");
        String address = solo.getView(R.id.pick_up_auto_complete).toString();
        Thread.sleep(2000);

        logoutUser();

        Thread.sleep(2000);
    }


    /**
     * Tests request cancelling when user is waiting for driver offer.
     * @throws InterruptedException
     *      throws exception if thread is interrupted
     * @throws NumberFormatException
     *      throws exception if string is converted to a number
     */
    @Test
    public void Case2() throws InterruptedException, NumberFormatException {
        loginUser();
        addNewRequest();

        assertTrue(solo.waitForText("Time Elapsed", 1, 2000));

        solo.clickOnView(solo.getView(R.id.add_money));
        assertTrue(solo.waitForText("4.5", 1, 2000));

        solo.clickOnView(solo.getView(R.id.reduce_money));
        assertTrue(solo.waitForText("3.5", 1, 2000));

        solo.clickOnButton("CANCEL REQUEST");
        assertTrue(solo.waitForText("HOP IN NOW!", 1, 2000));

        logoutUser();

        Thread.sleep(2000);
    }

    /**
     * Tests request cancelling when rider is waiting for driver pick up.
     * @throws InterruptedException
     *      throws exception if thread is interrupted
     */
    @Test
    public void Case3() throws InterruptedException {
        loginUser();
        addNewRequest();
        solo.clickOnView(solo.getView(R.id.rider_waiting_driver_next));
        assertTrue(solo.waitForText("responded",
                1, 2000));

        //Tests Dialog
        TextView driverName = (TextView) solo.getView(R.id.rider_driver_offer_name);
        solo.clickOnView(driverName);
        assertTrue(solo.waitForText("Car:",1,2000));
        solo.goBack();


        //Declining driver offer
        solo.clickOnButton("DECLINE");
        Thread.sleep(2000);
        assertTrue(solo.waitForText("Time Elapsed",1,2000));
        solo.clickOnView(solo.getView(R.id.rider_waiting_driver_next));

        //Accepting driver offer then cancelling request
        solo.clickOnButton("ACCEPT");
        solo.clickOnButton("CANCEL REQUEST");
        assertTrue(solo.waitForText("HOP",1,2000));

        logoutUser();

        Thread.sleep(2000);
    }

    /**
     * Test fragments of rider picked up, rider confirming arriving at drop off location, payment
     *      actions,and completing ride without rating
     * @throws InterruptedException
     *       throws exception if thread is interrupted
     */
    @Test
    public void Case4() throws InterruptedException {
        loginUser();
        addNewRequest();
        solo.clickOnView(solo.getView(R.id.rider_waiting_driver_next));
        solo.clickOnButton("ACCEPT");
        solo.clickOnView(solo.getView(R.id.rider_waiting_next_button));
        assertTrue(solo.waitForText("heading to ...",
                1,2000));

        //Tests Dialog
        TextView driverName = (TextView) solo.getView(R.id.rider_pickedup_driver_name);
        solo.clickOnView(driverName);
        assertTrue(solo.waitForText("Car:",1,2000));
        solo.goBack();

        solo.clickOnView(solo.getView(R.id.rider_pickedup_arrived_button));
        assertTrue(solo.waitForText("Payment",1,2000));

        solo.clickOnView(solo.getView(R.id.mock_next_complete_trip));

        Thread.sleep(2000);
    }

    /**
     * Test fragments of rider picked up, rider confirming arriving at drop off location, payment
     *      actions,and completing ride without rating
     * @throws InterruptedException
     *       throws exception if thread is interrupted
     */
    //@Test
    public void Case5() throws InterruptedException {
        logoutUser();
    }





    /**
     * Close activity after each test
     * @throws Exception
     *      throws all exception
     */
    @After
    public void tearDown() throws Exception{
        solo.finishOpenedActivities();
    }
}
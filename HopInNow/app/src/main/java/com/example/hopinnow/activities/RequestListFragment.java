package com.example.hopinnow.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.hopinnow.R;
import com.example.hopinnow.database.DriverDatabaseAccessor;
import com.example.hopinnow.database.DriverRequestDatabaseAccessor;
import com.example.hopinnow.entities.Driver;
import com.example.hopinnow.entities.LatLong;
import com.example.hopinnow.entities.Request;
import com.example.hopinnow.entities.RequestListAdapter;
import com.example.hopinnow.statuslisteners.AvailRequestListListener;
import com.example.hopinnow.statuslisteners.DriverProfileStatusListener;
import com.example.hopinnow.statuslisteners.DriverRequestListener;
import com.example.hopinnow.statuslisteners.RequestAddDeleteListener;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Author: Qianxi Li
 * Version: 1.0.0
 * show the current available request for driver to choose to take
 */
public class RequestListFragment extends Fragment implements DriverProfileStatusListener,
        AvailRequestListListener, DriverRequestListener, RequestAddDeleteListener {
    private ListView requestListView;
    private ArrayList<Request> requestList;
    private Request chooseRequest;
    private LatLong pickUp;
    private LatLong dropOff;
    private Location current;
    private LatLng startUp;
    private Context context;
    private Driver current_driver;
    private DriverDatabaseAccessor driverDatabaseAccessor;
    private DriverRequestDatabaseAccessor driverRequestDatabaseAccessor;
    private ProgressDialog progressDialog;
    private RequestListAdapter requestListAdapter;
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState){
        super.onCreateView(inflater,container,savedInstanceState);
        View view = inflater
                .inflate(R.layout.fragment_driver_requests, container, false);
        requestListView = view.findViewById(R.id.requestList);
        requestList = new ArrayList<>();
        //read request from database
        driverDatabaseAccessor = new DriverDatabaseAccessor();
        this.progressDialog = new ProgressDialog(getContext());
        this.progressDialog.setContentView(R.layout.custom_progress_bar);
        this.progressDialog.show();
        driverDatabaseAccessor.getDriverProfile(this);
        return view;
    }

    /*
    Citation:
    Author: VVB
    Date: Jul 21 '14 at 11:57
    Title: android - listview get item view by position
    Link: https://stackoverflow.com/questions/24811536/android-listview-get-item-view-by-position
     */
    private View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

    @Override
    public void onDriverProfileRetrieveSuccess(Driver driver) {
        this.current_driver = driver;
        driverRequestDatabaseAccessor = new DriverRequestDatabaseAccessor();
        if (((DriverMapActivity) requireNonNull(context)).isUseCurrent()){
            this.current = ((DriverMapActivity)context).getCurrentLoc();
            driverRequestDatabaseAccessor.getAllRequest(new LatLong(current.getLatitude(),
                    current.getLongitude()), this);
        }
        else {
            this.startUp = ((DriverMapActivity) requireNonNull(getActivity())).getStartUpLoc();
            driverRequestDatabaseAccessor.getAllRequest(
                    new LatLong(startUp.latitude, startUp.longitude), this);
        }
    }

    @Override
    public void onDriverProfileRetrieveFailure() {
        Toast.makeText(getContext(), "Weak Internet, try again later",
                Toast.LENGTH_LONG).show();
        driverDatabaseAccessor.getDriverProfile(this);
    }

    @Override
    public void onDriverProfileUpdateSuccess(Driver driver) {

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
        // Shway's comment:
        // I assume this is to setup the request list:
        this.requestList = requests;
        final FragmentActivity fragmentActivity = getActivity();
//        ((DriverMapActivity) Objects.requireNonNull(context)).setButtonInvisible();
        this.requestListAdapter = new RequestListAdapter(requestList, fragmentActivity);
        this.requestListView.setAdapter(this.requestListAdapter);

        // setting the listeners:
        requestListView.setOnItemClickListener((parent, view, position, id) -> {
            ((DriverMapActivity)context).clearMap();
            for(int i=0;i<requestList.size();i++){
                getViewByPosition(i, requestListView).findViewById(R.id.accept_btn)
                        .setVisibility(View.INVISIBLE);
            }
            View itemView = getViewByPosition(position, requestListView);
            Button acceptBtn = itemView.findViewById(R.id.accept_btn);
            acceptBtn.setVisibility(View.VISIBLE);
            chooseRequest = requestList.get(position);
            if (chooseRequest == null) {
                Toast.makeText(getContext(), "This request does not exist!",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            //ui test
            Button nextBtn = itemView.findViewById(R.id.mock_next_pickUpRider);
            nextBtn.setOnClickListener(v -> ((DriverMapActivity) Objects.requireNonNull(context))
                    .switchFragment(R.layout.fragment_driver_pick_rider_up));
            pickUp = chooseRequest.getPickUpLoc();
            LatLng pickUp_loc = new LatLng(pickUp.getLat(),pickUp.getLng());

            ((DriverMapActivity)context).setPickUpLoc(pickUp_loc);
            dropOff = chooseRequest.getDropOffLoc();
            LatLng dropOff_loc = new LatLng(dropOff.getLat(),dropOff.getLng());
            ((DriverMapActivity)context).setDropOffLoc(dropOff_loc);
            ((DriverMapActivity)context).updateBothMarker();
            ((DriverMapActivity)context).setBothMarker(pickUp_loc, dropOff_loc);
            acceptBtn.setOnClickListener(v -> {
                chooseRequest.setDriverEmail(current_driver.getEmail());
                chooseRequest.setCar(current_driver.getCar());
                driverRequestDatabaseAccessor.driverAcceptRequest(chooseRequest,
                        RequestListFragment.this);
                driverRequestDatabaseAccessor.driverListenOnRequestBeforeArrive(chooseRequest,
                            RequestListFragment.this);
                this.progressDialog.show();

            });

            //DO NOT DELETE ui mock next page button (viola)
            /*Button nextBtn = view.findViewById(R.id.mock_toPickUpRider);
            nextBtn.setOnClickListener(v -> {
                ((DriverMapActivity) requireNonNull(getActivity()))
                        .switchFragment(R.layout.fragment_driver_pick_rider_up);
            });*/
        });
        // Shway added this following lines:
        this.progressDialog.dismiss();
        if (((DriverMapActivity) requireNonNull(context)).isUseCurrent()){
            this.current = ((DriverMapActivity)context).getCurrentLoc();
            this.driverRequestDatabaseAccessor
                    .listenOnAllRequests(new LatLong(current.getLatitude(),
                            current.getLongitude()), this);
        }
        else {
            this.startUp = ((DriverMapActivity) requireNonNull(getActivity())).getStartUpLoc();
            this.driverRequestDatabaseAccessor
                    .listenOnAllRequests(
                            new LatLong(startUp.latitude, startUp.longitude),this);
        }
    }

    @Override
    public void onGetRequiredRequestsFailure() {
        Toast.makeText(getContext(), "Internet is too weak!", Toast.LENGTH_SHORT).show();
        driverRequestDatabaseAccessor.getAllRequest(new LatLong(10, 20), this);
    }

    @Override
    public void onAllRequestsUpdateSuccess(ArrayList<Request> requests) {
        this.requestList.clear();
        this.requestList.addAll(requests);
        this.requestListAdapter.notifyDataSetChanged();
        this.requestListView.setAdapter(requestListAdapter);
    }

    @Override
    public void onAllRequestsUpdateError() {

    }

    @Override
    public void onDriverRequestAccept() {
        current_driver.setCurRequest(chooseRequest);
        driverDatabaseAccessor.updateDriverProfile(current_driver, RequestListFragment.this);


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
        driverRequestDatabaseAccessor = new DriverRequestDatabaseAccessor();
        this.progressDialog.dismiss();
        Toast.makeText(context,"Rider has accepted your offer!",Toast.LENGTH_SHORT)
                .show();
        ((DriverMapActivity) Objects.requireNonNull(context)).switchFragment(R.layout.fragment_driver_pick_rider_up);
    }

    @Override
    public void onRequestDeclinedByRider() {
        this.progressDialog.dismiss();
        Toast.makeText(context,"Please find a new request.",Toast.LENGTH_SHORT)
                .show();
        ((DriverMapActivity) Objects.requireNonNull(context)).switchFragment(-1);

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

    }

    @Override
    public void onDriverRequestCompleteFailure() {

    }

    @Override
    public void onWaitOnRatingSuccess(Request request) {

    }

    @Override
    public void onWaitOnRatingError() {

    }

    @Override
    public void onAttach(@NonNull Context context) {
        this.context = context;
        super.onAttach(context);
    }
}

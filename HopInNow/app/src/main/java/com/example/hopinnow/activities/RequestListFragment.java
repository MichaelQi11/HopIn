package com.example.hopinnow.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.hopinnow.R;
import com.example.hopinnow.database.DriverDatabaseAccessor;
import com.example.hopinnow.database.RequestDatabaseAccessor;
import com.example.hopinnow.entities.Driver;
import com.example.hopinnow.entities.Request;
import com.example.hopinnow.entities.RequestListAdapter;
import com.example.hopinnow.entities.User;
import com.example.hopinnow.helperclasses.LatLong;
import com.example.hopinnow.statuslisteners.AvailRequestListListener;
import com.example.hopinnow.statuslisteners.DriverProfileStatusListener;
import com.example.hopinnow.statuslisteners.DriverRequestAcceptListener;
import com.example.hopinnow.statuslisteners.UserProfileStatusListener;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Objects;


public class RequestListFragment extends Fragment implements DriverProfileStatusListener, AvailRequestListListener, DriverRequestAcceptListener {
    private Integer prePosition = -1;
    //private Driver driver;
    private ListView requestListView;
    private ArrayList<Request> requestList;
    private Request chooseRequest;
    private LatLong Loc1 = new LatLong(53.651611, -113.323975);
    private LatLong Loc2 = new LatLong(53.591611, -113.323975);
    private LatLong pickUp;
    private LatLong dropOff;
    private Driver current_driver;
    private DriverDatabaseAccessor driverDatabaseAccessor;
    private RequestDatabaseAccessor requestDatabaseAccessor;
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        super.onCreateView(inflater,container,savedInstanceState);


        View view = inflater.inflate(R.layout.fragment_driver_requests, container, false);
        requestListView = (ListView)view.findViewById(R.id.requestList);
        requestList = new ArrayList<>();
        //read request from database

        driverDatabaseAccessor = new DriverDatabaseAccessor();
        driverDatabaseAccessor.getDriverProfile(this);
        requestDatabaseAccessor = new RequestDatabaseAccessor();
        requestDatabaseAccessor.getAllRequest(this);



        return view;
    }

    /*
    Citation:
    Author: VVB
    Date: Jul 21 '14 at 11:57
    Title: android - listview get item view by position
    Link: https://stackoverflow.com/questions/24811536/android-listview-get-item-view-by-position
     */
    public View getViewByPosition(int pos, ListView listView) {
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

    }

    @Override
    public void onDriverProfileRetrieveFailure() {

    }

    @Override
    public void onDriverProfileUpdateSuccess(Driver driver) {
        ((DriverMapActivity)getActivity()).switchFragment(R.layout.fragment_driver_pick_rider_up);

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
        this.requestList = requests;

        final FragmentActivity fragmentActivity = getActivity();
        ((DriverMapActivity)getActivity()).setButtonInvisible();
        RequestListAdapter adapter = new RequestListAdapter(requestList, fragmentActivity);

        requestListView.setAdapter(adapter);
        requestListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                for(int i=0;i<requestList.size();i++){
                    getViewByPosition(i, requestListView).findViewById(R.id.accept_btn).setVisibility(View.INVISIBLE);
                }
                View itemView = getViewByPosition(position, requestListView);
                Button acceptBtn = itemView.findViewById(R.id.accept_btn);
                acceptBtn.setVisibility(View.VISIBLE);
                chooseRequest = requestList.get(position);
                pickUp = chooseRequest.getPickUpLoc();
                LatLng pickUp_loc = new LatLng(pickUp.getLat(),pickUp.getLng());

                ((DriverMapActivity)getActivity()).setPickUpLoc(pickUp_loc);
                dropOff = chooseRequest.getDropOffLoc();
                LatLng dropOff_loc = new LatLng(dropOff.getLat(),dropOff.getLng());
                ((DriverMapActivity)getActivity()).setDropOffLoc(dropOff_loc);
                ((DriverMapActivity)getActivity()).setMapMarker(null, pickUp_loc);
                ((DriverMapActivity)getActivity()).setMapMarker(null, dropOff_loc);
                /*
                if (prePosition != -1){
                    Button preAcceptBtn = getViewByPosition(position, requestListView).findViewById(R.id.accept_btn);
                    preAcceptBtn.setVisibility(View.INVISIBLE);
                }*/
                prePosition = position;
                acceptBtn.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        chooseRequest.setDriverEmail(current_driver.getEmail());
                        chooseRequest.setCar(current_driver.getCar());
                        requestDatabaseAccessor.driverAcceptRequest(chooseRequest,RequestListFragment.this);
                        //requestDatabaseAccessor.deleteRequest(RequestListFragment.this);
                        //means confirm request

                    }
                });
                //prePosition = position;

            }
        });
    }

    @Override
    public void onGetRequiredRequestsFailure() {

    }

    @Override
    public void onDriverRequestAccept() {
        current_driver.setCurRequest(chooseRequest);
        driverDatabaseAccessor.updateDriverProfile(current_driver, RequestListFragment.this);

    }

    @Override
    public void onDriverRequestTimeoutOrFail() {

    }
}

package com.example.hopinnow.statuslisteners;

import com.example.hopinnow.entities.Request;

/**
 * Author: Shway Wang
 * Version: 1.0.1
 * Handles the events when request is accepted by the driver, and the rider is notified
 */
public interface RiderRequestListener {
    /**
     * Called when the rider's request is accepted by a driver,
     * the request object is returned
     * @param request
     *      the newly updated request object with the driver's email
     */
    void onRiderRequestAcceptedNotify(Request request);

    /**
     * Called when the request made by the rider is not accepted in time or fails to be accepted
     */
    void onRiderRequestTimeoutOrFail();

    /**
     * Called when the rider accepts the driver's request
     */
    void onRiderAcceptDriverRequest();

    /**
     * Called when the rider declines the driver's request
     */
    void onRiderDeclineDriverRequest();

    /**
     * Called when the rider is picked up successfully
     * @param request
     *      the current request object updated
     */
    void onRiderPickedupSuccess(Request request);

    /**
     * Called when the rider is picked failed
     */
    void onRiderPickedupTimeoutOrFail();

    /**
     * Called when the rider is dropped off successfully
     * @param request
     *      the current request object updated
     */
    void onRiderDropoffSuccess(Request request);

    /**
     * Called when the rider is dropped failed
     */
    void onRiderDropoffFail();

    /**
     * Called when the request is completed by the driver
     */
    void onRiderRequestComplete();

    /**
     * Called when the current running reuqest goes wrong or it is timed out
     */
    void onRiderRequestCompletionError();

    /**
     * Called when the rider rates the request successfully
     */
    void onRequestRatedSuccess();

    /**
     * Called when an error happens when the rider tries to rate the request:
     */
    void onRequestRatedError();
}

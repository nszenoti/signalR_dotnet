package com.example.signalr;

import android.util.Log;

import com.google.gson.JsonElement;

/**
 * Supposed to be called by remote.
 */

public class SignalrMirrorUtil {

    @SuppressWarnings("unused")
    public void turnOnMirrorDisplay(HubTurnOnOffModel hubTurnOnOffModel) {
        Log.e("Data ***", hubTurnOnOffModel.getAdvertisementUrl());
    }

    @SuppressWarnings("unused")
    public void mirrorInvoice(HubMirrorInvoiceModel data) {
        Log.e("Data", data.getInvoiceId());
    }

}

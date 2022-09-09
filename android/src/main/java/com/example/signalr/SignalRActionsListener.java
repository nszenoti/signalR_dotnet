package com.example.signalr;

import com.google.gson.JsonElement;

public interface SignalRActionsListener {
    void onConnect(String hubName, String connectionId);

    void onDisconnected(String hubName);

    void onException(String hubName,String exception);

    void onEvent(final String hubName, final String methodName, final JsonElement[] elements);

}

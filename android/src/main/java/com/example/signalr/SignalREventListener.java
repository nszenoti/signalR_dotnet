package com.example.signalr;

import com.google.gson.JsonElement;

public interface SignalREventListener {
    void onConnect(String connectionId);

    void onConnecting();

    void onDisconnected();

    void onException(String exception);

    void onEvent(String methodName, JsonElement[] elements);

}

package com.example.signalr;

public interface Constants {

    interface InvokeMethod {
        String CREATE_AND_CONNECT = "createAndConnect";
        String CONNECT = "connect";
        String DISCONNECT = "disconnect";
        String SUBSCRIBE_TO_HUB = "subscribeToHub";
        String INVOKE_HUB_METHOD = "invokeHubMethod";
        String STATUS = "connectionStatus";
        String ON_EVENT = "onEvent";
        String DISPOSE = "dispose";
    }

    interface HubArgument {
        String HUB_TOKEN = "hubToken";
        String SIGNALR_URL = "signalRURL";
        String HUB_NAME = "hubName";
        String METHOD_NAME = "methodName";
        String ARGS = "args";
        String ENABLE_LOGS = "logStatus";
        String HUB_OBSERVERS = "hubObservers";
    }


}

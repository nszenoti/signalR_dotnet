package com.example.signalr;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.JsonElement;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import io.flutter.Log;
import io.flutter.plugin.common.MethodChannel;
import microsoft.aspnet.signalr.client.Action;
import microsoft.aspnet.signalr.client.ConnectionState;
import microsoft.aspnet.signalr.client.ErrorCallback;
import microsoft.aspnet.signalr.client.InvalidStateException;
import microsoft.aspnet.signalr.client.LogLevel;
import microsoft.aspnet.signalr.client.Logger;
import microsoft.aspnet.signalr.client.SignalRFuture;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;

// generic class to handle any hub with single SignalR connection
public class SignalRConnector {

    private static SignalRConnector instance;
    private HubConnection hubConnection;
    private SignalRActionsListener listener;
    //future purpose for appointment and invoice hub
    private HashMap<String,HubProxy> proxyMap;
    private ArrayList<PublishHolder> publishHolder = new ArrayList<>();
    private HashMap<String, ArrayList<String>> subscribeToEvents;
    private SignalRSettings settings;

    private SignalRConnector(){
        proxyMap = new LinkedHashMap<>();
        subscribeToEvents = new LinkedHashMap<>();
        publishHolder = new ArrayList<>();
    }

    public static synchronized SignalRConnector get() {
        if (null == instance) {
            instance = new SignalRConnector();
        }
        return instance;
    }

    public void setListener(SignalRActionsListener listener){
        if(this.listener == null) {
            this.listener = listener;
        }
    }

    public SignalRActionsListener getListener(){
        return this.listener;
    }

    public void setHubEvents(String hubName, ArrayList<String> events) {
        this.subscribeToEvents.put(hubName, events);
    }



    public void connect(final SignalRSettings settings, boolean isReconnect) {
        if(hubConnection == null || hubConnection.getState() == ConnectionState.Disconnected) {

            if (!isReconnect) {
                try {
                    settings.hubToken = URLEncoder.encode(settings.hubToken, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    System.out.println("SignalRConnector:exception for :" + settings.hubName + "message:" + e.getMessage());
                    e.printStackTrace();
                }
            }
            this.settings = settings;
            this.hubConnection = new HubConnection(settings.url, "hubToken=" + settings.hubToken, false, new Logger() {
                @Override
                public void log(String s, LogLevel logLevel) {
                    if (settings.logStatus)
                        Log.d("Connection Log", s);
                }
            });
            try {
                HubProxy hubProxy = hubConnection.createHubProxy(settings.hubName);//pOSDisplayTerminalHub
//            if(!proxyMap.containsKey(settings.hubName)){
                proxyMap.put(settings.hubName, hubProxy);
//            }
            } catch (InvalidStateException e) {
                if (listener != null) {
                    listener.onException(settings.hubName, e.getMessage());
                }
            }
            SignalRFuture<Void> connectionFuture = hubConnection.start();
            hubConnection.connected(new Runnable() {
                @Override
                public void run() {
                    if (settings.logStatus)
                        Log.d("Connection Log", "SignalR Connected" + settings.hubName);
                    //initiateHubObservers();
                    if (listener != null) {
                        listener.onConnect(settings.hubName, hubConnection.getConnectionId());
                    }
                    subscribeHubEvents(settings.hubName);

                }
            });
            connectionFuture.done(new Action<Void>() {
                @Override
                public void run(Void obj) {
//                Log.d("Connection Log", "DONE");
//                for (SignalREventListener signalREventListener : events)
//                    signalREventListener.onConnect();
                }
            });
            connectionFuture.onError(new ErrorCallback() {
                @Override
                public void onError(Throwable error) {
                    if (error != null && error.getMessage() != null)
                        Log.d("Connection Log", error.getMessage());
                    if (listener != null) {
                        listener.onException(settings.hubName, error != null ? error.getMessage() : "No Error Details");
                    }
                }
            });
        }
    }

    private void subscribeHubEvents(String hubName) {
        ArrayList<String> events = subscribeToEvents.get(hubName);
        if (events != null)
            for (int i = 0; i < events.size(); i++) {
                subscribeToEvent(events.get(i), hubName);
            }
        for (PublishHolder publishHolderObj : publishHolder){
            publishEvent(publishHolderObj.result, publishHolderObj.hubName, publishHolderObj.eventName, publishHolderObj.args);
        }
        publishHolder.clear();
    }

    private void postEvent(final MethodChannel.Result result, final String hubName, final String eventName, Object... args){
        HubProxy currentHubProxy = proxyMap.get(hubName);
        if(currentHubProxy != null) {
            currentHubProxy.invoke(eventName, args).done(new Action<Void>() {
                @Override
                public void run(Void aVoid) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            HashMap<String, String> obj = new HashMap<>();
                            obj.put("status", "true");
                            result.success(obj);
                        }
                    });
//                connectionFuture.setResult(aVoid);
                }
            }).onError(new ErrorCallback() {
                @Override
                public void onError(Throwable throwable) {
//                connectionFuture.triggerError(throwable);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            HashMap<String, String> obj = new HashMap<>();
                            obj.put("status", "false");
                            result.success(obj);
                        }
                    });
                }
            });
        }
    }


    public void publishEvent(final MethodChannel.Result result, final String hubName, final String eventName, final Object... args) {
        if(hubConnection == null || (hubConnection.getState() != ConnectionState.Connected)){
            PublishHolder pHolder = new PublishHolder();
            pHolder.args = args;
            pHolder.eventName = eventName;
            pHolder.hubName = hubName;
            pHolder.result = result;
            publishHolder.add(pHolder);
            if(hubConnection != null && hubConnection.getState() == ConnectionState.Disconnected){
                connect(settings, true);
            }
        }else{
            postEvent(result,hubName,eventName,args);
        }
    }

    public void subscribeToEvent(final String eventName, final String hubName) {
        HubProxy currentHubProxy = proxyMap.get(hubName);
        if(currentHubProxy != null) {
            currentHubProxy.subscribe(eventName).addReceivedHandler(new Action<JsonElement[]>() {
                @Override
                public void run(JsonElement[] obj) {
                    listener.onEvent(hubName,eventName,obj);
                }
            });
        }
    }

    public void reset(){
        instance = null;
        hubConnection = null;
        listener = null;
        proxyMap = null;
        subscribeToEvents = null;
    }

    public void disconnect(){
        if(hubConnection != null) {
            hubConnection.disconnect();
        }
        reset();
    }

    static class PublishHolder {
        public MethodChannel.Result result;
        public String hubName;
        public String eventName;
        public Object[] args;
    }

}

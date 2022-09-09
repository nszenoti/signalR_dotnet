package com.example.signalr;


import android.os.Handler;
import android.os.Looper;

import com.google.gson.JsonElement;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodChannel;
import microsoft.aspnet.signalr.client.Action;
import microsoft.aspnet.signalr.client.ErrorCallback;
import microsoft.aspnet.signalr.client.InvalidStateException;
import microsoft.aspnet.signalr.client.LogLevel;
import microsoft.aspnet.signalr.client.Logger;
import microsoft.aspnet.signalr.client.SignalRFuture;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;

public class SignalRConnect {
    private static SignalRConnect instance;
    private SignalREventListener events;
    private HubConnection connection;
    private HubProxy hubProxy;
    private SignalRFuture<Void> connectionFuture;
    private ArrayList<String> hubObservers;
    public int isSingle = 0;

    private SignalRConnect() {
    }


    public static synchronized SignalRConnect get() {
        if (null == instance) {
            instance = new SignalRConnect();
        }
        return instance;
    }



    public void addEvent(SignalREventListener signalRInterface) {
        events = signalRInterface;
    }


    public void connect(final SignalRSettings settings) {
        try {
            settings.hubToken = URLEncoder.encode(settings.hubToken, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        this.connection = new HubConnection(settings.url, "hubToken=" + settings.hubToken, false, new Logger() {
            @Override
            public void log(String s, LogLevel logLevel) {
                if (settings.logStatus)
                    android.util.Log.d("Connection Log", s);
            }
        });
        try {
            hubProxy = connection.createHubProxy(settings.hubName);//pOSDisplayTerminalHub
        } catch (InvalidStateException e) {
            if (events != null)
                events.onException(e.getMessage());
        }


        connection.connected(new Runnable() {
            @Override
            public void run() {
                if (isSingle == 0) {
                    initiateHubObservers();
                }
                if (events != null)
                    events.onConnect(connection.getConnectionId());
            }
        });

        if (events != null)
            events.onConnecting();

        connectionFuture = connection.start();

//        connection.reconnecting(new Runnable() {
//            @Override
//            public void run() {
//                Log.d("Connection Log", "CONNECTING ...");
//            }
//        });
//        connection.reconnected(new Runnable() {
//            @Override
//            public void run() {
//                //Crashlytics.log("SignalR reconnected");
//                Log.d("Connection Log", "RECONNECTED");
//            }
//        });

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
                try {
                    if (events != null) {
                        events.onDisconnected();
                        events.onException(error.getMessage());
                    }
                } catch (Exception ignored) {
                }
            }
        });


    }

    private void initiateHubObservers() {
        if (connectionFuture != null && hubObservers != null) {
            for (String observer : hubObservers) {
                subscribeToEvent(observer);
            }
        }
    }

    public void subscribeObj() {
        hubProxy.subscribe(new SignalrMirrorUtil());
    }


    public void subscribeToEvent(final String eventName) {
        hubProxy.subscribe(eventName).addReceivedHandler(new Action<JsonElement[]>() {
            @Override
            public void run(JsonElement[] obj) {
                if (events != null) {
                    events.onEvent(eventName, obj);
                }
            }
        });
    }

    public void invokeMethod(final MethodChannel.Result result, String eventName, Object... args) {
        hubProxy.invoke(eventName, args).done(new Action<Void>() {
            @Override
            public void run(Void aVoid) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Map<String, String> obj = new HashMap<>();
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
                        Map<String, String> obj = new HashMap<>();
                        obj.put("status", "false");
                        result.success(obj);
                    }
                });
            }
        });
    }

    public void addHubObservers(ArrayList<String> hubObservers) {
        this.hubObservers = hubObservers;
    }

    public void disconnect(MethodChannel.Result result) {
        if (connection != null) {
            for (String observer : hubObservers) {
                hubProxy.removeSubscription(observer);
            }
            connection.disconnect();
        }
        clearAll();
    }

    private void clearAll() {
        hubProxy = null;
        connection = null;
        instance = null;
    }
}

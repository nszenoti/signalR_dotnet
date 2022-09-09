package com.example.signalr;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.example.signalr.Constants.HubArgument;
import com.google.gson.JsonElement;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;


public class SignalRManager implements FlutterPlugin, MethodCallHandler, SignalRActionsListener {
    private MethodChannel channel;
    private final Handler uiThreadHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "com.zenoti.mpos/defaultSignalr");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals(Constants.InvokeMethod.CREATE_AND_CONNECT)) {
            String url = call.argument(HubArgument.SIGNALR_URL);
            if (url != null && !url.contains("signalr")) {
                url = url + (url.endsWith("/") ? "signalr" : "/signalr");
            }
            final String hubName = call.argument(HubArgument.HUB_NAME);
            final String signalRToken = call.argument(HubArgument.HUB_TOKEN);
            final String enableLogs = call.argument(HubArgument.ENABLE_LOGS);
            if (call.argument(HubArgument.HUB_OBSERVERS) instanceof ArrayList) {
                final ArrayList<String> hubObservers = call.argument(HubArgument.HUB_OBSERVERS);
                SignalRConnect.get().addHubObservers(hubObservers);
            }
            SignalRConnector.get().setListener(this);
            SignalRSettings signalRSettings = new SignalRSettings(url,signalRToken,hubName,enableLogs);
            SignalRConnector.get().connect(signalRSettings, false);

        } else if (call.method.equalsIgnoreCase(Constants.InvokeMethod.SUBSCRIBE_TO_HUB)) {
            final String method = call.argument(HubArgument.METHOD_NAME);
            final String hubName = call.argument(HubArgument.HUB_NAME);
            SignalRConnector.get().subscribeToEvent(method,hubName);
        } else if (call.method.equals(Constants.InvokeMethod.INVOKE_HUB_METHOD)) {
            final String method = call.argument(HubArgument.METHOD_NAME);
            final String hubName = call.argument(HubArgument.HUB_NAME);
            if (call.argument(HubArgument.ARGS) instanceof ArrayList) {
                ArrayList<Object> argsArray = call.argument(HubArgument.ARGS);
                Object[] invokeObjs = argsArray.toArray();
                SignalRConnector.get().publishEvent(result, hubName, method, invokeObjs);
            } else {
                Object args = call.argument(HubArgument.ARGS);
                SignalRConnector.get().publishEvent(result, hubName, method, args);
            }
        } else if (call.method.equals(Constants.InvokeMethod.DISPOSE)) {
            //SignalRConnector.get().disconnect(result);
        } else {
            result.notImplemented();
        }
    }



    private Object getMapObj(JsonElement[] elements) {
        if (elements != null && elements.length > 0) {
            HashMap<String, Object> finalData = new HashMap<>();
            JSONObject data = null;
            try {
                data = new JSONObject(elements[0].toString());
                Iterator<String> keys = data.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object dataElement = data.get(key);
                    if (dataElement instanceof JSONObject) {
                        dataElement = getFinalElement((JSONObject) dataElement);
                    }
                    finalData.put(key, dataElement);
                }
                return finalData;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }

        }
        return null;
    }

    private Object getFinalElement(JSONObject dataElement) {
        HashMap<String, Object> finalData = new HashMap<>();
        Iterator<String> keys = dataElement.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                Object dataObject = dataElement.get(key);
                if (dataObject instanceof JSONObject) {
                    dataObject = getFinalElement((JSONObject) dataObject);
                }
                finalData.put(key, dataObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return finalData;
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }


    private void onStatusUpdated(final  String hubName, final HashMap<String, Object> obj) {
        uiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                channel.invokeMethod(Constants.InvokeMethod.STATUS, obj);
            }
        });
    }

    @Override
    public void onConnect(String hubName, String connectionId) {
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("status", "connected");
        obj.put(Constants.HubArgument.HUB_NAME, hubName);
        HashMap<String, String> args = new HashMap<>();
        args.put("connectionID", connectionId);
        obj.put("args", args);
        onStatusUpdated(hubName, obj);
    }

    @Override
    public void onDisconnected(String hubName) {
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("status", "disconnected");
        obj.put(Constants.HubArgument.HUB_NAME, hubName);
        onStatusUpdated(hubName, obj);
    }

    @Override
    public void onException(String hubName, String exception) {
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("status", "error");
        obj.put(Constants.HubArgument.HUB_NAME, hubName);
        HashMap<String, String> args = new HashMap<>();
        args.put("message", exception);
        obj.put("args", args);
        onStatusUpdated(hubName, obj);
    }

    @Override
    public void onEvent(final String hubName, final String methodName, final JsonElement[] elements) {
        uiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                HashMap<String, Object> obj = new HashMap<>();
                obj.put("methodName", methodName);
                obj.put("args", getMapObj(elements));
                //obj.put(HubArgument.HUB_NAME,hubName); uncomment if required
                channel.invokeMethod(Constants.InvokeMethod.ON_EVENT, obj);
            }
        });
    }
}

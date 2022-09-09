package com.example.signalr;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.gson.JsonElement;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;


public class InvoiceSignalRManager implements FlutterPlugin, MethodChannel.MethodCallHandler, SignalRActionsListener {

    private MethodChannel channel;
    private final Handler uiThreadHandler = new Handler(Looper.getMainLooper());
    private String hubName;
    private String CONNECTION_RESULT = "connection_result";

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        channel = new MethodChannel(binding.getBinaryMessenger(), "com.zenoti.mpos/flutter_invoice_signalR");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        String invoiceId;
        switch (call.method){
            case "StartInvoiceHubConnection":
                String url = call.argument(Constants.HubArgument.SIGNALR_URL);
                if (url != null && !url.contains("signalr")) {
                    url = url + (url.endsWith("/") ? "signalr" : "/signalr");
                }
                hubName = call.argument(Constants.HubArgument.HUB_NAME);
                final String signalRToken = call.argument(Constants.HubArgument.HUB_TOKEN);
                SignalRConnector.get().setListener(this);
                ArrayList<String> events = new ArrayList<>();
                events.add("NotifyInvoiceOpen");
                SignalRConnector.get().setHubEvents(hubName, events);
                SignalRConnector.get().connect(new SignalRSettings(url, signalRToken, hubName, null), false);
                break;
            case "DisconnectInvoiceHubConnection":
                SignalRConnector.get().disconnect();
                break;
            case "JoinInvoice":
                invoiceId =  call.argument("InvoiceId");
                SignalRConnector.get().publishEvent(result,hubName,"JoinInvoice", invoiceId);
                break;
            case "LeaveConnect":
                invoiceId =  call.argument("InvoiceId");
                SignalRConnector.get().publishEvent(result,hubName,"LeaveConnect", invoiceId);
                SignalRConnector.get().disconnect();
                break;
            case "NotifyInvoiceOpen":
                // publish and subscribe
                invoiceId =  call.argument("InvoiceId");
                String userName =  call.argument("UserName");
                HashMap<String, Object> obj = new HashMap<>();
                obj.put("InvoiceId", invoiceId);
                obj.put("UserName", userName);
                SignalRConnector.get().publishEvent(result,hubName,"notifyInvoiceOpen", obj);

                break;
            case "resetConnection":
                SignalRConnector.get().reset();
                break;
        }

    }

    @Override
    public void onConnect(String hubName, String connectionId) {
        int CONNECTED = 1;
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("connectionStatus",CONNECTED);
        onStatusUpdated(hubName, obj);
    }

    @Override
    public void onDisconnected(String hubName) {
        int DISCONNECTED = 2;
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("connectionStatus",DISCONNECTED);
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
                //obj.put(HubArgument.HUB_NAME,hubName); uncomment if required
                channel.invokeMethod(methodName, getMapObj(elements));
            }
        });
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

    private void onStatusUpdated(final  String hubName, final HashMap<String, Object> obj) {
        uiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                channel.invokeMethod(CONNECTION_RESULT, obj);
            }
        });
    }
}

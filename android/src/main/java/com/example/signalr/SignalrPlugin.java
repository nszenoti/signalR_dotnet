package com.example.signalr;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.example.signalr.Constants.HubArgument;
import com.google.gson.JsonElement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * SignalrPlugin
 */
public class SignalrPlugin implements FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;
    private final Handler uiThreadHandler = new Handler(Looper.getMainLooper());
    private final InvoiceSignalRManager invoiceSignalRManager;

    public SignalrPlugin() {
        invoiceSignalRManager = new InvoiceSignalRManager();
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "com.zenoti.mpos/signalr");
        channel.setMethodCallHandler(this);
        //flutterPluginBinding.getFlutterEngine().getPlugins().add(invoiceSignalRManager); //deprecated
        invoiceSignalRManager.onAttachedToEngine(flutterPluginBinding);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals(Constants.InvokeMethod.CREATE_AND_CONNECT)) {
            SignalRConnect.get().isSingle = 0;
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
            SignalRConnect.get().connect(new SignalRSettings(url, signalRToken, hubName, enableLogs));
            openEventChannel();
        } else if (call.method.equalsIgnoreCase(Constants.InvokeMethod.SUBSCRIBE_TO_HUB)) {
            final String method = call.argument(HubArgument.METHOD_NAME);
            SignalRConnect.get().subscribeToEvent(method);
        } else if (call.method.equals(Constants.InvokeMethod.INVOKE_HUB_METHOD)) {
            SignalRConnect.get().isSingle = 1;
            final String method = call.argument(HubArgument.METHOD_NAME);
            if (call.argument(HubArgument.ARGS) instanceof ArrayList) {
                ArrayList<Object> argsArray = call.argument(HubArgument.ARGS);
                try {
                    SignalRConnect.get().invokeMethod(result, method, (argsArray != null) ? argsArray.toArray() : null);
                } catch (Exception ignore) {
                }
            } else {
                Object args = call.argument(HubArgument.ARGS);
                SignalRConnect.get().invokeMethod(result, method, args);
            }
        } else if (call.method.equals(Constants.InvokeMethod.DISPOSE)) {
            SignalRConnect.get().disconnect(result);
        } else {
            result.notImplemented();
        }
    }

    private void openEventChannel() {
        SignalRConnect.get().addEvent(new SignalREventListener() {


            @Override
            public void onConnect(String connectionId) {
                Map<String, Object> obj = new HashMap<>();
                obj.put("status", "connected");
                Map<String, String> args = new HashMap<>();
                args.put("connectionID", connectionId);
                obj.put("args", args);
                onStatusUpdated(obj);
            }

            @Override
            public void onConnecting() {
                Map<String, Object> obj = new HashMap<>();
                obj.put("status", "starting");
                onStatusUpdated(obj);
            }

            @Override
            public void onDisconnected() {
                Map<String, Object> obj = new HashMap<>();
                obj.put("status", "disconnected");
                onStatusUpdated(obj);
            }

            @Override
            public void onException(final String exception) {
                Map<String, Object> obj = new HashMap<>();
                obj.put("status", "error");
                Map<String, String> args = new HashMap<>();
                args.put("message", exception);
                obj.put("args", args);
                onStatusUpdated(obj);
            }

            @Override
            public void onEvent(final String methodName, final JsonElement[] elements) {
                uiThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Map<String, Object> obj = new HashMap<>();
                        obj.put("methodName", methodName);
                        obj.put("args", getMapObj(elements));
                        channel.invokeMethod(Constants.InvokeMethod.ON_EVENT, obj);
                    }
                });
            }
        });
    }

    private Object getMapObj(JsonElement[] elements) {
        if (elements != null && elements.length > 0) {
            Map<String, Object> finalData = new HashMap<>();
            JSONObject data;
            try {
                data = new JSONObject(elements[0].toString());
                Iterator<String> keys = data.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object dataElement = data.get(key);
                    if (dataElement instanceof JSONObject) {
                        dataElement = getFinalElement((JSONObject) dataElement);
                        finalData.put(key, dataElement);
                    } else if (dataElement instanceof JSONArray) {
                        JSONArray jsonArray = (JSONArray)dataElement;
                        List<Object> elementList = new ArrayList<>();
                        for (int i = 0; i< jsonArray.length(); i++) {
                            JSONObject jsonObj = jsonArray.getJSONObject(i);
                            Object element = getFinalElement(jsonObj);
                            elementList.add(element);
                        }
                        finalData.put(key, elementList);
                    } else {
                        finalData.put(key, dataElement);
                    }
                }
                return finalData;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            } catch (Exception e) {
                return null;
            }

        }
        return null;
    }

    private Object getFinalElement(JSONObject dataElement) {
        Map<String, Object> finalData = new HashMap<>();
        Iterator<String> keys = dataElement.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                Object dataObject = dataElement.get(key);
                if (dataObject instanceof JSONObject) {
                    dataObject = getFinalElement((JSONObject) dataObject);
                    finalData.put(key, dataObject);
                } else if (dataObject instanceof JSONArray) {
                    // parse nested array elements
                    JSONArray jsonArray = (JSONArray)dataObject;
                    List<Object> elementList = new ArrayList<>();
                    for (int i = 0; i< jsonArray.length(); i++) {
                        JSONObject jsonObj = jsonArray.getJSONObject(i);
                        Object element = getFinalElement(jsonObj);
                        elementList.add(element);
                    }
                    finalData.put(key, elementList);
                } else {
                    finalData.put(key, dataObject);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception ignore) {
            }
        }
        return finalData;
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }


    private void onStatusUpdated(final Map<String, Object> obj) {
        uiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                channel.invokeMethod(Constants.InvokeMethod.STATUS, obj);
            }
        });
    }

}

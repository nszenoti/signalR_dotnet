package com.example.signalr;

public class SignalRSettings {

    public String url;
    public String hubToken;
    public String hubName;
    public boolean logStatus;

    public SignalRSettings(String url, String signalRToken, String hubName, String logStatus) {
        this.url = url;
        this.hubToken = signalRToken;
        this.hubName = hubName;
        this.logStatus = (logStatus != null && logStatus.equalsIgnoreCase("true"));
    }

}

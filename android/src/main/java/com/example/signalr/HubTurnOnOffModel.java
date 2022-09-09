package com.example.signalr;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 * HubTurnOnOffModel.
 * Created by SrinivasG on 30-09-2016.
 */

public class HubTurnOnOffModel implements Parcelable {
    @SerializedName("AdvertisementUrl")
    private String AdvertisementUrl;

    public String getAdvertisementUrl() {
        return AdvertisementUrl;
    }

    public void setAdvertisementUrl(String advertisementUrl) {
        AdvertisementUrl = advertisementUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.AdvertisementUrl);
    }

    public HubTurnOnOffModel() {
    }

    protected HubTurnOnOffModel(Parcel in) {
        this.AdvertisementUrl = in.readString();
    }

    public static final Creator<HubTurnOnOffModel> CREATOR = new Creator<HubTurnOnOffModel>() {
        @Override
        public HubTurnOnOffModel createFromParcel(Parcel source) {
            return new HubTurnOnOffModel(source);
        }

        @Override
        public HubTurnOnOffModel[] newArray(int size) {
            return new HubTurnOnOffModel[size];
        }
    };
}

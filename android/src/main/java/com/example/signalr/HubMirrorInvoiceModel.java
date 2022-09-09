package com.example.signalr;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 * HubMirrorInvoiceModel.
 * Created by Narendranath on 03-10-2016.
 */

public class HubMirrorInvoiceModel implements Parcelable {
    @SerializedName("InvoiceId@SerializedName(")
    private String invoiceId;

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.invoiceId);
    }

    public HubMirrorInvoiceModel() {
    }

    protected HubMirrorInvoiceModel(Parcel in) {
        this.invoiceId = in.readString();
    }

    public static final Creator<HubMirrorInvoiceModel> CREATOR = new Creator<HubMirrorInvoiceModel>() {
        @Override
        public HubMirrorInvoiceModel createFromParcel(Parcel source) {
            return new HubMirrorInvoiceModel(source);
        }

        @Override
        public HubMirrorInvoiceModel[] newArray(int size) {
            return new HubMirrorInvoiceModel[size];
        }
    };
}

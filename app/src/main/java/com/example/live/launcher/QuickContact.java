package com.example.live.launcher;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public final class QuickContact implements Parcelable {
    public final String id;
    public final String name;
    public final String number;
    public final int position;

    public QuickContact(String id, String name, String number, int position) {
        this.id = id;
        this.name = name;
        this.number = number;
        this.position = position;
    }

    protected QuickContact(Parcel in) {
        this.id = in.readString();
        this.name = in.readString();
        this.number = in.readString();
        this.position = in.readInt();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(number);
        dest.writeInt(position);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<QuickContact> CREATOR = new Creator<QuickContact>() {
        @Override
        public QuickContact createFromParcel(Parcel in) {
            return new QuickContact(in);
        }

        @Override
        public QuickContact[] newArray(int size) {
            return new QuickContact[size];
        }
    };

    @Override
    public String toString() {
        return "QuickContact{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", number='" + number + '\'' +
                ", position=" + position +
                '}';
    }
}

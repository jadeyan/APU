/**
 * Copyright 2004-2009 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.ac.core;

import net.cp.engine.SyncProgress;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class allows a SyncProgress object to be serialized and passed over an RPC interface.
 * This is most commonly done to communicate status messages from the SyncEngineService to the UI
 * which resides in a different process.
 * 
 * 
 * @see android.os.Parcelable
 */
public class ParcelableSyncProgress extends SyncProgress implements Parcelable
{
    /**
     * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
     */
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeInt((int)statusHeading);
        out.writeInt((int)statusDetail);
        out.writeInt(totalCount);
        out.writeInt(currentCount);
        out.writeString(itemStatusText);
        out.writeLong(itemTotalCount);
        out.writeLong(itemCurrentCount);
    }

    /**
     * Necessary to implement Parcelable on Android
     */
    public static final Parcelable.Creator<ParcelableSyncProgress> CREATOR =
        new Parcelable.Creator<ParcelableSyncProgress>() {
        public ParcelableSyncProgress createFromParcel(Parcel in)
        {
            return new ParcelableSyncProgress(in);
        }

        public ParcelableSyncProgress[] newArray(int size) {
            return new ParcelableSyncProgress[size];
        }
    };
    
    /**
     * Necessary to implement Parcelable on Android
     */
    protected ParcelableSyncProgress(Parcel in)
    {
        statusHeading = (short)in.readInt();
        statusDetail = (short)in.readInt();
        totalCount = in.readInt();
        currentCount = in.readInt();
        itemStatusText = in.readString();
        itemTotalCount = in.readLong();
        itemCurrentCount = in.readLong();
    }
    
    /**
     * @param progress The SyncProgress object to parcel
     */
    public ParcelableSyncProgress(SyncProgress progress)
    {
        this.statusHeading   = progress.statusHeading;
        this.statusDetail    = progress.statusDetail;
        this.totalCount      = progress.totalCount;
        this.currentCount    = progress.currentCount;
        this.itemStatusText  = progress.itemStatusText;
        this.itemTotalCount  = progress.itemTotalCount;
        this.itemCurrentCount = progress.itemCurrentCount;
    }
    
    /* (non-Javadoc)
     * @see android.os.Parcelable#describeContents()
     */
    public int describeContents()
    {
        return 0;
    }
}

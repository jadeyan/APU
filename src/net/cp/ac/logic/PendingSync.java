/**
 * Copyright 2004-2011 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.ac.logic;

import net.cp.mtk.common.CommonUtils;
import android.os.Parcel;
import android.os.Parcelable;

/**
 *
 *
 * Represents a sync session that needs to be run.
 * It implements Parcelable so that it can be sent over an RPC interface between the
 * service process and the UI process.
 *
 */
public class PendingSync implements Parcelable
{

    /**
     * The name of the Intent used to tell other process to reload their settings
     */
    public static final String PENDING_SYNC_INTENT = "net.cp.ac.intent.PENDING_SYNC";


    /**
     * Means the this sync was triggered by a SIS
     */
    public static final int ORIGIN_SIS = 1;


    /**
     * Means the this sync was triggered by a CIS
     */
    public static final int ORIGIN_CIS = 2;
    
    /**
     * Means this sync was triggered by time
     */
    public static final int ORIGIN_PERIOD = 3;


    /**
     * The name to use when adding an instance of this class as an "extra" to an Intent.
     */
    public static final String extraName = "net.cp.ac.logic.PendingSync.EXTRA";


    /**
     * Describes how this sync was triggered. e.g ORIGIN_SIS
     */
    public int origin;


    /**
     * The media types that should be synced.
     */
    public int mediaTypes;


    /**
     * If this sync was triggered by CIS, this variable may contain the number of changes.
     * May be -1 if the number of changes is unknown. Only valid for CIS!
     */
    public int cisNumChanges;

    /**
     * If this sync was triggered by SIS, this variable will contain the "user data" of teh data SMS.
     * This can be used to recreate the full server alert.
     * @see net.cp.syncml.client.ServerAlert
     */
    public byte[] sisData;

    /**
     * Default constructor, does nothing.
     */
    public PendingSync()
    {
        super();
        sisData = new byte[0];
    }

    /* (non-Javadoc)
     * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
     */
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeInt(origin);
        out.writeInt(mediaTypes);
        out.writeInt(cisNumChanges);
        out.writeByteArray(sisData);
    }

    /**
    * {@inheritDoc}
    *
    *  Includes all the data contained in this instance.
    */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Origin: ");

        if(origin == ORIGIN_CIS)
            builder.append("ORIGIN_CIS");
        else if(origin == ORIGIN_SIS)
            builder.append("ORIGIN_SIS");
        else
            builder.append(origin);

        builder.append("\nmediaTypes: ");
        builder.append(mediaTypes);

        builder.append("\ncisNumChanges: ");
        builder.append(cisNumChanges);

        builder.append("\nsisData: ");
        if(sisData != null && sisData.length > 0)
            try {builder.append(CommonUtils.hexEncode(sisData));}catch (Throwable e) {}

        return builder.toString();

    }

    /**
     * Required to implement Parcelable
     */
    public static final Parcelable.Creator<PendingSync> CREATOR =
        new Parcelable.Creator<PendingSync>() {
        public PendingSync createFromParcel(Parcel in)
        {
            return new PendingSync(in);
        }

        public PendingSync[] newArray(int size) {
            return new PendingSync[size];
        }
    };

    /**
     * Required to implement Parcelable.
     * @param in
     */
    protected PendingSync(Parcel in)
    {
        origin = in.readInt();
        mediaTypes = in.readInt();
        cisNumChanges = in.readInt();

        sisData = in.createByteArray();
    }

    /* (non-Javadoc)
     * @see android.os.Parcelable#describeContents()
     */
    public int describeContents()
    {
        return 0;
    }
}

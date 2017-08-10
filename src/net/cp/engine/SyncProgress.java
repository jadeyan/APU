/**
 * Copyright 2004-2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.engine;

/**
 * This class encapsulates the status/progress of the current sync.
 *
 * @author joconnor
 *
 */
public class SyncProgress
{
    /**
     * The main stage of the sync
     */
    public int statusHeading;

    /**
     * The specific status of the current sync
     */
    public int statusDetail;

    /**
     * If we are processing items,
     * this represents the total number of items.
     * If the total is unknown, this should be ignored or set to 0.
     */
    public int totalCount;

    /**
     * If we are processing items,
     * this represents the current number of items processed.
     * If the current count is unknown, this should be ignored or set to 0.
     */
    public int currentCount;

    /**
     * An optional String that can be associated with the current status.
     */
    public String itemStatusText;

    /**
     * If we are processing large items (e.g. files),
     * this represents the total count within the currently processed item (e.g. total bytes).
     * If not applicable, this should be set to 0 and ignored.
     */
    public long itemTotalCount;

    /**
     * If we are processing large items (e.g. files),
     * this represents the current count/progress within the currently processed item (e.g. bytes transferred).
     * If not applicable, this should be set to 0 and ignored.
     */
    public long itemCurrentCount;

    /**
     * Main constructor, sets all the member variables according to the supplied values.
     *
     * @param statusHeading
     * @param statusDetail
     * @param totalCount
     * @param currentCount
     * @param itemStatusText
     * @param itemTotalCount
     * @param itemCurrentCount
     */
    public SyncProgress(int statusHeading, int statusDetail,
            int totalCount, int currentCount, String itemStatusText,
            long itemTotalCount, long itemCurrentCount)
    {
        set(statusHeading, statusDetail, totalCount,
            currentCount, itemStatusText,itemTotalCount, itemCurrentCount);
    }

    /**
     * Default constructor, does nothing.
     */
    public SyncProgress()
    {

    }

    /**
     * Sets all the member variables according to the supplied values.
     *
     * @param statusHeading
     * @param statusDetail
     * @param totalCount
     * @param currentCount
     * @param itemStatusText
     * @param itemTotalCount
     * @param itemCurrentCount
     */
    public void set(int statusHeading, int statusDetail,
            int totalCount, int currentCount, String itemStatusText,
            long itemTotalCount, long itemCurrentCount)
    {
        this.statusHeading = statusHeading;
        this.statusDetail = statusDetail;
        this.totalCount = totalCount;
        this.currentCount = currentCount;
        this.itemStatusText = itemStatusText;
        this.itemTotalCount = itemTotalCount;
        this.itemCurrentCount = itemCurrentCount;
    }
}

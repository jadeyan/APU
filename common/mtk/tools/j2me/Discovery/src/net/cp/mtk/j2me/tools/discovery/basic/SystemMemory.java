/**
 * Copyright � 2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.mtk.j2me.tools.discovery.basic;


import net.cp.mtk.j2me.tools.discovery.Logger;
import net.cp.mtk.j2me.tools.discovery.DiscoveryMIDlet;


public class SystemMemory
{
    public static void evaluate(DiscoveryMIDlet midlet)
    {
        Logger.log("");
        Logger.log("-----------------------------------");
        Logger.log("JAVA HEAP:");
        Logger.log("");

        //show memory size
        midlet.setTestStatus("Testing heap...");
        logMemory();

        Logger.log("-----------------------------------");
    }

    
    private SystemMemory()
    {
        super();
    }

    private static void logMemory()
    {
        try
        {
            //show total memory
            long totalMemory = Runtime.getRuntime().totalMemory();
            if (totalMemory > 0)
                Logger.log("+ Total:         " + totalMemory + " bytes (" + (totalMemory/1024) + " KB)");
            else
                Logger.logIssue(Logger.SEVERITY_LOW, "Heap: device doesn't report total memory available (reported as '" + totalMemory + "')");
            
            //show free memory
            long freeMemory = Runtime.getRuntime().freeMemory();
            if (freeMemory > 0)
                Logger.log("+ Free:          " + freeMemory + " bytes (" + (freeMemory/1024) + " KB)");
            else
                Logger.logIssue(Logger.SEVERITY_LOW, "Heap: device doesn't report free memory available (reported as '" + freeMemory + "')");

            //check if the heap is dynamic
            if (totalMemory > 0)
            {
                //try to allocate 1MB more than the reported amount of total memory
                boolean dynamicHeap = false;
                int blockSize = (int)totalMemory + (1024 * 1024);
                if (testAllocate(blockSize))
                {
                    Logger.log("+ Dynamic Heap:  Yes (successfully allocated " + blockSize + " bytes (" + (blockSize/1024) + " KB)");
                    dynamicHeap = true;
                }
                else
                {
                    Logger.log("+ Dynamic Heap:  No");
                    dynamicHeap = false;
                }
                
                //if the heap is not dynamic, perform some additional checking
                if (! dynamicHeap)
                {
                    //make sure we have >1Mb of total memory
                    if (totalMemory <= (1024 * 1024))
                        Logger.logIssue(Logger.SEVERITY_CRITICAL, "Heap: heap size (" + totalMemory + " bytes) is insufficient (less than 1Mb)");
                    else if (totalMemory <= (1024 * 1024 * 2))
                        Logger.logIssue(Logger.SEVERITY_MEDIUM, "Heap: heap size (" + totalMemory + " bytes) may be insufficient (less than 2Mb)");

                    //determine the largest block of memory that can be allocated - try to allocate ever smaller blocks of 
                    //memory until it succeeds - if we tried allocating ever larger blocks, we risk fragmenting memory
                    blockSize = (int)totalMemory + (1024 * 1024);
                    for (; blockSize > 0; blockSize = blockSize - 102400)
                    {
                        if (testAllocate(blockSize))
                            break;
                    }
                    Logger.log("+ Largest Block: " + blockSize + " bytes (" + (blockSize/1024) + " KB)");
                }
            }
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_LOW, "Heap: failed to retrieve amount of total/free memory", e);
        }
    }
    
    private static boolean testAllocate(int size)
    {
        try
        {
            byte[] buffer = new byte[size];
            if (buffer.length != size)
            {
                Logger.logIssue(Logger.SEVERITY_CRITICAL, "Heap: allocated block size (" + buffer.length + " bytes) doesn't match expected size (" + size + " bytes)");
                return false;
            }
            
            return true;
        }
        catch (Throwable e)
        {
            return false;
        }
    }
}

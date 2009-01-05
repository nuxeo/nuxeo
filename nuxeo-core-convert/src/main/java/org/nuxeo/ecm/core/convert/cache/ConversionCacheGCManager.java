/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 *
 */
package org.nuxeo.ecm.core.convert.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;

/**
 *
 * Manage GC processing to clean up disk cache
 *
 * @author tiry
 *
 */
public class ConversionCacheGCManager {

    public static final String MAX_DISK_SPACE_USAGE_KEY = "MaxDiskSpaceUsageForCache";

    public static final long MAX_DISK_SPACE_USAGE_KB = 1000;

    private static final Log log = LogFactory.getLog(ConversionCacheGCManager.class);

    private static int gcRuns=0;

    private static int gcCalls=0;

    protected static int getMaxDiskSpaceUsageKB()
    {
        return ConversionServiceImpl.getMaxCacheSizeInKB();
    }

    public static int getGCRuns()
    {
        return gcRuns;
    }

    public static int getGCCalls()
    {
        return gcCalls;
    }


    public static long getCacheSizeInKB()
    {
        long totalSize=0;
        Set<String> cacheKeys = ConversionCacheHolder.getCacheKeys();

        for (String key : cacheKeys)
        {
            ConversionCacheEntry cacheEntry =  ConversionCacheHolder.getCacheEntry(key);
            totalSize+= cacheEntry.getDiskSpaceUsageInKB();
        }
        return totalSize;
    }

    public static boolean gcIfNeeded()
    {
        gcCalls+=1;
        log.debug("GC Thread awake, see if there is some work to be done");

        long totalSize = getCacheSizeInKB();
        long limit = getMaxDiskSpaceUsageKB();

        if (totalSize < limit)
        {
            log.debug("No GC needed, go back to sleep for now");
            return false;
        }

        // do the GC
        long deltaInKB = totalSize-limit;
        log.debug("GC needed to free " + deltaInKB + " KB of data");
        doGC(deltaInKB);
        log.debug("GC terminated");

        return true;
    }

    public static void doGC(long deltaInKB)
    {

        gcRuns+=1;
        Set<String> cacheKeys = ConversionCacheHolder.getCacheKeys();

        Map<Date, String> sortingMap = new HashMap<Date, String>();

        for (String key : cacheKeys)
        {
            ConversionCacheEntry cacheEntry = ConversionCacheHolder.getCacheEntry(key);
            sortingMap.put(cacheEntry.getLastAccessedTime(), key);
        }

        List<Date> accesTimeList = new ArrayList<Date>();

        accesTimeList.addAll(sortingMap.keySet());

        Collections.sort(accesTimeList);

        long deletedVolume=0;

        for (Date accessDate : accesTimeList)
        {
            ConversionCacheEntry cacheEntry = ConversionCacheHolder.getCacheEntry(sortingMap.get(accessDate));

            long deletePotential = cacheEntry.getDiskSpaceUsageInKB();

            deletedVolume+= deletePotential;
            ConversionCacheHolder.removeFromCache(sortingMap.get(accessDate));

            if (deletedVolume > deltaInKB)
                break;
        }
    }

}

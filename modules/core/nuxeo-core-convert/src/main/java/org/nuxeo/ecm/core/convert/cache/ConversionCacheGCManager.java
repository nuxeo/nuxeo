/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import java.util.Objects;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;

/**
 * Manages GC processing to clean up disk cache.
 *
 * @author tiry
 */
public class ConversionCacheGCManager {

    public static final String MAX_DISK_SPACE_USAGE_KEY = "MaxDiskSpaceUsageForCache";

    public static final long MAX_DISK_SPACE_USAGE_KB = 1000;

    private static final Log log = LogFactory.getLog(ConversionCacheGCManager.class);

    private static int gcRuns = 0;

    private static int gcCalls = 0;

    // Utility class.
    private ConversionCacheGCManager() {
    }

    protected static int getMaxDiskSpaceUsageKB() {
        return ConversionServiceImpl.getMaxCacheSizeInKB();
    }

    public static int getGCRuns() {
        return gcRuns;
    }

    public static int getGCCalls() {
        return gcCalls;
    }

    public static long getCacheSizeInKB() {
        long totalSize = 0;
        Set<String> cacheKeys = ConversionCacheHolder.getCacheKeys();

        for (String key : cacheKeys) {
            ConversionCacheEntry cacheEntry = ConversionCacheHolder.getCacheEntry(key);
            if (cacheEntry != null) {
                totalSize += cacheEntry.getDiskSpaceUsageInKB();
            }
        }
        return totalSize;
    }

    public static boolean gcIfNeeded() {
        log.debug("GC Thread awake, see if there is some work to be done");

        long totalSize = getCacheSizeInKB();
        long limit = getMaxDiskSpaceUsageKB();

        if (totalSize < limit) {
            gcCalls += 1;
            log.debug("No GC needed, go back to sleep for now");
            return false;
        }

        // do the GC
        long deltaInKB = totalSize - limit;
        if (limit < 0) {
            // mainly for testing : negative limit means cleanup everything
            deltaInKB = totalSize;
        }
        log.debug("GC needed to free " + deltaInKB + " KB of data");
        doGC(deltaInKB);
        log.debug("GC terminated");
        gcCalls += 1;
        return true;
    }

    public static void doGC(long deltaInKB) {

        Set<String> cacheKeys = ConversionCacheHolder.getCacheKeys();

        Map<Date, String> sortingMap = new HashMap<>();

        for (String key : cacheKeys) {
            ConversionCacheEntry cacheEntry = ConversionCacheHolder.getCacheEntry(key);
            Objects.requireNonNull(cacheEntry, key);
            sortingMap.put(cacheEntry.getLastAccessedTime(), key);
        }

        List<Date> accessTimeList = new ArrayList<>();
        accessTimeList.addAll(sortingMap.keySet());
        Collections.sort(accessTimeList);

        long deletedVolume = 0;
        for (Date accessDate : accessTimeList) {
            ConversionCacheEntry cacheEntry = ConversionCacheHolder.getCacheEntry(sortingMap.get(accessDate));

            long deletePotential = cacheEntry.getDiskSpaceUsageInKB();

            deletedVolume += deletePotential;
            ConversionCacheHolder.removeFromCache(sortingMap.get(accessDate));

            if (deletedVolume > deltaInKB) {
                break;
            }
        }
        gcRuns += 1;
    }

}

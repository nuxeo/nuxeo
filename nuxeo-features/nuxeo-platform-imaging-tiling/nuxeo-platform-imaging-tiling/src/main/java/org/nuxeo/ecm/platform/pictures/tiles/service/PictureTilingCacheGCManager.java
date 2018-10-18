/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.pictures.tiles.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTilingService;
import org.nuxeo.runtime.api.Framework;

/**
 * Manage GC processing to clean up disk cache
 *
 * @author tiry
 */
public class PictureTilingCacheGCManager {

    public static final String MAX_DISK_SPACE_USAGE_KEY = "MaxDiskSpaceUsageForCache";

    public static final long MAX_DISK_SPACE_USAGE_KB = 1000;

    private static final Log log = LogFactory.getLog(PictureTilingCacheGCManager.class);

    private static int gcRuns = 0;

    private static int gcCalls = 0;

    protected static long getMaxDiskSpaceUsageKB() {
        PictureTilingComponent ptc = (PictureTilingComponent) Framework.getService(PictureTilingService.class);
        String maxStr = ptc.getEnvValue(MAX_DISK_SPACE_USAGE_KEY, Long.toString(MAX_DISK_SPACE_USAGE_KB));
        return Long.parseLong(maxStr);
    }

    public static int getGCRuns() {
        return gcRuns;
    }

    public static int getGCCalls() {
        return gcCalls;
    }

    public static long getCacheSizeInKBs() {
        return getCacheSizeInBytes() / 1000;
    }

    public static long getCacheSizeInBytes() {
        long totalSize = 0;
        PictureTilingComponent ptc = (PictureTilingComponent) Framework.getService(PictureTilingService.class);
        Map<String, PictureTilingCacheInfo> cache = ptc.getCache();

        for (String key : cache.keySet()) {
            PictureTilingCacheInfo cacheEntry = cache.get(key);
            totalSize += cacheEntry.getDiskSpaceUsageInBytes();
        }

        return totalSize;
    }

    public static boolean gcIfNeeded() {
        gcCalls += 1;
        log.debug("GC Thread awake, see if there is some work to be done");

        long totalSize = getCacheSizeInKBs();
        long limit = getMaxDiskSpaceUsageKB();

        if (totalSize < limit) {
            log.debug("No GC needed, go back to sleep for now");
            return false;
        }

        // do the GC
        long deltaInKB = totalSize - limit;
        log.debug("GC needed to free " + deltaInKB + " KB of data");
        doGC(deltaInKB);
        log.debug("GC terminated");

        return true;
    }

    public static void doGC(long deltaInKB) {
        gcRuns += 1;
        PictureTilingComponent ptc = (PictureTilingComponent) Framework.getService(PictureTilingService.class);
        Map<String, PictureTilingCacheInfo> cache = ptc.getCache();

        Map<Date, String> sortingMap = new HashMap<Date, String>();

        for (String key : cache.keySet()) {
            PictureTilingCacheInfo cacheEntry = cache.get(key);
            sortingMap.put(cacheEntry.getLastAccessedTime(), key);
        }

        List<Date> accesTimeList = new ArrayList<Date>();

        accesTimeList.addAll(sortingMap.keySet());

        Collections.sort(accesTimeList);

        long deletedVolume = 0;
        for (Date accessDate : accesTimeList) {
            String key = sortingMap.get(accessDate);
            PictureTilingCacheInfo cacheEntry = cache.get(key);

            long deletePotential = cacheEntry.getDiskSpaceUsageInBytes() / (1024);

            if (deletePotential > deltaInKB - deletedVolume) {
                cacheEntry.partialCleanUp(deltaInKB - deletedVolume + 1);
                return;
            } else {
                deletedVolume += deletePotential;
                cacheEntry.cleanUp();
                cache.remove(key);
            }
            if (deletedVolume > deltaInKB)
                break;
        }
    }

}

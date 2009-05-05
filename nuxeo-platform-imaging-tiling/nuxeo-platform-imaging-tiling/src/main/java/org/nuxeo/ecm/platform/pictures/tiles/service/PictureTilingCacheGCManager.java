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
package org.nuxeo.ecm.platform.pictures.tiles.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * Manage GC processing to clean up disk cache
 *
 * @author tiry
 *
 */
public class PictureTilingCacheGCManager {

    public static final String MAX_DISK_SPACE_USAGE_KEY = "MaxDiskSpaceUsageForCache";

    public static final long MAX_DISK_SPACE_USAGE_KB = 1000;

    private static final Log log = LogFactory.getLog(PictureTilingCacheGCManager.class);

    private static int gcRuns = 0;

    private static int gcCalls = 0;

    protected static long getMaxDiskSpaceUsageKB() {
        String maxStr = PictureTilingComponent.getEnvValue(
                MAX_DISK_SPACE_USAGE_KEY,
                Long.toString(MAX_DISK_SPACE_USAGE_KB));
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
        Map<String, PictureTilingCacheInfo> cache = PictureTilingComponent.getCache();

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
        Map<String, PictureTilingCacheInfo> cache = PictureTilingComponent.getCache();

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
            PictureTilingCacheInfo cacheEntry = cache.get(sortingMap.get(accessDate));

            long deletePotential = cacheEntry.getDiskSpaceUsageInBytes() / (1024);

            if (deletePotential > deltaInKB - deletedVolume) {
                cacheEntry.partialCleanUp(deltaInKB - deletedVolume + 1);
                return;
            } else {
                deletedVolume += deletePotential;
                cacheEntry.cleanUp();
                cache.remove(accessDate);
            }
            if (deletedVolume > deltaInKB)
                break;
        }
    }

}

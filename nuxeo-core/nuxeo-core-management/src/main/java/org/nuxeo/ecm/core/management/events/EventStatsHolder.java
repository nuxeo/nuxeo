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
 */
package org.nuxeo.ecm.core.management.events;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;

/**
 * Helper class to store statistics about listeners calls.
 *
 * @author Thierry Delprat
 */
public class EventStatsHolder {

    protected static boolean collectAsyncHandlersExecTime = false;

    protected static boolean collectSyncHandlersExecTime = false;

    protected static Map<String, CallStat> syncStats = new ConcurrentHashMap<>();

    protected static Map<String, CallStat> aSyncStats = new ConcurrentHashMap<>();

    private EventStatsHolder() {
    }

    public static boolean isCollectAsyncHandlersExecTime() {
        return collectAsyncHandlersExecTime;
    }

    public static void setCollectAsyncHandlersExecTime(boolean collectAsyncHandlersExecTime) {
        EventStatsHolder.collectAsyncHandlersExecTime = collectAsyncHandlersExecTime;
    }

    public static boolean isCollectSyncHandlersExecTime() {
        return collectSyncHandlersExecTime;
    }

    public static void setCollectSyncHandlersExecTime(boolean collectSyncHandlersExecTime) {
        EventStatsHolder.collectSyncHandlersExecTime = collectSyncHandlersExecTime;
    }

    /**
     * @since 5.6
     */
    public static void clearStats() {
        syncStats.clear();
        aSyncStats.clear();
    }

    public static void logAsyncExec(EventListenerDescriptor desc, long delta) {
        if (!collectAsyncHandlersExecTime) {
            return;
        }
        String name = desc.getName();
        synchronized (aSyncStats) {
            CallStat stat = aSyncStats.get(name);
            if (stat == null) {
                String label = desc.asPostCommitListener().getClass().getSimpleName();
                if (desc.getIsAsync()) {
                    label += "(async)";
                } else {
                    label += "(sync)";
                }
                stat = new CallStat(label);
                aSyncStats.put(name, stat);
            }
            stat.update(delta);
        }
    }

    public static void logSyncExec(EventListenerDescriptor desc, long delta) {
        if (!collectSyncHandlersExecTime) {
            return;
        }
        String name = desc.getName();
        syncStats.computeIfAbsent(name, k -> new CallStat(desc.asEventListener().getClass().getSimpleName()))
                 .update(delta);
    }

    public static String getAsyncHandlersExecTime() {
        return getStringSummary(aSyncStats);
    }

    /**
     * @since 5.6
     */
    public static Map<String, CallStat> getAsyncHandlersCallStats() {
        return Collections.unmodifiableMap(aSyncStats);
    }

    public static String getSyncHandlersExecTime() {
        return getStringSummary(syncStats);
    }

    /**
     * @since 5.6
     */
    public static Map<String, CallStat> getSyncHandlersCallStats() {
        return Collections.unmodifiableMap(syncStats);
    }

    protected static String getStringSummary(Map<String, CallStat> stats) {
        long totalTime = 0;
        for (CallStat stat : stats.values()) {
            totalTime += stat.getAccumulatedTime();
        }
        if (totalTime == 0) {
            totalTime = 1;
        }
        StringBuilder sb = new StringBuilder();
        for (Entry<String, CallStat> en : stats.entrySet()) {
            String name = en.getKey();
            CallStat stat = en.getValue();
            sb.append(name);
            sb.append(" - ");
            sb.append(stat.getLabel());
            sb.append(" - ");
            sb.append(stat.getCallCount());
            sb.append(" calls - ");
            sb.append(stat.getAccumulatedTime());
            sb.append("ms - ");
            String pcent = String.format("%.2f", 100.0 * stat.getAccumulatedTime() / totalTime);
            sb.append(pcent);
            sb.append("%\n");
        }
        return sb.toString();
    }

    public static void resetHandlersExecTime() {
        clearStats();
    }

}

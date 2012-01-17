/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.core.management.events;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;

/**
 * Helper class to store statistics about listeners calls.
 *
 * @author Thierry Delprat
 */
public class EventStatsHolder {

    protected static boolean collectAsyncHandlersExecTime = false;

    protected static boolean collectSyncHandlersExecTime = false;

    protected static Map<String, CallStat> syncStats = new HashMap<String, CallStat>();

    protected static Map<String, CallStat> aSyncStats = new HashMap<String, CallStat>();

    private EventStatsHolder() {
    }

    public static boolean isCollectAsyncHandlersExecTime() {
        return collectAsyncHandlersExecTime;
    }

    public static void setCollectAsyncHandlersExecTime(
            boolean collectAsyncHandlersExecTime) {
        EventStatsHolder.collectAsyncHandlersExecTime = collectAsyncHandlersExecTime;
    }


    public static boolean isCollectSyncHandlersExecTime() {
        return collectSyncHandlersExecTime;
    }

    public static void setCollectSyncHandlersExecTime(
            boolean collectSyncHandlersExecTime) {
        EventStatsHolder.collectSyncHandlersExecTime = collectSyncHandlersExecTime;
    }

    /**
     * @since 5.6
     */
    public static void clearStats() {
        synchronized(aSyncStats) {
            EventStatsHolder.aSyncStats.clear();
        }
    }

    public static void logAsyncExec(EventListenerDescriptor desc, long delta) {
        if (!collectAsyncHandlersExecTime) {
            return;
        }
        String name = desc.getName();
        synchronized (aSyncStats) {
            CallStat stat = aSyncStats.get(name);
            if (stat == null) {
                String label = desc.asPostCommitListener().getClass()
                        .getSimpleName();
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
        synchronized (syncStats) {
            CallStat stat = syncStats.get(name);
            if (stat == null) {
                String label = desc.asEventListener().getClass()
                        .getSimpleName();
                stat = new CallStat(label);
                syncStats.put(name, stat);
            }
            stat.update(delta);
        }
    }

    public static String getAsyncHandlersExecTime() {
        return getStringSummary(aSyncStats);
    }

    /**
     * @since 5.6
     */
    public static Map<String,CallStat> getAsyncHandlersCallStats() {
        return Collections.unmodifiableMap(aSyncStats);
    }

    public static String getSyncHandlersExecTime() {
        return getStringSummary(syncStats);
    }

    /**
     * @since 5.6
     */
    public static Map<String,CallStat> getSyncHandlersCallStats() {
        return Collections.unmodifiableMap(syncStats);
    }

    protected static String getStringSummary(Map<String, CallStat> stats) {
        StringBuffer sb = new StringBuffer();
        synchronized (stats) {

            long totalTime = 0;
            for (String name : stats.keySet()) {
                totalTime += stats.get(name).getAccumulatedTime();
            }

            for (String name : stats.keySet()) {
                CallStat stat = stats.get(name);
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
        }
        return sb.toString();
    }

    public static void resetHandlersExecTime() {
        synchronized (syncStats) {
            syncStats = new HashMap<String, CallStat>();
        }
        synchronized (aSyncStats) {
            aSyncStats = new HashMap<String, CallStat>();
        }
    }

}

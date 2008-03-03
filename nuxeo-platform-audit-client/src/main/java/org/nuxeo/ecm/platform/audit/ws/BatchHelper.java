package org.nuxeo.ecm.platform.audit.ws;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BatchHelper {

    private static Map<String, BatchInfo> pageInfo = new ConcurrentHashMap<String, BatchInfo>();

    public static BatchInfo getBatchInfo(String sessionId, String dateRange)
    {
        if (!pageInfo.containsKey(sessionId))
        {
            pageInfo.put(sessionId, new BatchInfo(dateRange));
        }
        return pageInfo.get(sessionId);
    }

    public static void resetBatchInfo(String sessionId)
    {
        if (pageInfo.containsKey(sessionId))
        {
            pageInfo.remove(sessionId);
        }
    }
}

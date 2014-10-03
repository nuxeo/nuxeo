package org.nuxeo.elasticsearch;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.audit.ESAuditBackend;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class LogEntryGen {

    protected static Map<String, ExtendedInfo> createExtendedInfos() {
        Map<String, ExtendedInfo> infos = new HashMap<String, ExtendedInfo>();
        ExtendedInfo info = ExtendedInfoImpl.createExtendedInfo(new Long(1));
        infos.put("id", info);
        return infos;
    }

    protected static LogEntry doCreateEntry(String docId, String eventId,
            String category) {
        LogEntry createdEntry = new LogEntryImpl();
        createdEntry.setEventId(eventId);
        createdEntry.setCategory(category);
        createdEntry.setDocUUID(docId);
        createdEntry.setEventDate(new Date());
        createdEntry.setDocPath("/" + docId);
        createdEntry.setRepositoryId("test");
        createdEntry.setExtendedInfos(createExtendedInfos());

        return createdEntry;
    }

    public static void flushAndSync() throws Exception {

        TransactionHelper.commitOrRollbackTransaction();
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
        ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
        
        esa.getClient().admin().indices().prepareFlush(ESAuditBackend.IDX_NAME).execute().actionGet();
        esa.getClient().admin().indices().prepareRefresh(
                ESAuditBackend.IDX_NAME).execute().actionGet();
        TransactionHelper.startTransaction();

    }

    public static void generate(String docName, String eventPrefix, String categoryPrefix, int max) throws Exception {
        List<LogEntry> entries = new ArrayList<>();

        AuditLogger logger = Framework.getLocalService(AuditLogger.class);
        Assert.assertNotNull(logger);

        for (int i = 0; i < max; i++) {
            entries.add(doCreateEntry(docName, eventPrefix + i, categoryPrefix + i % 2));
        }
        logger.addLogEntries(entries);
        flushAndSync();

    }
    
}

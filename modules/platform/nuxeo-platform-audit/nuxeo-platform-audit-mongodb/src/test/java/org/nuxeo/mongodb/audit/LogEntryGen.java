/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Kevin Leturc
 */
package org.nuxeo.mongodb.audit;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class LogEntryGen {

    protected static Map<String, ExtendedInfo> createExtendedInfos() {
        Map<String, ExtendedInfo> infos = new HashMap<>();
        ExtendedInfo info = ExtendedInfoImpl.createExtendedInfo(Long.valueOf(1));
        infos.put("id", info);
        return infos;
    }

    public static LogEntry doCreateEntry(String docId, String eventId, String category) {
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
        TransactionHelper.startTransaction();
        Assert.assertTrue(Framework.getService(AuditLogger.class).await(10, TimeUnit.SECONDS));
    }

    public static void generate(String docName, String eventPrefix, String categoryPrefix, int max) throws Exception {
        List<LogEntry> entries = new ArrayList<>();

        AuditLogger logger = Framework.getService(AuditLogger.class);
        Assert.assertNotNull(logger);

        for (int i = 0; i < max; i++) {
            entries.add(doCreateEntry(docName, eventPrefix + i, categoryPrefix + i % 2));
        }
        logger.addLogEntries(entries);
        flushAndSync();
    }

}

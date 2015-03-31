/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.jaxrs.io;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.jaxrs.io.audit.LogEntryWriter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.8
 */

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
public class LogEntryWriterTest {

    @Inject
    CoreSession session;

    @Inject
    JsonFactory factory;

    @Before
    public void doBefore() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "folder1", "Folder");
        session.createDocument(doc);

        for (int i = 0; i < 3; i++) {
            doc = session.createDocumentModel("/folder1", "doc" + i, "Note");
            session.createDocument(doc);
        }
    }

    @Test
    public void itCanWriteLogEntry() throws Exception {
        DocumentModel folder = session.getDocument(new PathRef("/folder1"));
        String id = folder.getId();

        LogEntry entry = new LogEntryImpl();
        entry.setEventId("documentModified");
        entry.setDocUUID(id);
        entry.setEventDate(new Date());
        entry.setDocPath("/" + id);
        entry.setRepositoryId("test");
        entry.setCategory("Workflow");
        entry.setComment("comment");
        entry.setDocLifeCycle("deleted");
        entry.setLogDate(new Date());
        Map<String, ExtendedInfo> extendedInfo = new HashMap<String, ExtendedInfo>();

        DateTime testDate = new DateTime();
        extendedInfo.put("extInfo1", ExtendedInfoImpl.createExtendedInfo("testString"));
        extendedInfo.put("extInfo2", ExtendedInfoImpl.createExtendedInfo(2L));
        extendedInfo.put("extInfo3", ExtendedInfoImpl.createExtendedInfo(testDate));

        entry.setExtendedInfos(extendedInfo);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = factory.createJsonGenerator(out);

        // When it is written as Json
        LogEntryWriter logEntryWriter = new LogEntryWriter();
        logEntryWriter.writeEntity(jg, entry);
        jg.flush();

        // Then it contains
        ObjectMapper m = new ObjectMapper();
        JsonNode node = m.readTree(out.toString());
        assertEquals("Workflow", node.get("category").getTextValue());
        Iterator<Map.Entry<String, JsonNode>> infos = node.get("extended").getFields();
        int count = 0;
        while (infos.hasNext()) {
            Map.Entry<String, JsonNode> info = infos.next();
            count++;
            if ("extInfo1".equals(info.getKey())) {
                assertEquals("testString", info.getValue().getTextValue());
            }
            if ("extInfo2".equals(info.getKey())) {
                assertEquals(2L, info.getValue().getLongValue());
            }
            if ("extInfo3".equals(info.getKey())) {
                assertEquals(testDate, ISODateTimeFormat.dateTime().parseDateTime(info.getValue().getTextValue()));
            }
        }
        assertEquals(3, count);
    }
}

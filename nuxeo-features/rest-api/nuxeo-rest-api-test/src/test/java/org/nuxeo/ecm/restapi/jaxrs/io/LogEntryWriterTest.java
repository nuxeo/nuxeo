/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.jaxrs.io;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 5.8
 */

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.io")
@Deploy("org.nuxeo.ecm.core.persistence")
@Deploy("org.nuxeo.ecm.platform.audit")
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

        // When it is written as Json
        String json = MarshallerHelper.objectToJson(entry, CtxBuilder.get());

        // Then it contains
        ObjectMapper m = new ObjectMapper();
        JsonNode node = m.readTree(json);
        assertEquals("Workflow", node.get("category").textValue());
        Iterator<Map.Entry<String, JsonNode>> infos = node.get("extended").fields();
        int count = 0;
        while (infos.hasNext()) {
            Map.Entry<String, JsonNode> info = infos.next();
            count++;
            if ("extInfo1".equals(info.getKey())) {
                assertEquals("testString", info.getValue().textValue());
            }
            if ("extInfo2".equals(info.getKey())) {
                assertEquals(2L, info.getValue().longValue());
            }
            if ("extInfo3".equals(info.getKey())) {
                assertEquals(testDate, ISODateTimeFormat.dateTime().parseDateTime(info.getValue().textValue()));
            }
        }
        assertEquals(3, count);
    }
}

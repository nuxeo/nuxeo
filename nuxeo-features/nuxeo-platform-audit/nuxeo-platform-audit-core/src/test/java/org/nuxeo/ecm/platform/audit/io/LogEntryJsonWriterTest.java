/*
 * (C) Copyright 2015-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */
package org.nuxeo.ecm.platform.audit.io;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

@Features(AuditFeature.class)
@Deploy("org.nuxeo.ecm.platform.audit.tests:test-pageprovider-contrib.xml")
public class LogEntryJsonWriterTest extends AbstractJsonWriterTest.External<LogEntryJsonWriter, LogEntry> {

    @Inject
    private PageProviderService pps;

    @Inject
    private CoreSession session;

    public LogEntryJsonWriterTest() {
        super(LogEntryJsonWriter.class, LogEntry.class);
    }

    @Test
    public void test() throws Exception {
        DocumentModel root = session.getDocument(new PathRef("/"));
        PageProvider<?> pp = pps.getPageProvider("DOCUMENT_HISTORY_PROVIDER", null, Long.valueOf(20), Long.valueOf(0),
                new HashMap<>(), root);
        @SuppressWarnings("unchecked")
        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();
        JsonAssert json = jsonAssert(entries.get(0));
        json.properties(14);
        json.has("entity-type").isEquals("logEntry");
        json.has("id").isInt();
        json.has("category").isEquals("eventDocumentCategory");
        json.has("principalName").isEquals(SecurityConstants.SYSTEM_USERNAME);
        json.has("docPath").isEquals("/");
        json.has("docType").isEquals("Root");
        json.has("docUUID").isEquals(root.getId());
        json.has("eventId").isText();
        json.has("repositoryId").isEquals("test");
        json.has("eventDate").isText();
        json.has("logDate").isText();
        try {
            json.has("comment").isText();
        } catch (AssertionError e) {
            json.has("comment").isNull();
        }
        try {
            json.has("docLifeCycle").isText();
        } catch (AssertionError e) {
            json.has("docLifeCycle").isNull();
        }
        json.has("extended").properties(0);
    }

    @Test
    public void testArrayInExtendedInfo() throws Exception {
        Map<String, ExtendedInfo> infos = new HashMap<>();
        infos.put("params", ExtendedInfoImpl.createExtendedInfo(new Object[] { "a simple string" }));

        JsonAssert json = assertLogEntry(infos);
        json.has("extended").properties(1).has("params").isArray().contains("a simple string");
    }

    @Test
    public void testIntegerArrayInExtendedInfo() throws IOException {
        Map<String, ExtendedInfo> infos = new HashMap<>();
        infos.put("params", ExtendedInfoImpl.createExtendedInfo(new Integer[] { 1, 2, 3 }));

        JsonAssert json = assertLogEntry(infos);
        json.has("extended").properties(1).has("params").isArray().contains(1, 2, 3);
    }

    @Test
    public void testEmptyArrayInExtendedInfo() throws IOException {
        Map<String, ExtendedInfo> infos = new HashMap<>();
        infos.put("params", ExtendedInfoImpl.createExtendedInfo(new Integer[] {}));

        JsonAssert json = assertLogEntry(infos);
        json.has("extended").properties(1).has("params").isArray().length(0);
    }

    @Test
    public void testBlobArrayInExtendedInfo() throws IOException {
        Map<String, ExtendedInfo> infos = new HashMap<>();
        infos.put("params",
                ExtendedInfoImpl.createExtendedInfo(new Blob[] { Blobs.createBlob("a simple string blob") }));

        JsonAssert json = assertLogEntry(infos);
        json.has("extended").properties(0);
    }

    @Test
    public void testBlobListInExtendedInfo() throws IOException {
        Map<String, ExtendedInfo> infos = new HashMap<>();
        infos.put("params",
                ExtendedInfoImpl.createExtendedInfo((Serializable) Arrays.asList(Blobs.createBlob("a simple string blob"))));

        JsonAssert json = assertLogEntry(infos);
        json.has("extended").properties(0);
    }

    @Test
    public void testSingleBlobInExtendedInfo() throws IOException {
        Map<String, ExtendedInfo> infos = new HashMap<>();
        infos.put("params", ExtendedInfoImpl.createExtendedInfo(new StringBlob("a simple string blob")));

        JsonAssert json = assertLogEntry(infos);
        assertTrue(json.has("extended")
                       .properties(1)
                       .has("params")
                       .toString()
                       .startsWith("\"org.nuxeo.ecm.core.api.impl.blob.StringBlob@"));
    }

    protected JsonAssert assertLogEntry(Map<String, ExtendedInfo> infos) throws IOException {
        LogEntry logEntry = new LogEntryImpl();
        logEntry.setExtendedInfos(infos);

        JsonAssert json = jsonAssert(logEntry);
        json.properties(14);
        json.has("entity-type").isEquals("logEntry");
        json.has("id").isEquals(0);
        json.has("category").isNull();
        json.has("principalName").isNull();
        json.has("docPath").isNull();
        json.has("docType").isNull();
        json.has("docUUID").isNull();
        json.has("eventId").isNull();
        json.has("repositoryId").isNull();
        json.has("eventDate").isText();
        json.has("logDate").isText();
        json.has("comment").isNull();
        json.has("docLifeCycle").isNull();
        return json;
    }

    @Test
    public void testMapInExtendedInfo() throws Exception {
        Map<String, ExtendedInfo> infos = new HashMap<>();

        HashMap<String, Serializable> infoMap = new HashMap<>();
        infoMap.put("String", "abcde");
        Date now = new Date();
        infoMap.put("Date", now);
        infoMap.put("Boolean", false);
        infoMap.put("Integer", 1);
        infoMap.put("Double", 2.0);
        infoMap.put("Blob", (Serializable) Blobs.createBlob("Some blob"));
        infos.put("params", ExtendedInfoImpl.createExtendedInfo(infoMap));

        LogEntry logEntry = new LogEntryImpl();
        logEntry.setExtendedInfos(infos);

        JsonAssert json = jsonAssert(logEntry);
        json.properties(14);
        json.has("entity-type").isEquals("logEntry");
        json.has("id").isEquals(0);
        json.has("category").isNull();
        json.has("principalName").isNull();
        json.has("docPath").isNull();
        json.has("docType").isNull();
        json.has("docUUID").isNull();
        json.has("eventId").isNull();
        json.has("repositoryId").isNull();
        json.has("eventDate").isText();
        json.has("logDate").isText();
        json.has("comment").isNull();
        json.has("docLifeCycle").isNull();

        JsonAssert params = json.has("extended").properties(1).has("params").isObject();
        params.has("String").isEquals("abcde");
        params.has("Date").isEquals(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(now));
        params.has("Integer").isEquals(1);
        params.has("Double").isEquals(2.0, 0.0);
        params.has("Boolean").isEquals(false);
        params.hasNot("Blob");
    }

}

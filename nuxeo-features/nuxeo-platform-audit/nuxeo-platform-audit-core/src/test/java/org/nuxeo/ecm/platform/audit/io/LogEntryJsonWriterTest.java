/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.platform.audit.io;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@Features(AuditFeature.class)
@LocalDeploy("org.nuxeo.ecm.platform.audit.tests:test-pageprovider-contrib.xml")
public class LogEntryJsonWriterTest extends AbstractJsonWriterTest.External<LogEntryJsonWriter, LogEntry> {

    public LogEntryJsonWriterTest() {
        super(LogEntryJsonWriter.class, LogEntry.class);
    }

    @Inject
    private PageProviderService pps;

    @Inject
    private CoreSession session;

    @Test
    public void test() throws Exception {
        DocumentModel root = session.getDocument(new PathRef("/"));
        PageProvider<?> pp = pps.getPageProvider("DOCUMENT_HISTORY_PROVIDER", null, Long.valueOf(20), Long.valueOf(0),
                new HashMap<String, Serializable>(), root);
        @SuppressWarnings("unchecked")
        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();
        JsonAssert json = jsonAssert(entries.get(0));
        json.properties(13);
        json.has("entity-type").isEquals("logEntry");
        json.has("id").isInt();
        json.has("category").isEquals("eventDocumentCategory");
        json.has("principalName").isEquals("system");
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
    }

}

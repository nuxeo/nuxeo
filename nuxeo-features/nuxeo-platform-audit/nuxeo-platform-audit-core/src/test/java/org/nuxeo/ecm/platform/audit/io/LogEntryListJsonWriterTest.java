/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntryList;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

@Features(AuditFeature.class)
@Deploy("org.nuxeo.ecm.platform.audit.tests:test-pageprovider-contrib.xml")
public class LogEntryListJsonWriterTest extends AbstractJsonWriterTest.External<LogEntryListJsonWriter, List<LogEntry>> {

    public LogEntryListJsonWriterTest() {
        super(LogEntryListJsonWriter.class, List.class, TypeUtils.parameterize(List.class, LogEntry.class));
    }

    @Inject
    private PageProviderService pps;

    @Inject
    private CoreSession session;

    @Test
    public void test() throws Exception {
        DocumentModel root = session.getDocument(new PathRef("/"));
        HashMap<String, Serializable> properties = new HashMap<String, Serializable>();
        String name = "DOCUMENT_HISTORY_PROVIDER";
        @SuppressWarnings("unchecked")
        PageProvider<LogEntry> pp = (PageProvider<LogEntry>) pps.getPageProvider(name, null, 3l, 0l, properties, root);
        LogEntryList list = new LogEntryList(pp);
        JsonAssert json = jsonAssert(list);
        json.properties(19);
        json.has("entity-type").isEquals("logEntries");
        json.has("isPaginable").isTrue();
        json.has("resultsCount").isInt();
        json.has("pageSize").isEquals(pp.getPageSize());
        json.has("maxPageSize").isEquals(pp.getMaxPageSize());
        json.has("currentPageSize").isEquals(pp.getCurrentPageSize());
        json.has("currentPageIndex").isEquals(0);
        json.has("numberOfPages").isEquals(pp.getNumberOfPages());
        json.has("isPreviousPageAvailable").isEquals(pp.isPreviousPageAvailable());
        json.has("isNextPageAvailable").isEquals(pp.isNextPageAvailable());
        json.has("isLastPageAvailable").isEquals(pp.isLastPageAvailable());
        json.has("isSortable").isEquals(pp.isSortable());
        json.has("hasError").isEquals(pp.hasError());
        json.has("errorMessage").isNull();
        json.has("pageIndex").isEquals(pp.getCurrentPageIndex());
        json.has("pageCount").isEquals(pp.getResultsCount());
        json.has("currentPageOffset").isEquals(pp.getCurrentPageOffset());
        json = json.has("entries").isArray();
        json = json.has(0).isObject();
        json.has("entity-type").isEquals("logEntry");
    }

}

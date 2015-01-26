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
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@Features(AuditFeature.class)
@LocalDeploy("org.nuxeo.ecm.platform.audit.tests:test-pageprovider-contrib.xml")
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
        json.properties(17);
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
        json = json.has("entries").isArray();
        json = json.has(0).isObject();
        json.has("entity-type").isEquals("logEntry");
    }

}

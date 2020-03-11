/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.platform.audit.api.document;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.audit.api.DocumentHistoryReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation of the {@link DocumentHistoryReader} interface. This is mainly a wrapper around the
 * {@link DocumentHistoryPageProvider}
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class DocumentHistoryReaderImpl implements DocumentHistoryReader {

    @Override
    public List<LogEntry> getDocumentHistory(DocumentModel doc, long pageIndex, long pageSize) {

        PageProvider<LogEntry> pp = getPageProvider(doc, pageIndex, pageSize);
        return pp.getCurrentPage();
    }

    @Override
    @SuppressWarnings("unchecked")
    public PageProvider<LogEntry> getPageProvider(DocumentModel doc, long pageIndex, long pageSize) {

        PageProviderService pps = Framework.getService(PageProviderService.class);
        PageProvider<LogEntry> pp = (PageProvider<LogEntry>) pps.getPageProvider("DOCUMENT_HISTORY_PROVIDER", null,
                Long.valueOf(pageSize), Long.valueOf(pageIndex), new HashMap<String, Serializable>(), doc);
        return pp;
    }

}

/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * Implementation of the {@link DocumentHistoryReader} interface. This is
 * mainly a wrapper around the {@link DocumentHistoryPageProvider}
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class DocumentHistoryReaderImpl implements DocumentHistoryReader {

    @Override
    public List<LogEntry> getDocumentHistory(DocumentModel doc, long pageIndex,
            long pageSize) throws Exception {

        PageProvider<LogEntry> pp = getPageProvider(doc, pageIndex, pageSize);
        return pp.getCurrentPage();
    }

    @Override
    @SuppressWarnings("unchecked")
    public PageProvider<LogEntry> getPageProvider(DocumentModel doc,
            long pageIndex, long pageSize) throws Exception {

        PageProviderService pps = Framework.getLocalService(PageProviderService.class);
        PageProvider<LogEntry> pp = (PageProvider<LogEntry>) pps.getPageProvider(
                "DOCUMENT_HISTORY_PROVIDER", null, Long.valueOf(pageSize),
                Long.valueOf(pageIndex), new HashMap<String, Serializable>(),
                doc);
        return pp;
    }

}

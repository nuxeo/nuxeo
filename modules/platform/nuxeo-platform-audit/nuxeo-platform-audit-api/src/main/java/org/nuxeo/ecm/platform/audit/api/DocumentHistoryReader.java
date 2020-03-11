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
package org.nuxeo.ecm.platform.audit.api;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.PageProvider;

/**
 * Dedicated interface to browse history of a document
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public interface DocumentHistoryReader {

    /**
     * Retrieve a page of {@link LogEntry} for the history of the input {@link DocumentModel}
     *
     * @param doc
     * @param pageIndex
     * @param pageSize
     * @return
     */
    List<LogEntry> getDocumentHistory(DocumentModel doc, long pageIndex, long pageSize);

    /**
     * Retrieve the {@link PageProvider} of {@link LogEntry} for the history of the input {@link DocumentModel}
     *
     * @param doc
     * @param pageIndex
     * @param pageSize
     * @return
     */
    PageProvider<LogEntry> getPageProvider(DocumentModel doc, long pageIndex, long pageSize);
}

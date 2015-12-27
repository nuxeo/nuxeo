/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: ContentHistoryActions.java 25663 2007-10-04 11:54:15Z cbaican $
 */

package org.nuxeo.ecm.platform.audit.web.listener;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.comment.LinkedDocument;

/**
 * Content history actions business interface.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface ContentHistoryActions extends Serializable {

    /**
     * Computes all log entries for current document.
     */
    List<? extends LogEntry> computeLogEntries();

    /**
     * Computes all log entries for given document.
     */
    List<? extends LogEntry> computeLogEntries(DocumentModel document);

    /**
     * Computes latest logs only.
     */
    List<? extends LogEntry> computeLatestLogEntries();

    Map<Long, String> computeLogEntriesComments();

    Map<Long, LinkedDocument> computeLogEntrieslinkedDocs();

    /**
     * Returns the log comment.
     * <p>
     * This log may be filled automatically when dealing with copy/paste/move log entries.
     *
     * @Deprecated This now handled by the PageProvider
     */
    @Deprecated
    String getLogComment(LogEntry entry);

    /**
     * Returns the log linked document.
     * <p>
     * The linked document is resolved from the log original comment, when dealing with copy/paste/move log entries.
     *
     * @Deprecated This now handled by the PageProvider
     */
    @Deprecated
    LinkedDocument getLogLinkedDocument(LogEntry entry);

    String doSearch();

    SortInfo getSortInfo();

}

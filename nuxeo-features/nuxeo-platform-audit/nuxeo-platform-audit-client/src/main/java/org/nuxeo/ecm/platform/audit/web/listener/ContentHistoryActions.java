/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

import javax.ejb.Remove;

import org.jboss.seam.annotations.Destroy;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.web.listener.ejb.LinkedDocument;

/**
 * Content history actions business interface.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface ContentHistoryActions extends Serializable {

    /**
     * Computes all log entries for current document.
     */
    List<? extends LogEntry> computeLogEntries() throws AuditException;

    /**
     * Computes all log entries for given document.
     */
    List<? extends LogEntry> computeLogEntries(DocumentModel document)
            throws AuditException;

    /**
     * Computes latest logs only.
     */
    List<? extends LogEntry> computeLatestLogEntries() throws AuditException;

    Map<Long, String> computeLogEntriesComments() throws AuditException;

    Map<Long, LinkedDocument> computeLogEntrieslinkedDocs()
            throws AuditException;

    /**
     * Returns the log comment.
     * <p>
     * This log may be filled automatically when dealing with copy/paste/move
     * log entries.
     */
    String getLogComment(LogEntry entry);

    /**
     * Returns the log linked document.
     * <p>
     * The linked document is resolved from the log original comment, when
     * dealing with copy/paste/move log entries.
     */
    LinkedDocument getLogLinkedDocument(LogEntry entry);

    String doSearch();

    SortInfo getSortInfo();

    @Destroy
    @Remove
    void destroy();

}

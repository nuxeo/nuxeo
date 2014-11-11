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
     * Invalidates log entries.
     * <p>
     * The invalidation will be done lazily.
     *
     * @throws AuditException
     */
    void invalidateLogEntries() throws AuditException;

    /**
     * Computes all log entries.
     *
     * @see @factory
     *
     * @throws AuditException
     */
    List<LogEntry> computeLogEntries() throws AuditException;

    /**
     * Computes latest logs only.
     *
     * @see @factory
     *
     * @throws AuditException
     */
    List<LogEntry> computeLatestLogEntries() throws AuditException;

    Map<Long, String> computeLogEntriesComments();

    Map<Long, LinkedDocument> computeLogEntrieslinkedDocs();

    String doSearch() throws AuditException;

    SortInfo getSortInfo();

    @Destroy
    @Remove
    void destroy();

}

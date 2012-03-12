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
     * Retrieve a page of {@link LogEntry} for the history of the input
     * {@link DocumentModel}
     *
     * @param doc
     * @param pageIndex
     * @param pageSize
     * @return
     * @throws Exception
     */
    List<LogEntry> getDocumentHistory(DocumentModel doc, long pageIndex,
            long pageSize) throws Exception;

    /**
     * Retrieve the {@link PageProvider} of {@link LogEntry} for the history of
     * the input {@link DocumentModel}
     *
     * @param doc
     * @param pageIndex
     * @param pageSize
     * @return
     * @throws Exception
     */
    PageProvider<LogEntry> getPageProvider(DocumentModel doc, long pageIndex,
            long pageSize) throws Exception;
}

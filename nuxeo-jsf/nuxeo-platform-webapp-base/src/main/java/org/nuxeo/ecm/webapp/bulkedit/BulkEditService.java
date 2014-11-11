/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.webapp.bulkedit;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Service handling bulk edition of documents.
 *
 * @since 5.7.3
 */
public interface BulkEditService {

    String BULK_EDIT_PREFIX = "bulkEdit/";

    String CONTEXT_DATA = "contextData";

    /**
     * Copy all the marked properties (stored in the ContextData of
     * {@code sourceDoc}) from {@code sourceDoc} to all the {@code targetDocs}.
     *
     * @param session the {@code CoreSession} to use
     * @param sourceDoc the doc where to get the properties to copy
     * @param targetDocs the docs where to set the properties
     */
    void updateDocuments(CoreSession session, DocumentModel sourceDoc,
            List<DocumentModel> targetDocs) throws ClientException;

}

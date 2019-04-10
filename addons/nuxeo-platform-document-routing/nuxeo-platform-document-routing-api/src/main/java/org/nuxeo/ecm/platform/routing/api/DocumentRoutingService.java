/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.routing.api;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;

/**
 * The DocumentRoutingService allows to start new {@link DocumentRoute}
 * instance.
 *
 * @author arussel
 *
 */
public interface DocumentRoutingService {
    /**
     * Create a new {@link DocumentRoute} instance from this
     * {@link DocumentRoute} model.
     *
     * @param model The model used to create the instance.
     * @param documents The list of document bound to the instance.
     * @param startInstance if the {@link DocumentRoute} is automatically
     *            started.
     * @return the created {@link DocumentRoute} instance.
     */
    DocumentRoute createNewInstance(DocumentRoute model,
            List<String> documentIds, CoreSession session,
            boolean startInstance);

    /**
     * @see #createNewInstance(DocumentRoute, List, CoreSession, boolean) with
     *      only one document attached.
     */
    DocumentRoute createNewInstance(DocumentRoute model,
            String documentId, CoreSession session, boolean startInstance);

    /**
     * @see #createNewInstance(DocumentRoute, List, CoreSession, boolean) with
     *      startInstance <code>true</code>
     */
    DocumentRoute createNewInstance(DocumentRoute model,
            List<String> documentIds, CoreSession session);

    /**
     * @see #createNewInstance(DocumentRoute, List, CoreSession, boolean) with
     *      startInstance <code>true</code> and only one document attached.
     */
    DocumentRoute createNewInstance(DocumentRoute model,
            String documentId, CoreSession session);

    /**
     * Return the list of available {@link DocumentRoute} model for this
     * session.
     *
     * @param session The session used to query the {@link DocumentRoute}.
     * @return A list of available {@link DocumentRoute}
     */
    List<DocumentRoute> getAvailableDocumentRouteModel(CoreSession session);
}

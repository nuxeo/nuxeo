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

package org.nuxeo.ecm.restapi.server.jaxrs.adapters;

import org.nuxeo.ecm.automation.core.util.Paginable;
import org.nuxeo.ecm.automation.jaxrs.io.documents.PaginableDocumentModelListImpl;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.PageProvider;

/**
 * Adapter that returns a list of {{@link DocumentModel}s.
 *
 * @since 5.7.3
 */
public abstract class DocumentModelListPaginableAdapter extends
        PaginableAdapter<DocumentModel> {

    @Override
    protected Paginable<DocumentModel> getPaginableEntries(
            PageProvider<DocumentModel> pageProvider) {
        return new PaginableDocumentModelListImpl(pageProvider, "restdocid");
    }
}

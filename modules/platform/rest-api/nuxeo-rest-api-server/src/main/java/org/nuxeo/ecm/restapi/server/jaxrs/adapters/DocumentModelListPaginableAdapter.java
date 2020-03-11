/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
public abstract class DocumentModelListPaginableAdapter extends PaginableAdapter<DocumentModel> {

    @Override
    protected Paginable<DocumentModel> getPaginableEntries(PageProvider<DocumentModel> pageProvider) {
        return new PaginableDocumentModelListImpl(pageProvider, "restdocid");
    }
}

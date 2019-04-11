/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.ui.tree.document;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.webengine.ui.tree.ContentProvider;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
// FIXME: properly handle exceptions
public class DocumentContentProvider implements ContentProvider {

    private static final Log log = LogFactory.getLog(DocumentContentProvider.class);

    private static final long serialVersionUID = 1L;

    protected CoreSession session;

    public DocumentContentProvider(CoreSession session) {
        this.session = session;
    }

    public void setSession(CoreSession session) {
        this.session = session;
    }

    public CoreSession getSession() {
        return session;
    }

    @Override
    public Object[] getElements(Object input) {
        if (input instanceof Repository) {
            return new DocumentModel[] { session.getRootDocument() };
        } else { // may be a document
            return getChildren(input);
        }
    }

    @Override
    public Object[] getChildren(Object obj) {
        if (obj instanceof DocumentModel) {
            DocumentModelList list = session.getChildren(((DocumentModel) obj).getRef());
            return list.toArray(new DocumentModel[list.size()]);
        }
        return null;
    }

    @Override
    public boolean isContainer(Object obj) {
        if (obj instanceof DocumentModel) {
            return ((DocumentModel) obj).isFolder();
        }
        return false;
    }

    @Override
    public String getLabel(Object obj) {
        if (obj instanceof DocumentModel) {
            return ((DocumentModel) obj).getTitle();
        }
        return null;
    }

    @Override
    public String[] getFacets(Object object) {
        return null;
    }

    @Override
    public String getName(Object obj) {
        if (obj instanceof DocumentModel) {
            return ((DocumentModel) obj).getName();
        }
        return null;
    }

}

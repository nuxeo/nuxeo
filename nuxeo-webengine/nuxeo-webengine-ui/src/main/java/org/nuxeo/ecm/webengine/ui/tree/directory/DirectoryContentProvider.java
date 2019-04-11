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

package org.nuxeo.ecm.webengine.ui.tree.directory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.webengine.ui.tree.ContentProvider;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DirectoryContentProvider implements ContentProvider {

    private static final Log log = LogFactory.getLog(DirectoryContentProvider.class);

    private static final long serialVersionUID = 1L;

    protected final Session session;

    public DirectoryContentProvider(Session session) {
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    protected void createQuery(DocumentModel parent, Map<String, Serializable> query) {
        String id = parent == null ? null : parent.getId();
        query.put("parent", id);
    }

    @Override
    public Object[] getElements(Object input) {
        if (input instanceof Directory) {
            return getChildren(null);
        } else { // may be a document
            return getChildren(input);
        }
    }

    @Override
    public Object[] getChildren(Object obj) {
        if (obj == null || obj instanceof DocumentModel) {
            DocumentModel parent = (DocumentModel) obj;
            Map<String, Serializable> args = new HashMap<>();
            createQuery(parent, args);
            DocumentModelList list = session.query(args);
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
    public String getName(Object obj) {
        if (obj instanceof DocumentModel) {
            return ((DocumentModel) obj).getId();
        }
        return null;
    }

    @Override
    public String getLabel(Object obj) {
        if (obj instanceof DocumentModel) {
            return ((DocumentModel) obj).getName();
        }
        return null;
    }

    @Override
    public String[] getFacets(Object object) {
        return null;
    }

}

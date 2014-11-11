/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.webengine.ui.tree.ContentProvider;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
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

    protected void createQuery(DocumentModel parent, Map<String,Serializable> query) {
        String id = parent == null ? null : parent.getId();
        query.put("parent", id);
    }

    public Object[] getElements(Object input) {
        if (input instanceof Directory) {
            return getChildren(null);
        } else { // may be a document
            return getChildren(input);
        }
    }

    public Object[] getChildren(Object obj) {
        try {
            if (obj == null || obj instanceof DocumentModel) {
                DocumentModel parent = (DocumentModel)obj;
                Map<String, Serializable> args = new HashMap<String, Serializable>();
                createQuery(parent, args);
                DocumentModelList list = session.query(args);
                return list.toArray(new DocumentModel[list.size()]);
            }
        } catch (ClientException e) {
            log.error(e, e);
        }
        return null;
    }

    public boolean isContainer(Object obj) {
        if (obj instanceof DocumentModel) {
            return ((DocumentModel)obj).isFolder();
        }
        return false;
    }

    public String getName(Object obj) {
        if (obj instanceof DocumentModel) {
            return ((DocumentModel)obj).getId();
        }
        return null;
    }

    public String getLabel(Object obj) {
        if (obj instanceof DocumentModel) {
            return ((DocumentModel)obj).getName();
        }
        return null;
    }

    public String[] getFacets(Object object) {
        return null;
    }

}

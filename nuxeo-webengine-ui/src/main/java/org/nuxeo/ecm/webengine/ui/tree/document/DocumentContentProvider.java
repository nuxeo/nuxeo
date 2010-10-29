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

package org.nuxeo.ecm.webengine.ui.tree.document;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
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

    public Object[] getElements(Object input) {
        if (input instanceof Repository) {
            try {
                return new DocumentModel[]{session.getRootDocument()};
            } catch (ClientException e) {
                log.error(e, e);
                return null;
            }
        } else { // may be a document
            return getChildren(input);
        }
    }

    public Object[] getChildren(Object obj) {
        if (obj instanceof DocumentModel) {
            try {
                DocumentModelList list = session.getChildren(((DocumentModel) obj).getRef());
                return list.toArray(new DocumentModel[list.size()]);
            } catch (ClientException e) {
                log.error(e, e);
            }
        }
        return null;
    }

    public boolean isContainer(Object obj) {
        if (obj instanceof DocumentModel) {
            return ((DocumentModel) obj).isFolder();
        }
        return false;
    }

    public String getLabel(Object obj) {
        if (obj instanceof DocumentModel) {
            try {
                return ((DocumentModel) obj).getTitle();
            } catch (ClientException e) {
                log.error(e, e);
            }
        }
        return null;
    }

    public String[] getFacets(Object object) {
        return null;
    }

    public String getName(Object obj) {
        if (obj instanceof DocumentModel) {
            return ((DocumentModel) obj).getName();
        }
        return null;
    }

}

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

package org.nuxeo.ecm.webengine.ui.tree;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DirectoryContentProvider implements ContentProvider {

    protected CoreSession session;

    public DirectoryContentProvider(CoreSession session) {
        this.session = session;
    }

    public Object[] getChildren(Object obj) {
        if (obj instanceof DocumentModel) {
            try {
                DocumentModelList list = session.getChildren(((DocumentModel)obj).getRef());
                return list.toArray(new DocumentModel[list.size()]);
            } catch (ClientException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public boolean hasChildren(Object obj) {
        if (obj instanceof DocumentModel) {
            return ((DocumentModel)obj).isFolder();
        }
        return false;
    }

    public String getName(Object obj) {
        if (obj instanceof DocumentModel) {
            return ((DocumentModel)obj).getName();
        }
        return null;
    }

}

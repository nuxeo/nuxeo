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

package org.nuxeo.ecm.webengine.actions;


import java.io.IOException;

import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webengine.WebContext;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.WebObject;
import org.nuxeo.ecm.webengine.forms.FormData;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CreateActionHandler implements ActionHandler {

    public void run(WebObject object) throws WebException {
        DocumentModel parent = object.getDocument();
        String name = object.getWebContext().getFirstUnresolvedSegment();
        if (name == null) { // try the name attribute
            name = object.getWebContext().getForm().getString("name");
        }
        if (name == null) { /// create a child with a generated name
            DocumentModel doc = createSubPage(parent, null, object.getWebContext());
            String path = object.getUrlPath();
            if (path.endsWith("/")) {
                path += doc.getName();
            } else {
                path += '/' + doc.getName();
            }
            try {
                object.getWebContext().getResponse().sendRedirect(path);
            } catch (IOException e) {
                throw new WebException("Failed to redirect to the newly created page: "+path, e);
            }
        } else {
            DocumentModel doc = createSubPage(parent, name, object.getWebContext());
            object.getWebContext().resolveFirstUnresolvedSegment(doc);
        }
    }

    private static DocumentModel createSubPage(DocumentModel parent, String name,
            WebContext context) throws WebException {
        try {
            CoreSession session = context.getCoreSession();
            FormData form = context.getForm();
            String type = form.getDocumentType();
            if (type == null) {
                throw new WebException("Invalid argument exception. Nos doc type specified");
            }
            String path = parent.getPathAsString();
            // TODO  not the best method to create an unnamed doc - should refactor core API
            if (name == null) {
                name = form.getDocumentTitle();
                if (name == null) {
                    name = IdUtils.generateId(type);
                } else {
                    name = IdUtils.generateId(name);
                }
                String baseTitle = name;
                int i = 0;
                while (true) {
                    try {
                        if (i == 10) {
                            throw new WebException("Failed to create document. Giving up.");
                        }
                        session.getDocument(new PathRef(path, name));
                        name = baseTitle+"_"+Long.toHexString(IdUtils.generateLongId());
                        i++;
                    } catch (Exception e) {
                        // the name should be ok
                        break;
                    }
                }
            }
            DocumentModel newPage = session.createDocumentModel(parent.getPathAsString(), name, type);
            form.fillDocument(newPage);
            newPage = session.createDocument(newPage);
            session.save();
            return newPage;
        } catch (Exception e) {
            throw new WebException("Failed to create document: "+name, e);
        }
    }

}

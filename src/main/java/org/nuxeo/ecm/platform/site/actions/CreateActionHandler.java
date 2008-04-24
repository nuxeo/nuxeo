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

package org.nuxeo.ecm.platform.site.actions;


import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.site.SiteException;
import org.nuxeo.ecm.platform.site.SiteObject;
import org.nuxeo.ecm.platform.site.SiteRequest;
import org.nuxeo.ecm.platform.site.util.DocumentFormHelper;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CreateActionHandler implements ActionHandler {

    public void run(SiteObject object) throws SiteException {
        if (object.isResolved()) {
            DocumentModel parent = object.getDocument();
            object = object.next();
            String name = object.getName();
            if (object != null && !object.isResolved()) {
                createSubPage(parent, name, object.getSiteRequest());
                return;
            }
        }
        throw new SiteException("Faield to create document. The document already exists: "+object.getPath());
    }

    private DocumentModel createSubPage(DocumentModel doc, String name, SiteRequest request)
    throws SiteException {
        try {
            CoreSession session = request.getCoreSession();
            String type = DocumentFormHelper.getDocumentType(request);
            if (type == null) {
                throw new SiteException("Invalid argument exception. Nos doc type specified");
            }
            DocumentModel newPage = session.createDocumentModel(doc.getPathAsString(), name, type);
            DocumentFormHelper.fillDocumentProperties(doc, request);
            newPage = session.createDocument(newPage);
            session.save();
            return newPage;
        } catch (Exception e) {
            throw new SiteException("Failed to create document: "+name, e);
        }
    }

    private DocumentModel old_createSubPage(DocumentModel doc, String name, SiteRequest request)
    throws SiteException {
        try {
            CoreSession session = request.getCoreSession();
            DocumentModel newPage = session.createDocumentModel(doc.getPathAsString(), name,
            "Note");
            newPage.setProperty("dublincore", "title", name);
//          newPage.setProperty("note", "note", "This is new page ${title}");
            newPage.setProperty("note", "note", "");
            newPage = session.createDocument(newPage);
            session.save();
            return newPage;
        } catch (Exception e) {
            throw new SiteException("Failed to create document: "+name, e);
        }
    }

}

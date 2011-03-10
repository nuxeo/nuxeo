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

package org.nuxeo.ecm.core.rest;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.Validator;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocumentHelper {

    // Utility class.
    private DocumentHelper() {
    }

    public static DocumentModel createDocument(WebContext context, DocumentModel parent, String name) {
        try {
            PathSegmentService pss;
            try {
                pss = Framework.getService(PathSegmentService.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
            CoreSession session = context.getCoreSession();
            FormData form = context.getForm();
            String type = form.getDocumentType();
            if (type == null) {
                throw new WebException("Invalid argument exception. Nos doc type specified");
            }
            DocumentModel newDoc = session.createDocumentModel(type);
            form.fillDocument(newDoc);
            if (name != null) {
                newDoc.setPropertyValue("dc:title", name);
            }
            Module module = context.getModule();
            Validator v = module.getValidator(newDoc.getType());
            if (v != null) {
                newDoc = v.validate(newDoc);
            }
            newDoc.setPathInfo(parent.getPathAsString(),
                    pss.generatePathSegment(newDoc));
            newDoc = session.createDocument(newDoc);
            newDoc.setPropertyValue("dc:title", newDoc.getName());
            session.saveDocument(newDoc);
            session.save();
            return newDoc;
        } catch (Exception e) {
            throw WebException.wrap("Failed to create document: " + name, e);
        }
    }

    public static DocumentModel updateDocument(WebContext ctx, DocumentModel doc) {
        try {
            FormData form = ctx.getForm();
            form.fillDocument(doc);
            doc.putContextData(VersioningService.VERSIONING_OPTION,
                    form.getVersioningOption());
            Module module = ctx.getModule();
            Validator v = module.getValidator(doc.getType());
            if (v != null) {
                doc = v.validate(doc);
            }

            doc = ctx.getCoreSession().saveDocument(doc);
            ctx.getCoreSession().save();
            return doc;
        } catch (Exception e) {
            throw WebException.wrap("Failed to update document", e);
        }
    }

}

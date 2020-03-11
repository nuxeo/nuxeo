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

package org.nuxeo.ecm.core.rest;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.webengine.forms.FormData;
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
        FormData form = context.getForm();
        String type = form.getDocumentType();
        if (type == null) {
            throw new NuxeoException("Invalid argument exception. No doc type specified");
        }

        try {
            PathSegmentService pss = Framework.getService(PathSegmentService.class);
            CoreSession session = context.getCoreSession();
            DocumentModel newDoc = session.createDocumentModel(type);
            form.fillDocument(newDoc);
            if (name != null) {
                newDoc.setPropertyValue("dc:title", name);
            }
            newDoc.setPathInfo(parent.getPathAsString(), pss.generatePathSegment(newDoc));
            newDoc = session.createDocument(newDoc);
            newDoc.setPropertyValue("dc:title", newDoc.getName());
            session.saveDocument(newDoc);
            session.save();
            return newDoc;
        } catch (NuxeoException e) {
            e.addInfo("Failed to create document: " + name);
            throw e;
        }
    }

    public static DocumentModel updateDocument(WebContext ctx, DocumentModel doc) {
        FormData form = ctx.getForm();
        form.fillDocument(doc);
        doc.putContextData(VersioningService.VERSIONING_OPTION, form.getVersioningOption());
        doc = ctx.getCoreSession().saveDocument(doc);
        ctx.getCoreSession().save();
        return doc;
    }

}

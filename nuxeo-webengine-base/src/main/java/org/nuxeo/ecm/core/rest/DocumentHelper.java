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

import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.Validator;
import org.nuxeo.ecm.webengine.model.WebContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocumentHelper {

    // Utility class.
    private DocumentHelper() {
    }

    public static DocumentModel createDocument(WebContext context, DocumentModel parent, String name) {
        try {
            CoreSession session = context.getCoreSession();
            FormData form = context.getForm();
            String type = form.getDocumentType();
            if (type == null) {
                throw new WebException("Invalid argument exception. Nos doc type specified");
            }
            String path = parent.getPathAsString();
            // TODO not the best method to create an unnamed doc - should refactor core API
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
                        name = baseTitle + "_" + Long.toHexString(IdUtils.generateLongId());
                        i++;
                    } catch (Exception e) {
                        // the name should be ok
                        break;
                    }
                }
            }
            DocumentModel newDoc = session.createDocumentModel(parent.getPathAsString(), name, type);
            form.fillDocument(newDoc);
            if (newDoc.getTitle().length() == 0) {
                newDoc.getPart("dublincore").get("title").setValue(newDoc.getName());
            }
            Module m = context.getModule();
            Validator v = m.getValidator(newDoc.getType());
            if (v != null) {
                newDoc = v.validate(newDoc);
            }
            newDoc = session.createDocument(newDoc);
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
            VersioningActions va = form.getVersioningOption();
            if (va != null) {
                ScopedMap ctxData = doc.getContextData();
                ctxData.putScopedValue(ScopeType.REQUEST,
                        VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, true);
                ctxData.putScopedValue(ScopeType.REQUEST, VersioningActions.KEY_FOR_INC_OPTION, va);
            } else {
                ScopedMap ctxData = doc.getContextData();
                ctxData.putScopedValue(ScopeType.REQUEST,
                        VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, false);
            }
            Module m = ctx.getModule();
            Validator v = m.getValidator(doc.getType());
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

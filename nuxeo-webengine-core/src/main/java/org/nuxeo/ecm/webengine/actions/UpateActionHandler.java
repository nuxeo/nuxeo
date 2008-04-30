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
import java.io.Writer;

import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;
import org.nuxeo.ecm.webengine.SiteException;
import org.nuxeo.ecm.webengine.SiteObject;
import org.nuxeo.ecm.webengine.SiteRequest;
import org.nuxeo.ecm.webengine.servlet.SiteConst;
import org.nuxeo.ecm.webengine.util.DocumentFormHelper;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class UpateActionHandler implements ActionHandler {

    public void run(SiteObject object) throws SiteException {
        SiteRequest request = object.getSiteRequest();
        DocumentModel doc = object.getDocument();
        try {
            DocumentFormHelper.fillDocumentProperties(doc, request);
            VersioningActions va = DocumentFormHelper.getVersioningOption(request);
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
            doc = request.getCoreSession().saveDocument(doc);
            request.getCoreSession().save();
        } catch (Exception e) {
            throw new SiteException("Failed to update document", e);
        }
    }

    public void old_run(SiteObject object) throws SiteException {
        System.out.println("RUNNING UPDATE ACTION FOR: "+object.getPath());
        SiteRequest request = object.getSiteRequest();
        DocumentModel sourceDocument = object.getDocument();
        String newContent = request.getParameter("note");
        if (newContent != null) {
            String oldContent = (String) sourceDocument.getProperty("note", "note");
            if (!newContent.equals(oldContent)) {
                sourceDocument.setProperty("note", "note", newContent);
                try {
                    // flag doc for snapshoting
                    ScopedMap ctxData = sourceDocument.getContextData();
                    ctxData.putScopedValue(ScopeType.REQUEST,
                            VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, false);
                    ctxData.putScopedValue(ScopeType.REQUEST, VersioningActions.KEY_FOR_INC_OPTION,
                            VersioningActions.ACTION_INCREMENT_MINOR);
                    sourceDocument = request.getCoreSession().saveDocument(sourceDocument);
                    request.getCoreSession().save();
                } catch (SiteException e) {
                    throw e;
                } catch (Exception e) {
                    throw new SiteException("Error during update process", e);
                }
            }
        } else {
            try {
                Writer writer = request.getResponse().getWriter();
                writer.write("Unable to update");
                request.cancelRendering();
                request.getResponse().setStatus(SiteConst.SC_METHOD_FAILURE);
            } catch (IOException e) {
                throw new SiteException("Error during update process", e);
            }
        }
    }
}

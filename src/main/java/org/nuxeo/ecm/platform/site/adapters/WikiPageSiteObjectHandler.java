/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.site.adapters;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletResponse;

import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.platform.site.api.SiteException;
import org.nuxeo.ecm.platform.site.servlet.SiteConst;
import org.nuxeo.ecm.platform.site.servlet.SiteRequest;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;

public class WikiPageSiteObjectHandler extends NoteSiteObjectHandler {


    protected static final String wikiSchema = "note";
    protected static final String wikiContent = "note";
    protected static final String wikiContentUpdateField = "note";

    public WikiPageSiteObjectHandler() {
    }


    @Override
    public void doPost(SiteRequest request, HttpServletResponse response) throws SiteException {
        String newContent = request.getParameter(wikiContentUpdateField);
        if (newContent != null) {
            String oldContent = (String) sourceDocument.getProperty(wikiSchema, wikiContent);
            if (!newContent.equals(oldContent)) {
                sourceDocument.setProperty(wikiSchema, wikiContent, newContent);
                try {
                    // flag doc for snapshoting
                    ScopedMap ctxData = sourceDocument.getContextData();
                    ctxData.putScopedValue(ScopeType.REQUEST,
                            VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, true);
                    ctxData.putScopedValue(ScopeType.REQUEST, VersioningActions.KEY_FOR_INC_OPTION,
                            VersioningActions.ACTION_INCREMENT_MINOR);
                    sourceDocument = request.getCoreSession().saveDocument(sourceDocument);
                    request.getCoreSession().save();
                } catch (ClientException e) {
                    throw new SiteException("Error during update process", e);
                }
            }
            doGet(request, response);
        } else {
            try {
                Writer writer = response.getWriter();
                writer.write("Unable to update");
                request.cancelRendering();
                response.setStatus(SiteConst.SC_METHOD_FAILURE);
            } catch (IOException e) {
                throw new SiteException("Error during update process", e);
            }
        }
    }


}

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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.site.api.SiteException;
import org.nuxeo.ecm.platform.site.servlet.SiteConst;
import org.nuxeo.ecm.platform.site.servlet.SiteRequest;

public class NoteSiteObjectHandler extends DynamicTemplateSiteObjectHandler {

    public NoteSiteObjectHandler() {
    }

    public NoteSiteObjectHandler(DocumentModel doc) {
        super(doc);
    }

    @Override
    public String getTemplateName(SiteRequest request) {
        return getDynamicTemplateKey(request, "note", "note");
    }

    @Override
    public void doPost(SiteRequest request, HttpServletResponse response) throws SiteException {
        String newContent = request.getParameter("note");
        if (newContent != null) {
            sourceDocument.setProperty("note", "note", newContent);
            try {
                sourceDocument = request.getDocumentManager().saveDocument(sourceDocument);
                request.getDocumentManager().save();
            } catch (ClientException e) {
                throw new SiteException("Error during update process", e);
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

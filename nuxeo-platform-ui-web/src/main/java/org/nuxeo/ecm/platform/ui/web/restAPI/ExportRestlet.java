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
 *     Thierry Delprat
 *     Florent Guillaume
 *
 * $Id: ExportRestlet.java 30251 2008-02-18 19:17:33Z fguillaume $
 */

package org.nuxeo.ecm.platform.ui.web.restAPI;

import static org.jboss.seam.ScopeType.STATELESS;

import java.util.Map;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;

import com.noelios.restlet.http.HttpConstants;

@Name("exportRestlet")
@Scope(STATELESS)
public class ExportRestlet extends BaseNuxeoRestlet {

    @In(create = true)
    protected NavigationContext navigationContext;

    @Override
    public void handle(Request req, Response res) {
        boolean exportAsTree;
        boolean exportAsZip;
        CoreSession documentManager;
        DocumentModel root;

        String action = req.getResourceRef().getSegments().get(4);
        if (action.equals("exportTree")) {
            exportAsTree = true;
            exportAsZip = true;
        } else if (action.equals("exportSingle")) {
            exportAsTree = false;
            exportAsZip = false;
        } else if (action.equals("export")) {
            exportAsTree = false;
            exportAsZip = false;
        } else {
            exportAsTree = false;
            exportAsZip = false;
        }

        String format = req.getResourceRef().getQueryAsForm().getFirstValue(
                "format");
        if ("xml".equalsIgnoreCase(format)) {
            exportAsZip = false;
        } else if ("zip".equalsIgnoreCase(format)) {
            exportAsZip = true;
        }

        String repo = (String) req.getAttributes().get("repo");
        String docid = (String) req.getAttributes().get("docid");

        if (repo == null || repo.equals("*")) {
            handleError(res, "you must specify a repository");
            return;
        }

        try {
            navigationContext.setCurrentServerLocation(new RepositoryLocation(
                    repo));
            documentManager = navigationContext.getOrCreateDocumentManager();
            if (docid == null || docid.equals("*")) {
                root = documentManager.getRootDocument();
            } else {
                root = documentManager.getDocument(new IdRef(docid));
            }
        } catch (ClientException e) {
            handleError(res, e);
            return;
        }

        if (exportAsZip) {
            // set the content disposition and file name
            String FILENAME = "export.zip";

            // use the Facelets APIs to set a new header
            Map<String, Object> attributes = res.getAttributes();
            Form headers = (Form) attributes.get(HttpConstants.ATTRIBUTE_HEADERS);
            if (headers == null) {
                headers = new Form();
            }
            headers.add("Content-Disposition", String.format(
                    "attachment; filename=\"%s\";", FILENAME));
            attributes.put(HttpConstants.ATTRIBUTE_HEADERS, headers);
        }

        res.setEntity(new ExportRepresentation(exportAsTree, exportAsZip,
                documentManager, root));
    }

}

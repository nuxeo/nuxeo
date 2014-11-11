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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.restAPI;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.util.Collection;

import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.runtime.api.Framework;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

@Name("browseRestlet")
@Scope(EVENT)
public class BrowseRestlet extends BaseNuxeoRestlet implements Serializable {

    private static final long serialVersionUID = -4518256101431979971L;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    protected CoreSession documentManager;

    @Override
    public void handle(Request req, Response res) {
        String repo = (String) req.getAttributes().get("repo");
        String docid = (String) req.getAttributes().get("docid");
        DOMDocumentFactory domfactory = new DOMDocumentFactory();

        DOMDocument result = (DOMDocument) domfactory.createDocument();
        // Element root = result.createElement("browse");
        // result.setRootElement((org.dom4j.Element) root);

        if (repo == null || repo.equals("*")) {
            try {
                RepositoryManager repmanager = Framework.getService(RepositoryManager.class);
                Collection<Repository> repos = repmanager.getRepositories();

                Element serversNode = result.createElement("avalaibleServers");
                result.setRootElement((org.dom4j.Element) serversNode);

                for (Repository availableRepo : repos) {
                    Element server = result.createElement("server");
                    server.setAttribute("title", availableRepo.getName());
                    server.setAttribute("url", getRelURL(
                            availableRepo.getName(), "*"));
                    serversNode.appendChild(server);
                }
            } catch (Exception e) {
                handleError(result, res, e);
                return;
            }
        } else {
            DocumentModel dm;
            try {
                navigationContext.setCurrentServerLocation(new RepositoryLocation(
                        repo));
                documentManager = navigationContext.getOrCreateDocumentManager();
                if (docid == null || docid.equals("*")) {
                    dm = documentManager.getRootDocument();
                } else {
                    dm = documentManager.getDocument(new IdRef(docid));
                }
            } catch (ClientException e) {
                handleError(result, res, e);
                return;
            }

            Element current = result.createElement("document");
            try {
                current.setAttribute("title", dm.getTitle());
            } catch (DOMException e1) {
                handleError(res, e1);
            } catch (ClientException e1) {
                handleError(res, e1);
            }
            current.setAttribute("type", dm.getType());
            current.setAttribute("id", dm.getId());
            current.setAttribute("url", getRelURL(repo, dm.getRef().toString()));
            result.setRootElement((org.dom4j.Element) current);

            if (dm.isFolder()) {
                // Element childrenElem = result.createElement("children");
                // root.appendChild(childrenElem);

                DocumentModelList children;
                try {
                    children = documentManager.getChildren(dm.getRef());
                } catch (ClientException e) {
                    handleError(result, res, e);
                    return;
                }

                for (DocumentModel child : children) {
                    Element el = result.createElement("document");
                    try {
                        el.setAttribute("title", child.getTitle());
                    } catch (DOMException e) {
                        handleError(res, e);
                    } catch (ClientException e) {
                        handleError(res, e);
                    }
                    el.setAttribute("type", child.getType());
                    el.setAttribute("id", child.getId());
                    el.setAttribute("url", getRelURL(repo,
                            child.getRef().toString()));
                    current.appendChild(el);
                }
            }
        }
        res.setEntity(result.asXML(), MediaType.TEXT_XML);
    }

    private static String getRelURL(String repo, String uuid) {
        return '/' + repo + '/' + uuid;
    }

}

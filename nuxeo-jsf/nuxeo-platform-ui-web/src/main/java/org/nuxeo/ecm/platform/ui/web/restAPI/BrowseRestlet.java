/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.restAPI;

import java.io.Serializable;

import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

public class BrowseRestlet extends BaseStatelessNuxeoRestlet implements Serializable {

    private static final long serialVersionUID = -4518256101431979971L;

    @Override
    protected void doHandleStatelessRequest(Request req, Response res) {
        String repo = (String) req.getAttributes().get("repo");
        String docid = (String) req.getAttributes().get("docid");

        DOMDocumentFactory domFactory = new DOMDocumentFactory();

        DOMDocument result = (DOMDocument) domFactory.createDocument();

        if (repo == null || repo.equals("*")) {
            try {
                Element serversNode = result.createElement("avalaibleServers");
                result.setRootElement((org.dom4j.Element) serversNode);

                RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
                for (String repositoryName : repositoryManager.getRepositoryNames()) {
                    Element server = result.createElement("server");
                    server.setAttribute("title", repositoryName);
                    server.setAttribute("url", getRelURL(repositoryName, "*"));
                    serversNode.appendChild(server);
                }
                res.setEntity(result.asXML(), MediaType.APPLICATION_XML);
                res.getEntity().setCharacterSet(CharacterSet.UTF_8);
                return;
            } catch (DOMException e) {
                handleError(result, res, e);
                return;
            }
        } else {
            DocumentModel dm;

            boolean init = initRepository(res, repo);
            boolean isRoot = false;
            try {
                if (init) {
                    if (docid == null || docid.equals("*")) {
                        dm = session.getRootDocument();
                        isRoot = true;
                    } else {
                        dm = session.getDocument(new IdRef(docid));
                    }
                } else {
                    handleError(res, "Unable to init repository");
                    return;
                }
            } catch (NuxeoException e) {
                handleError(res, e);
                return;
            }

            Element current = result.createElement("document");
            try {
                current.setAttribute("title", dm.getTitle());
            } catch (DOMException | NuxeoException e) {
                handleError(res, e);
            }
            current.setAttribute("type", dm.getType());
            current.setAttribute("id", dm.getId());
            current.setAttribute("name", dm.getName());
            if (isRoot) {
                current.setAttribute("url", getRelURL(repo, ""));
            } else {
                current.setAttribute("url", getRelURL(repo, dm.getRef().toString()));
            }
            result.setRootElement((org.dom4j.Element) current);

            if (dm.isFolder()) {
                // Element childrenElem = result.createElement("children");
                // root.appendChild(childrenElem);

                DocumentModelList children;
                try {
                    children = session.getChildren(dm.getRef());
                } catch (NuxeoException e) {
                    handleError(result, res, e);
                    return;
                }

                for (DocumentModel child : children) {
                    Element el = result.createElement("document");
                    try {
                        el.setAttribute("title", child.getTitle());
                    } catch (DOMException e) {
                        handleError(res, e);
                    } catch (NuxeoException e) {
                        handleError(res, e);
                    }
                    el.setAttribute("type", child.getType());
                    el.setAttribute("id", child.getId());
                    el.setAttribute("name", child.getName());
                    el.setAttribute("url", getRelURL(repo, child.getRef().toString()));
                    current.appendChild(el);
                }
            }

            res.setEntity(result.asXML(), MediaType.APPLICATION_XML);
            res.getEntity().setCharacterSet(CharacterSet.UTF_8);
        }
    }

    private static String getRelURL(String repo, String uuid) {
        return '/' + repo + '/' + uuid;
    }

}

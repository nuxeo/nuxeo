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

package org.nuxeo.ecm.platform.ui.web.restAPI;

import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * Base class for stateless restlet, i.e. Restlets that don't use Seam.
 *
 * @author tiry
 */
public class BaseStatelessNuxeoRestlet extends BaseNuxeoRestlet {

    protected CoreSession session;
    protected DocumentRef targetDocRef;
    protected DocumentModel targetDocument;

    protected boolean initRepository(Response res, String repoId) {
        DOMDocumentFactory domFactory = new DOMDocumentFactory();
        DOMDocument result = (DOMDocument) domFactory.createDocument();

        if (repoId == null || repoId.equals("*")) {
            handleError(result, res, "you must specify a repository");
            return false;
        }

        RepositoryManager rm;
        try {
            rm = Framework.getService(RepositoryManager.class);
        } catch (Exception e1) {
            handleError(result, res, e1);
            return false;
        }
        Repository repo = rm.getRepository(repoId);

        if (repo == null) {
            handleError(res, "Unable to get " + repoId + " repository");
            return false;
        }

        try {
            session = repo.open();
        } catch (Exception e1) {
            handleError(result, res, e1);
            return false;
        }
        if (session == null) {
            handleError(result, res,
                    "Unable to open " + repoId + " repository");
            return false;
        }
        return true;
    }

    protected boolean initRepositoryAndTargetDocument(Response res,
            String repoId, String docId) {

        DOMDocumentFactory domFactory = new DOMDocumentFactory();
        DOMDocument result = (DOMDocument) domFactory.createDocument();

        if (repoId == null || repoId.equals("*")) {
            handleError(result, res, "you must specify a repository");
            return false;
        }

        if (docId == null || docId.equals("")) {
            handleError(result, res, "you must specify a document");
            return false;
        }

        RepositoryManager rm;
        try {
            rm = Framework.getService(RepositoryManager.class);
        } catch (Exception e1) {
            handleError(result, res, e1);
            return false;
        }
        Repository repo = rm.getRepository(repoId);

        if (repo == null) {
            handleError(res, "Unable to get " + repoId + " repository");
            return false;
        }

        try {
            session = repo.open();
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            handleError(result, res, e1);
            return false;
        }
        if (session == null) {
            handleError(result, res,
                    "Unable to open " + repoId + " repository");
            return false;
        }

        targetDocRef = new IdRef(docId);

        try {
            targetDocument = session.getDocument(targetDocRef);
        } catch (ClientException e) {
            handleError(result, res,
                    "Unable to open " + repoId + " repository");
            return false;
        }

        return true;
    }

    protected void cleanUp() {
        if (session != null) {
            CoreInstance.getInstance().close(session);
            session = null;
            targetDocRef = null;
            targetDocument = null;
        }
    }

    @Override
    public void handle(Request request, Response response) {
        try {
            doHandleStatelessRequest(request, response);
        }
        finally {
            cleanUp();
        }
    }

    protected void doHandleStatelessRequest(Request req, Response res) {
    }

}

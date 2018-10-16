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
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.restAPI;

import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.restlet.Request;
import org.restlet.Response;

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
        try {
            session = CoreInstance.openCoreSession(repoId);
        } catch (NuxeoException e) {
            handleError(result, res, e);
            return false;
        }
        return true;
    }

    protected boolean initRepositoryAndTargetDocument(Response res, String repoId, String docId) {

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

        try {
            session = CoreInstance.openCoreSession(repoId);
        } catch (NuxeoException e) {
            handleError(result, res, e);
            return false;
        }

        targetDocRef = new IdRef(docId);

        try {
            targetDocument = session.getDocument(targetDocRef);
        } catch (NuxeoException e) {
            handleError(result, res, "Unable to open " + repoId + " repository");
            return false;
        }

        return true;
    }

    protected void cleanUp() {
        if (session != null) {
            ((CloseableCoreSession) session).close();
            session = null;
            targetDocRef = null;
            targetDocument = null;
        }
    }

    @Override
    public void handle(Request request, Response response) {
        try {
            doHandleStatelessRequest(request, response);
        } finally {
            cleanUp();
        }
    }

    protected void doHandleStatelessRequest(Request req, Response res) {
    }

}

/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.ui.web.tag.fn.LiveEditConstants;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * This RESTLET allows to delete documents
 *
 * @author jthimonier
 */
public class DeleteDocumentRestlet extends BaseStatelessNuxeoRestlet implements
        LiveEditConstants {

    private static final Log log = LogFactory.getLog(DeleteDocumentRestlet.class);

    @Override
    protected void doHandleStatelessRequest(Request req, Response res) {

        String repoId = (String) req.getAttributes().get("repo");
        String docId = (String) req.getAttributes().get("docid");

        DOMDocumentFactory domFactory = new DOMDocumentFactory();
        DOMDocument result = (DOMDocument) domFactory.createDocument();

        if (docId != null) {
            // init repo and document
            boolean initOk = initRepositoryAndTargetDocument(res, repoId, docId);
            if (!initOk) {
                return;
            }
        } else {
            // init repo
            boolean initOk = initRepository(res, repoId);
            if (!initOk) {
                return;
            }

            // init document
            String path = getQueryParamValue(req, "path", null);
            if (path == null) {
                return;
            }
            targetDocRef = new PathRef(path);
            try {
                targetDocument = session.getDocument(targetDocRef);
            } catch (ClientException e) {
                handleError(result, res, "Unable to get document " + path);
                return;
            }
            docId = targetDocument.getId();
        }

        try {
            if (!session.canRemoveDocument(targetDocRef)) {
                handleError(res, "This document can't be removed");
                return;
            }

            session.removeDocument(targetDocRef);
            session.save();

            // build the XML response document holding the ref
            Element docElement = result.addElement(documentTag);
            docElement.addElement(docRefTag).setText(
                    "Document " + docId + " deleted");
            res.setEntity(result.asXML(), MediaType.TEXT_XML);
            res.getEntity().setCharacterSet(CharacterSet.UTF_8);
        } catch (ClientException e) {
            log.error(e.getMessage(), e);
            handleError(res, e);
        }
    }

}

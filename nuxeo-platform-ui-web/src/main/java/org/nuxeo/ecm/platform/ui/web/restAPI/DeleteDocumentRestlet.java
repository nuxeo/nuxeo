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
import org.nuxeo.ecm.platform.ui.web.tag.fn.LiveEditConstants;
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

    private Log log = LogFactory.getLog(DeleteDocumentRestlet.class);

    @Override
    public void handle(Request req, Response res) {

        String repoId = (String) req.getAttributes().get("repo");
        String docId = (String) req.getAttributes().get("docid");

        DOMDocumentFactory domfactory = new DOMDocumentFactory();
        DOMDocument result = (DOMDocument) domfactory.createDocument();

        // init repo and document
        Boolean initOk = super.initRepositoryAndTargetDocument(res, repoId,
                docId);
        if (!initOk) {
            return;
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
        } catch (ClientException e) {
            log.error(e.getMessage(), e);
            handleError(res, e);
        }
    }
}

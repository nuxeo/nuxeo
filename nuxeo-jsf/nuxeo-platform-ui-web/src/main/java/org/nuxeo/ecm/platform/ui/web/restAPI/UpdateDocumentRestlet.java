/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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

/**
 * This RESTLET allows to update document properties
 * @author jthimonier
 */
package org.nuxeo.ecm.platform.ui.web.restAPI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.ui.web.tag.fn.LiveEditConstants;
import org.restlet.data.CharacterSet;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class UpdateDocumentRestlet extends BaseStatelessNuxeoRestlet implements LiveEditConstants {

    private static final Log log = LogFactory.getLog(UpdateDocumentRestlet.class);

    @Override
    protected void doHandleStatelessRequest(Request req, Response res) {
        String repoId = (String) req.getAttributes().get("repo");
        String docId = (String) req.getAttributes().get("docid");

        DOMDocumentFactory domFactory = new DOMDocumentFactory();
        DOMDocument result = (DOMDocument) domFactory.createDocument();

        // init repo and document
        boolean initOk = initRepositoryAndTargetDocument(res, repoId, docId);
        if (!initOk) {
            return;
        }

        try {
            Form queryParameters = req.getResourceRef().getQueryAsForm();
            for (String paramName : queryParameters.getNames()) {
                if (!DOC_TYPE.equals(paramName)) {
                    // treat all non doctype parameters as string fields
                    targetDocument.setPropertyValue(paramName, getQueryParamValue(req, paramName, null));
                }
            }
            session.saveDocument(targetDocument);
            session.save();

            // build the XML response document holding the ref
            Element docElement = result.addElement(documentTag);
            docElement.addElement(docRefTag).setText("Document " + docId + " has been updated");
            res.setEntity(result.asXML(), MediaType.APPLICATION_XML);
            res.getEntity().setCharacterSet(CharacterSet.UTF_8);
        } catch (NuxeoException e) {
            log.error(e.getMessage(), e);
            handleError(res, e);
        }
    }

}

/*
 * (C) Copyright 2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: CreateDocumentRestlet.java 30586 2008-02-26 14:30:17Z ogrisel $
 */

package org.nuxeo.ecm.platform.ui.web.restAPI;

import java.io.Serializable;

import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.ui.web.tag.fn.LiveEditConstants;
import org.nuxeo.runtime.api.Framework;
import org.restlet.data.CharacterSet;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;

/**
 * Allow the creation of a new document of the specified document type
 *
 * @author Olivier Grisel <ogrisel@nuxeo.com>
 */
public class CreateDocumentRestlet extends BaseNuxeoRestlet implements LiveEditConstants, Serializable {

    private static final long serialVersionUID = -7223939557577366747L;

    @Override
    public void handle(Request req, Response res) {
        String repo = (String) req.getAttributes().get("repo");
        if (repo == null || repo.equals("*")) {
            handleError(res, "you must specify a repository");
            return;
        }

        DocumentModel parentDm;
        try (CloseableCoreSession documentManager = CoreInstance.openCoreSession(repo)) {
            String parentDocRef = (String) req.getAttributes().get("parentdocid");
            if (parentDocRef != null) {
                parentDm = documentManager.getDocument(new IdRef(parentDocRef));
            } else {
                handleError(res, "you must specify a valid document IdRef for the parent document");
                return;
            }

        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        String docTypeName = getQueryParamValue(req, DOC_TYPE, DEFAULT_DOCTYPE);
        String titleField = "dublincore:title";
        String title = getQueryParamValue(req, titleField, "New " + docTypeName);

            DocumentModel newDm = documentManager.createDocumentModel(docTypeName);
            Form queryParameters = req.getResourceRef().getQueryAsForm();
            for (String paramName : queryParameters.getNames()) {
                if (!DOC_TYPE.equals(paramName)) {
                    // treat all non doctype parameters as string fields
                    newDm.setPropertyValue(paramName, getQueryParamValue(req, paramName, null));
                    // TODO: handle multi-valued parameters as StringList fields
                }
                // override the title for consistency
                newDm.setPropertyValue(titleField, title);
            }
            // create the document in the repository
            newDm.setPathInfo(parentDm.getPathAsString(), pss.generatePathSegment(newDm));
            newDm = documentManager.createDocument(newDm);
            documentManager.save();

            // build the XML response document holding the ref
            DOMDocumentFactory domFactory = new DOMDocumentFactory();
            DOMDocument resultDocument = (DOMDocument) domFactory.createDocument();
            Element docElement = resultDocument.addElement(documentTag);
            docElement.addElement(docRepositoryTag).setText(newDm.getRepositoryName());
            docElement.addElement(docRefTag).setText(newDm.getRef().toString());
            docElement.addElement(docTitleTag).setText(newDm.getTitle());
            docElement.addElement(docPathTag).setText(newDm.getPathAsString());
            Representation rep = new StringRepresentation(resultDocument.asXML(), MediaType.APPLICATION_XML);
            rep.setCharacterSet(CharacterSet.UTF_8);
            res.setEntity(rep);
        } catch (NuxeoException e) {
            handleError(res, e);
        }
    }

}

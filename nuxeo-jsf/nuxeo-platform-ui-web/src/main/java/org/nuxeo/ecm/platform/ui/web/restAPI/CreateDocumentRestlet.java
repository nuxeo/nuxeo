/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: CreateDocumentRestlet.java 30586 2008-02-26 14:30:17Z ogrisel $
 */

package org.nuxeo.ecm.platform.ui.web.restAPI;

import java.io.Serializable;

import static org.jboss.seam.ScopeType.EVENT;

import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.tag.fn.LiveEditConstants;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
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
@Name("createDocumentRestlet")
@Scope(EVENT)
public class CreateDocumentRestlet extends BaseNuxeoRestlet implements
        LiveEditConstants, Serializable {

    private static final long serialVersionUID = -7223939557577366747L;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    protected CoreSession documentManager;

    @Override
    public void handle(Request req, Response res) {
        String repo = (String) req.getAttributes().get("repo");
        if (repo == null || repo.equals("*")) {
            handleError(res, "you must specify a repository");
            return;
        }

        DocumentModel parentDm;
        PathSegmentService pss;
        try {
            navigationContext.setCurrentServerLocation(new RepositoryLocation(
                    repo));
            documentManager = navigationContext.getOrCreateDocumentManager();
            String parentDocRef = (String) req.getAttributes().get(
                    "parentdocid");
            if (parentDocRef != null) {
                parentDm = documentManager.getDocument(new IdRef(parentDocRef));
            } else {
                handleError(res,
                        "you must specify a valid document IdRef for the parent document");
                return;
            }
            pss = Framework.getService(PathSegmentService.class);
        } catch (Exception e) {
            handleError(res, e);
            return;
        }

        String docTypeName = getQueryParamValue(req, DOC_TYPE, DEFAULT_DOCTYPE);
        String titleField = "dublincore:title";
        String title = getQueryParamValue(req, titleField, "New " + docTypeName);

        try {
            DocumentModel newDm = documentManager.createDocumentModel(docTypeName);
            Form queryParameters = req.getResourceRef().getQueryAsForm();
            for (String paramName : queryParameters.getNames()) {
                if (!DOC_TYPE.equals(paramName)) {
                    // treat all non doctype parameters as string fields
                    newDm.setPropertyValue(paramName, getQueryParamValue(req,
                            paramName, null));
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
            docElement.addElement(docRepositoryTag).setText(
                    newDm.getRepositoryName());
            docElement.addElement(docRefTag).setText(newDm.getRef().toString());
            docElement.addElement(docTitleTag).setText(newDm.getTitle());
            docElement.addElement(docPathTag).setText(newDm.getPathAsString());
            Representation rep = new StringRepresentation(resultDocument.asXML(),
                    MediaType.APPLICATION_XML);
            rep.setCharacterSet(CharacterSet.UTF_8);
            res.setEntity(rep);
        } catch (ClientException e) {
            handleError(res, e);
        }
    }

}

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
 * $Id: CreationContainerListRestlet.java 30586 2008-02-26 14:30:17Z ogrisel $
 */

package org.nuxeo.ecm.platform.ui.web.restAPI;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.ui.web.tag.fn.LiveEditConstants;
import org.nuxeo.runtime.api.Framework;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * This restlet gets the list of containers that are suitable for new document creation.
 * <p>
 * The actual logic is delegated to the FileManagerService.
 *
 * @author Olivier Grisel <ogrisel@nuxeo.com>
 */
@Name("creationContainerListRestlet")
@Scope(EVENT)
public class CreationContainerListRestlet extends BaseNuxeoRestlet implements
        LiveEditConstants, Serializable {

    private static final Log log = LogFactory.getLog(CreationContainerListRestlet.class);
    private static final long serialVersionUID = 5403775170948512675L;

    @Override
    public void handle(Request req, Response res) {

        DocumentModelList containers = null;
        String docType = getQueryParamValue(req, DOC_TYPE, DEFAULT_DOCTYPE);
        try {
            FileManager fileManager = Framework.getService(FileManager.class);
            containers = fileManager.getCreationContainers(
                    getUserPrincipal(req), docType);
        } catch (Exception e) {
            handleError(res, e);
        }

        // build the XML response document holding the containers info
        DOMDocumentFactory domFactory = new DOMDocumentFactory();
        DOMDocument resultDocument = (DOMDocument) domFactory.createDocument();
        Element containersElement = resultDocument.addElement("containers");
        for (DocumentModel parent : containers) {
            Element docElement = containersElement.addElement(documentTag);
            docElement.addElement(docRepositoryTag).setText(
                    parent.getRepositoryName());
            docElement.addElement(docRefTag).setText(parent.getRef().toString());
            try {
                docElement.addElement(docTitleTag).setText(parent.getTitle());
            } catch (ClientException e) {
                log.error("not setting doc title tag: " + e);
            }
            docElement.addElement(docPathTag).setText(parent.getPathAsString());
        }
        res.setEntity(resultDocument.asXML(), MediaType.TEXT_XML);
        res.getEntity().setCharacterSet(CharacterSet.UTF_8);
    }

}

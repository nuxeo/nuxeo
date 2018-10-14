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
 * $Id: CreationContainerListRestlet.java 30586 2008-02-26 14:30:17Z ogrisel $
 */

package org.nuxeo.ecm.platform.ui.web.restAPI;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
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
public class CreationContainerListRestlet extends BaseNuxeoRestlet implements LiveEditConstants, Serializable {

    private static final Log log = LogFactory.getLog(CreationContainerListRestlet.class);

    private static final long serialVersionUID = 5403775170948512675L;

    @Override
    public void handle(Request req, Response res) {

        DocumentModelList containers = null;
        String docType = getQueryParamValue(req, DOC_TYPE, DEFAULT_DOCTYPE);
        FileManager fileManager = Framework.getService(FileManager.class);
        containers = fileManager.getCreationContainers(ClientLoginModule.getCurrentPrincipal(), docType);

        // build the XML response document holding the containers info
        DOMDocumentFactory domFactory = new DOMDocumentFactory();
        DOMDocument resultDocument = (DOMDocument) domFactory.createDocument();
        Element containersElement = resultDocument.addElement("containers");
        for (DocumentModel parent : containers) {
            Element docElement = containersElement.addElement(documentTag);
            docElement.addElement(docRepositoryTag).setText(parent.getRepositoryName());
            docElement.addElement(docRefTag).setText(parent.getRef().toString());
            docElement.addElement(docTitleTag).setText(parent.getTitle());
            docElement.addElement(docPathTag).setText(parent.getPathAsString());
        }
        res.setEntity(resultDocument.asXML(), MediaType.APPLICATION_XML);
        res.getEntity().setCharacterSet(CharacterSet.UTF_8);
    }

}

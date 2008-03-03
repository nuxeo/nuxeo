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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.documenttemplates;

import javax.ejb.Remove;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * Stateful Seam component.
 *
 * <ul>
 * <li> lookup of document templates
 * <li> creation of document from a template
 * </ul>
 *   *
 */
public interface DocumentTemplatesActions {


    /**
     * Removes the components.
     *
     */
    @Remove
    void destroy();

    /**
     *
     * @return list of DocumentModels of available templates
     *         of currently selected type.
     * @throws ClientException
     */
    DocumentModelList getTemplates() throws ClientException;

    DocumentModelList getTemplates(String targetTypeName) throws ClientException;

    /**
     * Factory Accessor on the getter.
     *
     * @return
     */
    DocumentModelList templatesListFactory();

    /**
     * Creates a Document from a template.
     *
     * @param doc the DocumentModel with edited data
     * @param templateId the template id
     * @return
     * @throws ClientException
     */
    String createDocumentFromTemplate(DocumentModel doc, String templateId)
            throws ClientException;

    /**
     * Creates a Document from a template using the selectedTemplateId.
     *
     * @param doc
     * @return
     * @throws ClientException
     */
    String createDocumentFromTemplate(DocumentModel doc) throws ClientException;


    /**
     * Create a Document from a template using the selectedTemplateId
     * and the changeableDocument.
     *
     * @return
     * @throws ClientException
     */
    String createDocumentFromTemplate() throws ClientException;

    /**
     * Getter of the selected template id.
     *
     * @return
     */
    String getSelectedTemplateId();

    /**
     * Setter for the template to use.
     *
     * @param requestedId
     */
    void setSelectedTemplateId(String requestedId);

    /**
     * Getter for type of the document to be created.
     *
     * @return
     */
    String getTargetType();

    /**
     * Setter for the type of document to be created.
     *
     * @param targetType
     */
    void setTargetType(String targetType);


    /**
     * Listener to children changed event.
     *
     * @param targetDoc
     */
    void documentChildrenChanged(DocumentModel targetDoc);

    /**
     * Listener for domain changed event.
     *
     * @param targetDoc
     */
    void domainChanged(DocumentModel targetDoc);

}

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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.documenttemplates;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * Stateful Seam component.
 * <ul>
 * <li>lookup of document templates
 * <li>creation of document from a template
 * </ul>
 */
public interface DocumentTemplatesActions {

    /**
     * @return list of DocumentModels of available templates of currently selected type.
     */
    DocumentModelList getTemplates();

    DocumentModelList getTemplates(String targetTypeName);

    /**
     * Factory accessor on the getter.
     */
    DocumentModelList templatesListFactory();

    /**
     * Creates a Document from a template.
     *
     * @param doc the DocumentModel with edited data
     * @param templateId the template id
     */
    String createDocumentFromTemplate(DocumentModel doc, String templateId);

    /**
     * Creates a Document from a template using the selectedTemplateId.
     */
    String createDocumentFromTemplate(DocumentModel doc);

    /**
     * Creates a Document from a template using the selectedTemplateId and the changeableDocument.
     */
    String createDocumentFromTemplate();

    /**
     * Getter of the selected template id.
     */
    String getSelectedTemplateId();

    /**
     * Setter for the template to use.
     */
    void setSelectedTemplateId(String requestedId);

    /**
     * Getter for type of the document to be created.
     */
    String getTargetType();

    /**
     * Setter for the type of document to be created.
     */
    void setTargetType(String targetType);

    /**
     * Listener to children changed event.
     */
    void documentChildrenChanged();

    /**
     * Listener for domain changed event.
     */
    void domainChanged();

}

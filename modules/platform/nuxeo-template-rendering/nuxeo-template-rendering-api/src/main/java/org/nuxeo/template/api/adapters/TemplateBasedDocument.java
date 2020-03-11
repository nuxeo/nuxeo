/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.template.api.adapters;

import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.template.api.TemplateInput;

/**
 * Adapter interface for the {@link DocumentModel} that support rendering via a Template. This Document can be
 * associated with a {@link TemplateSourceDocument} that provides the rendering template as well as the default inputs
 * used for rendering.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public interface TemplateBasedDocument {

    /**
     * Associate the document to a Template.
     *
     * @param template DocumentModel holding the template
     * @param save flag to indicate if target DocumentModel must be saved or not
     * @return the updated DocumentModel
     * @throws PropertyException
     */
    DocumentModel setTemplate(DocumentModel template, boolean save) throws PropertyException;

    /**
     * Retrieve the {@link TemplateSourceDocument} for a given template name
     *
     * @param templateName name of the template
     * @return the {@link TemplateSourceDocument}
     */
    TemplateSourceDocument getSourceTemplate(String templateName);

    /**
     * Retrieve the Template {@link DocumentRef} for a given template name
     *
     * @param templateName name of the template
     * @return the associated template {@link DocumentRef}
     */
    DocumentRef getSourceTemplateDocRef(String templateName);

    /**
     * Retrieve the Template {@link DocumentModel} for a given template name
     *
     * @param templateName name of the template
     * @return the associated template {@link DocumentModel}
     */
    DocumentModel getSourceTemplateDoc(String templateName);

    /**
     * List all {@link TemplateSourceDocument}s that are bound to the underlying {@link DocumentModel}
     *
     * @return
     */
    List<TemplateSourceDocument> getSourceTemplates();

    /**
     * Return the template type for a given template name
     *
     * @param templateName
     * @return
     */
    String getTemplateType(String templateName);

    /**
     * Initialize the template parameters from the associated template
     *
     * @param templateName
     * @param save flag to indicate if target DocumentModel must be saved or not
     * @return the updated DocumentModel
     */
    DocumentModel initializeFromTemplate(String templateName, boolean save);

    /**
     * Render the named template against the underlying DocumentModel and store the result in the main Blob
     *
     * @param templateName
     * @param save flag to indicate if target DocumentModel must be saved or not
     * @return the resulting {@link Blob}
     */
    Blob renderAndStoreAsAttachment(String templateName, boolean save);

    /**
     * Render the named template against the underlying DocumentModel
     *
     * @param templateName
     * @return the resulting {@link Blob}
     */
    Blob renderWithTemplate(String templateName);

    /**
     * Indicate of the associated Template requires parameters or not
     *
     * @param templateName
     * @return
     */
    boolean hasParams(String templateName);

    /**
     * Retrieve parameters for the associated template
     *
     * @param templateName
     * @return
     */
    List<TemplateInput> getParams(String templateName);

    /**
     * Save parameters changes.
     *
     * @param templateName
     * @param params the updated list of parameters
     * @param save flag to indicate if target DocumentModel must be saved or not
     * @return
     */
    DocumentModel saveParams(String templateName, List<TemplateInput> params, boolean save);

    /**
     * Return the underlying adapted {@link DocumentModel}
     *
     * @return
     */
    DocumentModel getAdaptedDoc();

    /**
     * Return the {@link Blob} of the associated template
     *
     * @param templateName
     * @return
     */
    Blob getTemplateBlob(String templateName);

    /**
     * Indicate of the associated Template has editable parameters or not
     *
     * @param templateName
     * @return
     */
    boolean hasEditableParams(String templateName);

    /**
     * Find the template associated to a given RenditionName
     *
     * @param renditionName
     * @return the template name if any, null otherwise
     */
    String getTemplateNameForRendition(String renditionName);

    /**
     * Get the names of all the associated templates
     *
     * @return
     */
    List<String> getTemplateNames();

    /**
     * Detach a template from the underlying {@link DocumentModel}
     *
     * @param templateName
     * @param save
     * @return
     */
    DocumentModel removeTemplateBinding(String templateName, boolean save);
}

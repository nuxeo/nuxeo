/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.TemplateProcessor;

/**
 * It is mainly the source used by {@link TemplateBasedDocument} to handle the rendering.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public interface TemplateSourceDocument {

    String INIT_DONE_FLAG = "TEMPLATE_INIT_DONE";

    /**
     * Return the String representation of the parameters of the template
     */
    String getParamsAsString() throws PropertyException;

    /**
     * Add or update a {@link TemplateInput} to the list of template parameters.
     */
    List<TemplateInput> addInput(TemplateInput input);

    /**
     * Returns whether or not the {@link TemplateInput} already exists, based on the name, in the template.
     */
    boolean hasInput(String inputName);

    /**
     * Return the template Type (i.e. the associated {@link TemplateProcessor} name.
     *
     * @return {@link TemplateProcessor} name if any, null otherwise
     */
    String getTemplateType();

    /**
     * Initialize the DocumentModel
     * <ul>
     * <li>finds associated TemplateProcessor</li>
     * <li>extract Template parameters</li>
     * </ul>
     *
     * @param save flag to indicate if target DocumentModel must be saved or not
     */
    void initTemplate(boolean save);

    /**
     * Initialize the Types2Template binding
     */
    void initTypesBindings();

    /**
     * Retrieve the Blob holding the template file
     */
    Blob getTemplateBlob() throws PropertyException;

    /**
     * Retrieve the parameters associated to the Template file
     */
    List<TemplateInput> getParams() throws PropertyException;

    /**
     * Save parameters changes
     *
     * @param params the updated list of parameters
     * @param save flag to indicate if target DocumentModel must be saved or not
     * @return the updated DocumentModel
     */
    DocumentModel saveParams(List<TemplateInput> params, boolean save);

    /**
     * Return the underlying adapted {@link DocumentModel}s
     */
    DocumentModel getAdaptedDoc();

    /**
     * Save changes in the underlying {@link DocumentModel}
     */
    DocumentModel save();

    /**
     * Return flag to indicate if Documents associated to this template can override parametes value
     */
    boolean allowInstanceOverride();

    /**
     * Indicate of the associated Template has editable parameters or not
     */
    boolean hasEditableParams();

    /**
     * Get List of Document Types than can be associated to this Template.
     *
     * @return List of Document Types or an empty List
     */
    List<String> getApplicableTypes();

    /**
     * Get List of Document Types that must be automatically bound to this template at creation time
     *
     * @return List of Document Types or an empty List
     */
    List<String> getForcedTypes();

    /**
     * Get the list of {@link TemplateBasedDocument}s associated to this template
     */
    List<TemplateBasedDocument> getTemplateBasedDocuments();

    /**
     * Remove Type mapping for this template
     */
    void removeForcedType(String type, boolean save);

    /**
     * Update the Type mapping for this template
     */
    void setForcedTypes(String[] forcedTypes, boolean save);

    /**
     * Sets the expected output mime-type. If the expected mime-type is different from the output of the rendering,
     * converters will be applied.
     */
    void setOutputFormat(String mimetype, boolean save);

    /**
     * Return the expected mime-type of the resulting rendering
     */
    String getOutputFormat();

    /**
     * Indicate if the template can be used as main blob in the {@link TemplateBasedDocument} (i.e. if the template is
     * editable by the end user)
     */
    boolean useAsMainContent();

    /**
     * Shortcut to access the underlying {@link DocumentModel} name
     */
    String getName();

    /**
     * Shortcut to access the underlying {@link Blob} filename
     *
     * @return name
     */
    String getFileName();

    /**
     * Shortcut to access the underlying {@link DocumentModel} title
     *
     * @return template filename
     */
    String getTitle();

    /**
     * Shortcut to access the underlying {@link DocumentModel} versionLabel
     *
     * @return versionLabel
     */
    String getVersionLabel();

    /**
     * Shortcut to access the underlying {@link DocumentModel} uuid
     *
     * @return UUID
     */
    String getId();

    /**
     * Return label key used for template
     */
    String getLabel();

    /**
     * Associate Template to a Rendition
     */
    void setTargetRenditioName(String renditionName, boolean save);

    /**
     * Get the associated Rendition if any
     *
     * @return Rendition name or null
     */
    String getTargetRenditionName();

    /**
     * Write accessor to the {@link Blob} used to store the template
     */
    void setTemplateBlob(Blob blob, boolean save);

}

/*
 * (C) Copyright 2006-20012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

    public static final String INIT_DONE_FLAG = "TEMPLATE_INIT_DONE";

    /**
     * Return the String representation of the parameters of the template
     *
     * @return
     * @throws PropertyException
     */
    public String getParamsAsString() throws PropertyException;

    /**
     * Add or update a {@link TemplateInput} to the list of template parameters.
     *
     * @param input
     * @return
     */
    public List<TemplateInput> addInput(TemplateInput input);

    /**
     * Returns whether or not the {@link TemplateInput} already exists, based on the name, in the template.
     */
    boolean hasInput(String inputName);

    /**
     * Return the template Type (i.e. the associated {@link TemplateProcessor} name.
     *
     * @return {@link TemplateProcessor} name if any, null otherwise
     */
    public String getTemplateType();

    /**
     * Initialize the DocumentModel
     * <ul>
     * <li>finds associated TemplateProcessor</li>
     * <li>extract Template parameters</li>
     * </ul>
     *
     * @param save flag to indicate if target DocumentModel must be saved or not
     */
    public void initTemplate(boolean save);

    /**
     * Initialize the Types2Template binding
     */
    public void initTypesBindings();

    /**
     * Retrieve the Blob holding the template file
     *
     * @return
     * @throws PropertyException
     */
    public Blob getTemplateBlob() throws PropertyException;

    /**
     * Retrieve the parameters associated to the Template file
     *
     * @return
     * @throws PropertyException
     */
    public List<TemplateInput> getParams() throws PropertyException;

    /**
     * Save parameters changes
     *
     * @param params the updated list of parameters
     * @param save flag to indicate if target DocumentModel must be saved or not
     * @return the updated DocumentModel
     */
    public DocumentModel saveParams(List<TemplateInput> params, boolean save);

    /**
     * Return the underlying adapted {@link DocumentModel}s
     *
     * @return
     */
    public DocumentModel getAdaptedDoc();

    /**
     * Save changes in the underlying {@link DocumentModel}
     *
     * @return
     */
    public DocumentModel save();

    /**
     * Return flag to indicate if Documents associated to this template can override parametes value
     *
     * @return
     */
    public boolean allowInstanceOverride();

    /**
     * Indicate of the associated Template has editable parameters or not
     *
     * @return
     */
    public boolean hasEditableParams();

    /**
     * Get List of Document Types than can be associated to this Template.
     *
     * @return List of Document Types or an empty List
     */
    public List<String> getApplicableTypes();

    /**
     * Get List of Document Types that must be automatically bound to this template at creation time
     *
     * @return List of Document Types or an empty List
     */
    public List<String> getForcedTypes();

    /**
     * Get the list of {@link TemplateBasedDocument}s associated to this template
     *
     * @return
     */
    public List<TemplateBasedDocument> getTemplateBasedDocuments();

    /**
     * Remove Type mapping for this template
     *
     * @param type
     * @param save
     */
    public void removeForcedType(String type, boolean save);

    /**
     * Update the Type mapping for this template
     *
     * @param forcedTypes
     * @param save
     */
    public void setForcedTypes(String[] forcedTypes, boolean save);

    /**
     * Sets the expected output mime-type. If the expected mime-type is different from the output of the rendering,
     * converters will be applied.
     *
     * @param mimetype
     * @param save
     */
    public void setOutputFormat(String mimetype, boolean save);

    /**
     * Return the expected mime-type of the resulting rendering
     *
     * @return
     */
    public String getOutputFormat();

    /**
     * Indicate if the template can be used as main blob in the {@link TemplateBasedDocument} (i.e. if the template is
     * editable by the end user)
     *
     * @return
     */
    public boolean useAsMainContent();

    /**
     * Shortcut to access the underlying {@link DocumentModel} name
     *
     * @return
     */
    public String getName();

    /**
     * Shortcut to access the underlying {@link Blob} filename
     *
     * @return name
     */
    public String getFileName();

    /**
     * Shortcut to access the underlying {@link DocumentModel} title
     *
     * @return template filename
     */
    public String getTitle();

    /**
     * Shortcut to access the underlying {@link DocumentModel} versionLabel
     *
     * @return versionLabel
     */
    public String getVersionLabel();

    /**
     * Shortcut to access the underlying {@link DocumentModel} uuid
     *
     * @return UUID
     */
    public String getId();

    /**
     * Return label key used for template
     *
     * @return
     */
    public String getLabel();

    /**
     * Associate Template to a Rendition
     *
     * @param renditionName
     * @param save
     */
    public void setTargetRenditioName(String renditionName, boolean save);

    /**
     * Get the associated Rendition if any
     *
     * @return Rendition name or null
     */
    public String getTargetRenditionName();

    /**
     * Write accessor to the {@link Blob} used to store the template
     *
     * @param blob
     * @param save
     */
    public void setTemplateBlob(Blob blob, boolean save);

}

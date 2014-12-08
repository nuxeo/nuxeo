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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.model.PropertyException;
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
     * @throws ClientException
     */
    public DocumentModel setTemplate(DocumentModel template, boolean save) throws PropertyException, ClientException;

    /**
     * Retrieve the {@link TemplateSourceDocument} for a given template name
     * 
     * @param templateName name of the template
     * @return the {@link TemplateSourceDocument}
     * @throws Exception
     */
    public TemplateSourceDocument getSourceTemplate(String templateName) throws Exception;

    /**
     * Retrieve the Template {@link DocumentRef} for a given template name
     * 
     * @param templateName name of the template
     * @return the associated template {@link DocumentRef}
     * @throws Exception
     */
    public DocumentRef getSourceTemplateDocRef(String templateName) throws Exception;

    /**
     * Retrieve the Template {@link DocumentModel} for a given template name
     * 
     * @param templateName name of the template
     * @return the associated template {@link DocumentModel}
     * @throws Exception
     */
    public DocumentModel getSourceTemplateDoc(String templateName) throws Exception;

    /**
     * List all {@link TemplateSourceDocument}s that are bound to the underlying {@link DocumentModel}
     * 
     * @return
     */
    public List<TemplateSourceDocument> getSourceTemplates();

    /**
     * Return the template type for a given template name
     * 
     * @param templateName
     * @return
     */
    public String getTemplateType(String templateName);

    /**
     * Initialize the template parameters from the associated template
     * 
     * @param templateName
     * @param save flag to indicate if target DocumentModel must be saved or not
     * @return the updated DocumentModel
     * @throws Exception
     */
    public DocumentModel initializeFromTemplate(String templateName, boolean save) throws Exception;

    /**
     * Render the named template against the underlying DocumentModel and store the result in the main Blob
     * 
     * @param templateName
     * @param save flag to indicate if target DocumentModel must be saved or not
     * @return the resulting {@link Blob}
     * @throws Exception
     */
    public Blob renderAndStoreAsAttachment(String templateName, boolean save) throws Exception;

    /**
     * Render the named template against the underlying DocumentModel
     * 
     * @param templateName
     * @return the resulting {@link Blob}
     * @throws Exception
     */
    public Blob renderWithTemplate(String templateName) throws Exception;

    /**
     * Indicate of the associated Template requires parameters or not
     * 
     * @param templateName
     * @return
     * @throws ClientException
     */
    public boolean hasParams(String templateName) throws ClientException;

    /**
     * Retrieve parameters for the associated template
     * 
     * @param templateName
     * @return
     * @throws ClientException
     */
    public List<TemplateInput> getParams(String templateName) throws ClientException;

    /**
     * Save parameters changes.
     * 
     * @param templateName
     * @param params the updated list of parameters
     * @param save flag to indicate if target DocumentModel must be saved or not
     * @return
     * @throws Exception
     */
    public DocumentModel saveParams(String templateName, List<TemplateInput> params, boolean save) throws Exception;

    /**
     * Return the underlying adapted {@link DocumentModel}
     * 
     * @return
     */
    public DocumentModel getAdaptedDoc();

    /**
     * Return the {@link Blob} of the associated template
     * 
     * @param templateName
     * @return
     * @throws Exception
     */
    public Blob getTemplateBlob(String templateName) throws Exception;

    /**
     * Indicate of the associated Template has editable parameters or not
     * 
     * @param templateName
     * @return
     * @throws ClientException
     */
    public boolean hasEditableParams(String templateName) throws ClientException;

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
    public List<String> getTemplateNames();

    /**
     * Detach a template from the underlying {@link DocumentModel}
     * 
     * @param templateName
     * @param save
     * @return
     * @throws ClientException
     */
    public DocumentModel removeTemplateBinding(String templateName, boolean save) throws ClientException;
}
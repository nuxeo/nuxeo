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
package org.nuxeo.template.adapters.doc;

import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.template.TemplateInput;
import org.nuxeo.template.adapters.source.TemplateSourceDocument;

/**
 * Adapter interface for the {@link DocumentModel} that support rendering via a
 * Template. This Document can be associated with a
 * {@link TemplateSourceDocument} that provides the rendering template as well
 * as the default inputs used for rendering.
 * 
 * @author Tiry (tdelprat@nuxeo.com)
 * 
 */
public interface TemplateBasedDocument {

    public DocumentModel setTemplate(DocumentModel template, boolean save)
            throws PropertyException, ClientException;

    public TemplateSourceDocument getSourceTemplate(String templateName)
            throws Exception;

    public DocumentModel getSourceTemplateDoc(String templateName)
            throws Exception;

    public List<TemplateSourceDocument> getSourceTemplates();

    public String getTemplateType(String templateName);

    public DocumentModel initializeFromTemplate(String templateName,
            boolean save) throws Exception;

    public Blob renderAndStoreAsAttachment(String templateName, boolean save)
            throws Exception;

    public boolean isBidirectional();

    public Blob renderWithTemplate(String templateName) throws Exception;

    /*
     * public DocumentModel updateDocumentModelFromBlob(boolean save) throws
     * Exception;
     */

    public boolean hasParams(String templateName) throws ClientException;

    public List<TemplateInput> getParams(String templateName)
            throws ClientException;

    public DocumentModel saveParams(String templateName,
            List<TemplateInput> params, boolean save) throws Exception;

    public DocumentModel getAdaptedDoc();

    public Blob getTemplateBlob(String templateName) throws Exception;

    public boolean hasEditableParams(String templateName)
            throws ClientException;

    String getTemplateNameForRendition(String renditionName);

    public List<String> getTemplateNames();

    public DocumentModel removeTemplateBinding(String templateName, boolean save)
            throws ClientException;
}
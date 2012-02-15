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
package org.nuxeo.ecm.platform.template.adapters.doc;

import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.template.TemplateInput;
import org.nuxeo.ecm.platform.template.adapters.source.TemplateSourceDocument;

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

    public TemplateSourceDocument getSourceTemplate() throws Exception;

    public DocumentModel getSourceTemplateDoc() throws Exception;

    public String getTemplateType();

    public DocumentModel initializeFromTemplate(boolean save) throws Exception;

    public Blob renderAndStoreAsAttachment(boolean save) throws Exception;

    public boolean isBidirectional();

    public Blob renderWithTemplate() throws Exception;

    public DocumentModel updateDocumentModelFromBlob(boolean save)
            throws Exception;

    public List<TemplateInput> getParams() throws PropertyException,
            ClientException;

    public DocumentModel saveParams(List<TemplateInput> params, boolean save)
            throws Exception;

    public DocumentModel getAdaptedDoc();

    public Blob getTemplateBlob() throws Exception;

    public boolean hasEditableParams() throws ClientException;
}
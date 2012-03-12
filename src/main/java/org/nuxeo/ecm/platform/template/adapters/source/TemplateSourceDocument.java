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

package org.nuxeo.ecm.platform.template.adapters.source;

import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.template.TemplateInput;
import org.nuxeo.ecm.platform.template.adapters.doc.TemplateBasedDocument;

/**
 * Adapter interface for the {@link DocumentModel} that can provide a template.
 * It is mainly the source used by {@link TemplateBasedDocument} to handle the
 * rendering.
 * 
 * @author Tiry (tdelprat@nuxeo.com)
 * 
 */
public interface TemplateSourceDocument {

    public static final String INIT_DONE_FLAG = "TEMPLATE_INIT_DONE";

    public String getParamsAsString() throws PropertyException, ClientException;

    public List<TemplateInput> addInput(TemplateInput input) throws Exception;

    public String getTemplateType();

    public void initTemplate(boolean save) throws Exception;

    public void initTypesBindings() throws Exception;

    public Blob getTemplateBlob() throws PropertyException, ClientException;

    public List<TemplateInput> getParams() throws PropertyException,
            ClientException;

    public DocumentModel saveParams(List<TemplateInput> params, boolean save)
            throws Exception;

    public DocumentModel getAdaptedDoc();

    public DocumentModel save() throws ClientException;

    public boolean allowInstanceOverride();

    public boolean hasEditableParams() throws ClientException;

    public List<String> getApplicableTypes();

    public List<String> getForcedTypes();

    public List<TemplateBasedDocument> getTemplateBasedDocuments()
            throws ClientException;

    public void removeForcedType(String type, boolean save)
            throws ClientException;

    public void setForcedTypes(String[] forcedTypes, boolean save)
            throws ClientException;

    public void setOutputFormat(String mimetype, boolean save);

    public String getOutputFormat();

    public boolean useAsMainContent();

    public String getName();

    public String getFileName() throws ClientException;

    public String getTitle() throws ClientException;

    public String getVersionLabel();

    public String getId();

    public String getLabel() throws ClientException;

    public void setTargetRenditioName(String renditionName, boolean save)
            throws ClientException;

    public String getTargetRenditionName() throws ClientException;

    public void setTemplateBlob(Blob blob, boolean save) throws Exception;

}
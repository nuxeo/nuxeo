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

package org.nuxeo.ecm.platform.template.adapters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.template.TemplateInput;
import org.nuxeo.ecm.platform.template.XMLSerializer;
import org.nuxeo.ecm.platform.template.adapters.doc.TemplateBasedDocument;
import org.nuxeo.ecm.platform.template.adapters.source.TemplateSourceDocument;
import org.nuxeo.ecm.platform.template.processors.TemplateProcessor;
import org.nuxeo.ecm.platform.template.service.TemplateProcessorService;
import org.nuxeo.runtime.api.Framework;

/**
 * Base class for shared code bewteen the {@link TemplateBasedDocument} and the
 * {@link TemplateSourceDocument}.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public abstract class AbstractTemplateDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static Log log = LogFactory.getLog(AbstractTemplateDocument.class);

    protected DocumentModel adaptedDoc;

    protected CoreSession getSession() {
        if (adaptedDoc == null) {
            return null;
        }
        return CoreInstance.getInstance().getSession(adaptedDoc.getSessionId());
    }

    public DocumentModel getAdaptedDoc() {
        return adaptedDoc;
    }

    protected abstract String getTemplateParamsXPath();

    public Blob getTemplateBlob() throws PropertyException, ClientException {
        BlobHolder bh = getAdaptedDoc().getAdapter(BlobHolder.class);
        if (bh != null) {
            return bh.getBlob();
        }
        return null;
    }

    public List<TemplateInput> getParams() throws ClientException {
        String dataPath = getTemplateParamsXPath();

        if (adaptedDoc.getPropertyValue(dataPath) == null) {
            return new ArrayList<TemplateInput>();
        }
        String xml = adaptedDoc.getPropertyValue(dataPath).toString();

        try {
            return XMLSerializer.readFromXml(xml);
        } catch (Exception e) {
            log.error("Unable to parse parameters", e);
            return new ArrayList<TemplateInput>();
        }
    }

    public DocumentModel saveParams(List<TemplateInput> params, boolean save)
            throws Exception {
        String dataPath = getTemplateParamsXPath();
        String xml = XMLSerializer.serialize(params);
        adaptedDoc.setPropertyValue(dataPath, xml);
        if (save) {
            adaptedDoc = getSession().saveDocument(adaptedDoc);
        }
        return adaptedDoc;
    }

    public DocumentModel save() throws ClientException {
        return getSession().saveDocument(adaptedDoc);
    }

    public abstract String getTemplateType();

    protected TemplateProcessor getTemplateProcessor() {
        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);
        return tps.getProcessor(getTemplateType());
    }

    public boolean hasEditableParams() throws ClientException {
        for (TemplateInput param : getParams()) {
            if (!param.isReadOnly()) {
                return true;
            }
        }
        return false;
    }

}

/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.template.automation;

import org.dom4j.DocumentException;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.XMLSerializer;
import org.nuxeo.template.adapters.doc.TemplateBindings;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;

import java.util.List;

/**
 * Operation to wrapp the rendition process
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@Operation(id = RenderWithTemplateOperation.ID, category = Constants.CAT_CONVERSION, label = "Render with template", description = "Render the target document with the associated template if any. Returns the rendered Blob or the main Blob if no template is associated to the document.")
public class RenderWithTemplateOperation {

    public static final String ID = "TemplateProcessor.Render";

    @Context
    protected OperationContext ctx;

    @Param(name = "templateName", required = false)
    protected String templateName = TemplateBindings.DEFAULT_BINDING;

    @Param(name = "store", required = false, values = "false")
    protected Boolean store = false;

    @Param(name = "save", required = false, values = "true")
    protected Boolean save = true;

    @Param(name = "attach", required = false)
    protected Boolean attach = false;

    @Param(name = "templateData", required = false)
    protected String templateData = null;

    @OperationMethod
    public Blob run(DocumentModel targetDocument) {
        TemplateBasedDocument renderable = targetDocument.getAdapter(TemplateBasedDocument.class);
        if (attach && (renderable == null || !renderable.getTemplateNames().contains(templateName))) {
            TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);
            List<DocumentModel> templates = tps.getTemplateDocs(ctx.getCoreSession(), templateName);
            renderable = (templates == null || templates.size() == 0) ? null :
                tps.makeTemplateBasedDocument(targetDocument, templates.get(0), true)
                    .getAdapter(TemplateBasedDocument.class);
        }
        if (renderable != null) {
            if (templateData != null) {
                List<TemplateInput> params;
                try {
                    params = XMLSerializer.readFromXml(templateData);
                } catch (DocumentException e) {
                    throw new NuxeoException(e.getMessage(), e);
                }
                if (params != null) {
                    renderable.saveParams(templateName, params, true);
                }
            }
            if (store) {
                return renderable.renderAndStoreAsAttachment(templateName, save);
            } else {
                return renderable.renderWithTemplate(templateName);
            }
        } else {
            BlobHolder bh = targetDocument.getAdapter(BlobHolder.class);
            if (bh != null) {
                return bh.getBlob();
            } else {
                return null;
            }
        }
    }
}

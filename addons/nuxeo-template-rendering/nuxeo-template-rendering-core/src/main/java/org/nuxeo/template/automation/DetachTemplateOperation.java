/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Gabriel Barata <gbarata@nuxeo.com>
 */
package org.nuxeo.template.automation;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

import java.util.List;

/**
 * Operation to detach an bound documents from a template.
 *
 * @since 9.1
 */
@Operation(id = DetachTemplateOperation.ID, category = Constants.CAT_CONVERSION,
           label = "Detach a template",
           description = "Detach a template from all its bound documents.")
public class DetachTemplateOperation {

    public static final String ID = "TemplateProcessor.Detach";

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public void run(DocumentModel targetTemplate) {
        TemplateSourceDocument template = targetTemplate.getAdapter(TemplateSourceDocument.class);
        TemplateProcessorService tps = Framework.getService(TemplateProcessorService.class);
        List<TemplateBasedDocument> documents = template.getTemplateBasedDocuments();
        documents.forEach(doc -> tps.detachTemplateBasedDocument(doc.getAdaptedDoc(), template.getName(), true));
    }
}

/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.platform.rendition.operation;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.operations.document.PublishDocument;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.api.Framework;

/**
 * Publish the rendition of a document. If rendition is not given and default rendition option is false, it falls back
 * on {@link PublishDocument} operation.
 *
 * @since 10.3
 */
@Operation(id = PublishRendition.ID, category = Constants.CAT_DOCUMENT, label = "Publish Document's Rendition", description = "input document's chosen rendition into the target section. If rendition is not given and default rendition option is false, it falls back on default publishing. Existing proxy is overrided if the override attribute is set. Return the created proxy.", aliases = {
        "Document.PublishRendition" })
public class PublishRendition {

    public static final String ID = PublishDocument.ID;

    @Context
    protected CoreSession session;

    @Param(name = "renditionName", required = false)
    protected String renditionName;

    @Param(name = "defaultRendition", required = false)
    protected boolean defaultRendition;

    @Param(name = "target")
    protected DocumentModel target;

    @Param(name = "override", required = false, values = "true")
    protected boolean override = true;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        if (!defaultRendition && StringUtils.isEmpty(renditionName)) {
            return session.publishDocument(doc, target, override);
        } else {
            RenditionService rs = Framework.getService(RenditionService.class);
            return rs.publishRendition(doc, target, renditionName, override);
        }
    }

    @OperationMethod
    public DocumentModelList run(DocumentModelList docs) {
        DocumentModelList result = new DocumentModelListImpl();
        for (DocumentModel doc : docs) {
            result.add(run(doc));
        }
        return result;
    }

}

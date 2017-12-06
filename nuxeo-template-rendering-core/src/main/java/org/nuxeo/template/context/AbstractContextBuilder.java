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
package org.nuxeo.template.context;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.context.DocumentWrapper;

public abstract class AbstractContextBuilder {

    protected static final Log log = LogFactory.getLog(AbstractContextBuilder.class);

    public static final String[] RESERVED_VAR_NAMES = { "doc", "document", "blobHolder", "username", "principal",
            "templateName" };

    public Map<String, Object> build(DocumentModel doc, DocumentWrapper nuxeoWrapper, String templateName) {

        Map<String, Object> ctx = new HashMap<String, Object>();

        CoreSession session = doc.getCoreSession();

        // doc infos
        ctx.put("doc", nuxeoWrapper.wrap(doc));
        ctx.put("document", nuxeoWrapper.wrap(doc));

        // blob wrapper
        ctx.put("blobHolder", new BlobHolderWrapper(doc));

        // user info
        ctx.put("username", session.getPrincipal().getName());
        ctx.put("principal", session.getPrincipal());

        ctx.put("templateName", templateName);

        // fetch extensions
        TemplateProcessorService tps = Framework.getService(TemplateProcessorService.class);
        tps.addContextExtensions(doc, nuxeoWrapper, ctx);

        return ctx;
    }

    public Map<String, Object> build(TemplateBasedDocument templateBasedDocument, String templateName) {

        DocumentModel doc = templateBasedDocument.getAdaptedDoc();

        Map<String, Object> context = build(doc, templateName);

        return context;
    }

    protected abstract DocumentWrapper getWrapper();

    public Map<String, Object> build(DocumentModel doc, String templateName) {

        return build(doc, getWrapper(), templateName);
    }
}

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
package org.nuxeo.ecm.automation.core.rendering.operations;

import java.io.IOException;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.rendering.RenderingService;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.runtime.services.resource.ResourceService;

import freemarker.template.TemplateException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = RenderDocumentFeed.ID, category = Constants.CAT_CONVERSION, label = "Render Document Feed", description = "Get a list of documents as input and outputs a single blob containing the rendering of the document list. The template attribute may contain either the template content either a template URI. Template URis are strings in the form 'template:template_name' and will be located using the runtime resource service. Return the rendered blob")
public class RenderDocumentFeed {

    public static final String ID = "Render.DocumentFeed";

    @Context
    protected ResourceService rs;

    @Context
    protected OperationContext ctx;

    @Param(name = "template", widget = Constants.W_TEMPLATE_RESOURCE)
    protected String template;

    @Param(name = "type", widget = Constants.W_OPTION, required = false, values = { "ftl", "mvel" })
    protected String type = "ftl";

    @Param(name = "filename", required = false, values = "output.ftl")
    protected String name = "output.ftl";

    @Param(name = "mimetype", required = false, values = "text/xml")
    protected String mimeType = "text/xml";

    @Param(name = "charset", required = false)
    protected String charset = "UTF-8";

    @OperationMethod
    public Blob run(DocumentModelList docs) throws OperationException, RenderingException, TemplateException,
            IOException {
        String content = RenderingService.getInstance().render(type, template, ctx);
        return Blobs.createBlob(content, mimeType, charset, name);
    }

}

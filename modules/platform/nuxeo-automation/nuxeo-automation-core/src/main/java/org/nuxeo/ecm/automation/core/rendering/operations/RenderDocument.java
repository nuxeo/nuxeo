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
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.automation.core.rendering.RenderingService;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.runtime.services.resource.ResourceService;

import freemarker.template.TemplateException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = RenderDocument.ID, category = Constants.CAT_CONVERSION, label = "Render Document", description = "Get a document or a list of document in input and outputs one or more blobs that contain a rendered view for each input document given a rendering template. The template attribute may contain either the template content either a template URI. Template URis are strings in the form 'template:template_name' and will be located using the runtime resource service. Return the rendered file(s) as blob(s)")
public class RenderDocument {

    public static final String ID = "Render.Document";

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

    @OperationMethod(collector = BlobCollector.class)
    public Blob run(DocumentModel doc) throws OperationException, RenderingException, TemplateException, IOException {
        String content = RenderingService.getInstance().render(type, template, ctx);
        return Blobs.createBlob(content, mimeType, null, name);
    }

}

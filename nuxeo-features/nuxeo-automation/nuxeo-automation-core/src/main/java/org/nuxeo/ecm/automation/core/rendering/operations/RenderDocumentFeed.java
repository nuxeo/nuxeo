/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.rendering.operations;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.rendering.RenderingService;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.runtime.services.resource.ResourceService;

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

    @Param(name = "type", widget = Constants.W_OPTION, required = false, values = {"ftl", "mvel"})
    protected String type = "ftl";

    @Param(name = "filename", required = false, values="output.ftl")
    protected String name = "output.ftl";

    @Param(name = "mimetype", required = false, values="text/xml")
    protected String mimeType = "text/xml";

    @OperationMethod
    public Blob run(DocumentModelList docs) throws Exception {
        String content = RenderingService.getInstance().render(type, template, ctx);
        StringBlob blob = new StringBlob(content);
        blob.setFilename(name);
        blob.setMimeType(mimeType);
        return blob;
    }

}

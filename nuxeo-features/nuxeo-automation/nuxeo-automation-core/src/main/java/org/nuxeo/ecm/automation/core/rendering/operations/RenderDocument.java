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
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.automation.core.rendering.RenderingService;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.runtime.services.resource.ResourceService;

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

    @Param(name = "type", widget = Constants.W_OPTION, required = false, values = {"ftl", "mvel"})
    protected String type = "ftl";

    @Param(name = "filename", required = false, values="output.ftl")
    protected String name = "output.ftl";

    @Param(name = "mimetype", required = false, values="text/xml")
    protected String mimeType = "text/xml";

    @OperationMethod(collector = BlobCollector.class)
    public Blob run(DocumentModel doc) throws Exception {
        String content = RenderingService.getInstance().render(type, template, ctx);
        StringBlob blob = new StringBlob(content);
        blob.setFilename(name);
        blob.setMimeType(mimeType);
        return blob;
    }

}

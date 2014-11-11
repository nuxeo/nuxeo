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
package org.nuxeo.ecm.automation.core.rendering;

import java.util.Map;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.scripting.Scripting;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RenderingService {

    //TODO use a runtime service
    private static RenderingService instance = new RenderingService();

    public static RenderingService getInstance() {
        return instance;
    }

    protected MvelRender mvel = new MvelRender();

    protected FreemarkerRender ftl = new FreemarkerRender();


    public String render(String type, String uriOrContent, OperationContext ctx) throws Exception {
        Map<String, Object> map = Scripting.initBindings(ctx);
        //map.put("DocUrl", MailTemplateHelper.getDocumentUrl(doc, viewId));
        return getRenderer(type).render(uriOrContent, map);
    }


    public Renderer getRenderer(String type) {
        if ("mvel".equals(type)) {
            return mvel;
        } else {
            return ftl;
        }
    }

}

/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

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
package org.nuxeo.ecm.webengine.model.impl;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.app.impl.DefaultContext;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class RootResource extends ModuleRoot {

    protected RootResource(UriInfo info, HttpHeaders headers, String moduleName) {
        WebContext ctx = WebEngine.getActiveContext();
        Module module = ctx.getEngine().getModule(moduleName);
        ((DefaultContext) ctx).setUriInfo(info);
        ((DefaultContext) ctx).setHttpHeaders(headers);
        ((DefaultContext) ctx).setModule(module);
        WebObject wo = getClass().getAnnotation(WebObject.class);
        if (wo == null) {
            throw new IllegalStateException(
                    "You should use WebObject annotation on module root resources");
        }
        try {
            initialize(ctx, module.getType(wo.type()));
        } finally {
            ctx.push(this);
            setRoot(true);
        }
    }

}

/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.rest.domains;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.core.Context;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.WebEngine2;
import org.nuxeo.ecm.webengine.rest.adapters.WebObject;

import com.sun.jersey.api.core.HttpContext;

/**
 * The dispatch is using by default a right path match (limited=false).
 * This way we avoid generating a resource chain corresponding to each
 * segment in the path.
 * Anyway in some cases you may want a segment by segment dispatch
 * to build a chain of resources for each segment. In this case you need to use the
 * {@link ChainingWebDomain} variant. See {@link DocumentDomain} for an example.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@ProduceMime({"text/html", "*/*"})
public class DefaultWebDomain<T extends DomainDescriptor> extends AbstractWebDomain<T> {


    public DefaultWebDomain(WebEngine2 engine, T desc) throws WebException {
        super (engine, desc);
    }


    protected WebObject resolve(WebContext2 ctx, String path) throws WebException {
        if (descriptor.type != null) { // the type of resource to serve is defined
            WebObject obj = ctx.getEngine().getWebTypeManager().newInstance(descriptor.type);
            if (obj != null) {
                obj.initialize(ctx, path);
                return obj;
            }
        }
        return null;
    }


    @Path(value="{path}", limited=false)
    public WebObject dispatch(@PathParam("path") String path, @Context HttpContext ctx) throws Exception {
        WebContext2 webCtx = (WebContext2)ctx;
        webCtx.setDomain(this);
        return resolve((WebContext2)ctx, path);
    }



}

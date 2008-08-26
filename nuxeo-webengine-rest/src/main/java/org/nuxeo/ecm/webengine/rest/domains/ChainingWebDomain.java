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
import javax.ws.rs.core.Context;

import org.nuxeo.ecm.core.CoreService;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.WebEngine2;
import org.nuxeo.ecm.webengine.rest.adapters.WebObject;
import org.nuxeo.runtime.api.Framework;

import com.sun.jersey.api.core.HttpContext;

/**
 * This domain is initiating a chain resolving of objects on the traversal path
 * because of limited = true
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ChainingWebDomain<T extends DomainDescriptor> extends DefaultWebDomain<T> {


    public ChainingWebDomain(WebEngine2 engine, T desc) throws WebException {
        super (engine, desc);
    }

    @Path(value="{path}", limited=true)
    public WebObject dispatch(@PathParam("path") String path, @Context HttpContext ctx) throws Exception {
        return super.dispatch(path, ctx);
    }


}

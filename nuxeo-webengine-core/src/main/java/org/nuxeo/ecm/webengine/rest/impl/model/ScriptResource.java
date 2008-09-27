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

package org.nuxeo.ecm.webengine.rest.impl.model;

import javax.ws.rs.GET;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.model.ManagedResource;
import org.nuxeo.ecm.webengine.rest.model.WebApplication;
import org.nuxeo.ecm.webengine.rest.model.WebType;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ScriptResource extends ManagedResource {

    @GET
    public Object get() {
        return "hello world!";
    }

    public ScriptResource(WebApplication cfg) throws WebException {
        super (cfg);
    }
    
    @Override
    protected WebType getResourceType(WebContext2 ctx) throws WebException {
        return ctx.getApplication().getType("Script");
    }


}

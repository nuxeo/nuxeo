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
 */
package org.nuxeo.ecm.webengine.cmis;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.nuxeo.ecm.webengine.atom.WorkspaceResource;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@WebObject(type="CmisWorkspace")
public class CMISWorkspaceResource extends WorkspaceResource {

    @Path("services")
    public Resource getServices() {
        return ctx.newObject("CmisServices", ws);
    }

    @Path("objects/{id}")
    public Resource getObject(@PathParam("id") String id) {
        return newObject("CmisObject", id, ws);
    }

    @Path("types/{id}")
    public Resource getType(@PathParam("id") String id) {
        return newObject("CmisType", id, ws);
    }

//    //TODO BUG in resteasy - dispatch method from superclass hides @Path annotations from this class
//    // need to redefine the method here
//    @Path("{segment}")
//    public Object dispatch(@PathParam("segment") String segment) {
//        return super.dispatch(segment);
//    }

}

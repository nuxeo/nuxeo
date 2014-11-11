/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.admin;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

@WebObject(type = "Group")
@Produces("text/html;charset=UTF-8")
public class Group extends DefaultObject {

    NuxeoGroup group;

    @Override
    protected void initialize(Object... args) {
        assert args != null && args.length > 0;
        group = (NuxeoGroup) args[0];
    }

    @GET
    public Object doGet() {
        return getView("index").arg("group", group);
    }

    @POST
    public Response doPost() {
        return redirect(getPrevious().getPath());
    }

    @PUT
    public Response doPut() {
        return redirect(getPath());
    }

    @DELETE
    public Response doDelete() throws Exception {
        UserManager userManager = Framework.getService(UserManager.class);
        userManager.deleteGroup(group);
        return redirect(getPrevious().getPath());
    }

    @POST
    @Path("@put")
    public Response simulatePut() {
        return doPut();
    }

    @GET
    @Path("@delete")
    public Response simulateDelete() throws Exception {
        return doDelete();
    }

}

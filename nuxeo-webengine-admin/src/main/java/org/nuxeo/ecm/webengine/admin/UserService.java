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

import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.NuxeoGroupImpl;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

@WebObject(type = "UserManager")
@Produces("text/html; charset=UTF-8")
public class UserService extends DefaultObject {

    @GET
    @POST
    public Object getIndex(@QueryParam("query") String query,
            @QueryParam("group") String group) throws Exception {
        if (query != null && !query.equals("")) {
            UserManager userManager = Framework.getService(UserManager.class);
            if (group != null) {
                List<NuxeoGroup> results = userManager.searchGroups(query);
                return getView("index").arg("groups", results);
            } else {
                List<NuxeoPrincipal> results = userManager.searchPrincipals(query);
                return getView("index").arg("users", results);
            }
        }
        return getView("index");
    }

    @Path("user/{user}")
    public Object searchUsers(@PathParam("user") String user) throws Exception {
        UserManager userManager = Framework.getService(UserManager.class);
        NuxeoPrincipal principal = userManager.getPrincipal(user);
        if (principal == null) {
            throw new WebResourceNotFoundException("User not found: " + user);
        }
        return newObject("User", principal);
    }

    @Path("group/{group}")
    public Object searchGroups(@PathParam("group") String group) throws Exception {
        UserManager userManager = Framework.getService(UserManager.class);
        // FIXME: find better name for it
        NuxeoGroup principal = userManager.getGroup(group);
        if (principal == null) {
            throw new WebResourceNotFoundException("Group not found: " + group);
        }
        return newObject("Group", principal);
    }

    @POST
    @Path("user")
    public Response postUser() throws Exception {
        HttpServletRequest req = ctx.getRequest();
        String username = req.getParameter("username");
        UserManager userManager = Framework.getService(UserManager.class);
        if (username != null && !username.equals("")) {
            NuxeoPrincipal user = userManager.getPrincipal(username);
            String[] selectedGroups;
            if (user != null) {
                // update
                user.setFirstName(req.getParameter("firstName"));
                user.setLastName(req.getParameter("lastName"));
                user.setPassword(req.getParameter("password"));

                selectedGroups = req.getParameterValues("groups");
                List<String> listGroups = Arrays.asList(selectedGroups);
                user.setGroups(listGroups);

                userManager.updatePrincipal(user);
            } else {
                // create
                user = new NuxeoPrincipalImpl(req.getParameter("username"));
                user.setFirstName(req.getParameter("firstName"));
                user.setLastName(req.getParameter("lastName"));
                user.setPassword(req.getParameter("password"));

                selectedGroups = req.getParameterValues("groups");
                List<String> listGroups = Arrays.asList(selectedGroups);
                user.setGroups(listGroups);

                userManager.createPrincipal(user);
            }
            return redirect(getPath() + "/user/" + user.getName());
        }
        // FIXME
        return null;
    }

    @POST
    @Path("group")
    public Response postGroup() throws Exception {
        String groupName = ctx.getRequest().getParameter("groupName");
        UserManager userManager = Framework.getService(UserManager.class);
        if (groupName != null && !groupName.equals("")) {
            NuxeoGroup group = new NuxeoGroupImpl(groupName);
            userManager.createGroup(group);
            return redirect(getPath() + "/group/" + group.getName());
        }
        // FIXME
        return null;
    }

    public List<NuxeoGroup> getGroups() throws Exception {
        return Framework.getService(UserManager.class).getAvailableGroups();
    }

    public List<NuxeoPrincipal> getUsers() throws Exception {
        return Framework.getService(UserManager.class).getAvailablePrincipals();
    }

}

/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.admin;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

@WebObject(type = "User")
@Produces("text/html;charset=UTF-8")
public class User extends DefaultObject {

    NuxeoPrincipalImpl principal;

    @Override
    protected void initialize(Object... args) {
        assert args != null && args.length > 0;
        principal = (NuxeoPrincipalImpl) args[0];
    }

    @GET
    public Object doGet() {
        return getView("index").arg("user", principal);
    }

    @POST
    public Response doPost() {
        return redirect(getPrevious().getPath());
    }

    @PUT
    public Response doPut() {
        UserManager userManager = Framework.getService(UserManager.class);
        HttpServletRequest req = ctx.getRequest();
        // update
        principal.setFirstName(req.getParameter("firstName"));
        principal.setLastName(req.getParameter("lastName"));
        principal.setPassword(req.getParameter("password"));
        principal.setEmail(req.getParameter("email"));

        String[] selectedGroups = req.getParameterValues("groups");
        List<String> listGroups = Arrays.asList(selectedGroups);
        principal.setGroups(listGroups);

        userManager.updatePrincipal(principal);
        return redirect(getPath());
    }

    @DELETE
    public Response doDelete() {
        UserManager userManager = Framework.getService(UserManager.class);
        userManager.deletePrincipal(principal);
        return redirect(getPrevious().getPath());
    }

    @POST
    @Path("@put")
    public Response simulatePut() {
        return doPut();
    }

    @GET
    @Path("@delete")
    public Response simulateDelete() {
        return doDelete();
    }

}

package org.nuxeo.ecm.webengine.admin;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.runtime.api.*;
import org.nuxeo.ecm.platform.usermanager.*;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

import java.util.Arrays;
import java.util.List;

@WebObject(type = "User")
@Produces("text/html; charset=UTF-8")
public class User extends DefaultObject {

    NuxeoPrincipal principal;

    @Override
    protected void initialize(Object... args) {
        assert args != null && args.length > 0;
        principal = (NuxeoPrincipal) args[0];
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
    public Response doPut() throws Exception {
        UserManager userManager = Framework.getService(UserManager.class);
        HttpServletRequest req = ctx.getRequest();
        // update
        principal.setFirstName(req.getParameter("firstName"));
        principal.setLastName(req.getParameter("lastName"));
        principal.setPassword(req.getParameter("password"));

        String[] selectedGroups = req.getParameterValues("groups");
        List<String> listGroups = Arrays.asList(selectedGroups);
        principal.setGroups(listGroups);

        userManager.updatePrincipal(principal);
        return redirect(getPath());
    }

    @DELETE
    public Response doDelete() throws Exception {
        UserManager userManager = Framework.getService(UserManager.class);
        userManager.deletePrincipal(principal);
        return redirect(getPrevious().getPath());
    }

    @POST
    @Path("@put")
    public Response simulatePut() throws Exception {
        return doPut();
    }

    @GET
    @Path("@delete")
    public Response simulateDelete() throws Exception {
        return doDelete();
    }

}

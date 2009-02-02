package org.nuxeo.ecm.webengine.admin;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.runtime.api.*;
import org.nuxeo.ecm.platform.usermanager.*;
import org.nuxeo.ecm.core.api.NuxeoGroup;

@WebObject(type = "Group")
@Produces("text/html; charset=UTF-8")
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


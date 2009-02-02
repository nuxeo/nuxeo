package org.nuxeo.ecm.webengine.admin;

import org.nuxeo.ecm.core.rest.DocumentRoot;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@WebObject(type = "Admin", guard = "user=Administrator")
@Produces("text/html; charset=UTF-8")
public class Main extends ModuleRoot {

    @Path("users")
    public Object getUserManagement() {
        return newObject("UserManager");
    }

    @Path("engine")
    public Object getEngine() {
        return newObject("Engine");
    }

    @Path("repository")
    public Object getRepository() {
        return new DocumentRoot(ctx, "/");
    }

    @GET
    public Object getIndex() {
        return getView("index");
    }

    @GET
    @Path("help")
    public Object getHelp() {
        return getTemplate("help/help.ftl");
    }

    @GET
    @Path("about")
    public Object getAbout() {
        return getTemplate("help/about.ftl");
    }

    // handle errors
    public Object handleError(WebApplicationException e) {
        if (e instanceof WebSecurityException) {
            return Response.status(401).entity(getTemplate("error/error_401.ftl")).build();
        } else if (e instanceof WebResourceNotFoundException) {
            return Response.status(404).entity(getTemplate("error/error_404.ftl")).build();
        } else {
            return super.handleError(e);
        }
    }

}

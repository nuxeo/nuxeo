package org.nuxeo.ecm.webdav;

import org.nuxeo.ecm.webdav.resource.RootResource;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

/**
 * Wire the JAX RS root classes into WebEngine.
 *
 * TODO: doesn't work currently.
 *
 * @author fermigier
 */
@WebObject(type = "JaxrsApiRoot")
@Produces("text/html;charset=UTF-8")
public class RootModule extends DefaultObject {

    @GET
    public String test() {
        return "OK";
    }

    @Path("dav")
    public Object dav(@Context HttpServletRequest request) {
        return new RootResource(request);
    }

}

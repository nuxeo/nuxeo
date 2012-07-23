package org.nuxeo.apidoc.browse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

@WebObject(type = "redirectWO")
@Produces("text/html")
public class RedirectResource extends DefaultObject {

    protected String orgDistributionId = null;

    protected String targetDistributionId = null;

    @Override
    protected void initialize(Object... args) {
        orgDistributionId = (String) args[0];
        targetDistributionId = (String) args[1];
        targetDistributionId = targetDistributionId.replace(" ", "%20");
    }

    @GET
    @Produces("text/html")
    public Object get() {
        return newLocation(targetDistributionId, null);
    }

    @GET
    @Produces("text/html")
    @Path("/{subPath:.*}")
    public Object catchAll(@PathParam("subPath")
    String subPath) {
        return newLocation(targetDistributionId, subPath);
    }

    protected Response newLocation(String target, String subPath) {
        String path = getPrevious().getPath();
        String url = ctx.getServerURL().append(path).append("/" + target).toString();
        if (subPath != null) {
            url = url + "/" + subPath;
        }
        return redirect(url);
    }

}

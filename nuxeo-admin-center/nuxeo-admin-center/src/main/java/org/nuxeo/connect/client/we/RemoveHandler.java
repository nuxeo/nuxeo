package org.nuxeo.connect.client.we;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.connect.update.Package;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * Provides REST bindings for {@link Package} removal.
 *
 * @author <a href="mailto:tm@nuxeo.com">Thierry Martins</a>
 */
@WebObject(type = "removeHandler")
public class RemoveHandler extends DefaultObject {

    protected static final Log log = LogFactory.getLog(RemoveHandler.class);

    @GET
    @Produces("text/html")
    @Path(value = "start/{pkgId}")
    public Object startInstall(@PathParam("pkgId") String pkgId,
            @QueryParam("source") String source) {

        try {
            PackageUpdateService pus = Framework.getLocalService(PackageUpdateService.class);
            pus.removePackage(pkgId);

            return getView("removeDone").arg("pkgId", pkgId).arg(
                    "source", source);
        } catch (Exception e) {
            log.error("Error during first step of installation", e);
            return getView("removeError").arg("e", e);
        }
    }

}

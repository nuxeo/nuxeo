package org.nuxeo.apidoc.browse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.SeamComponentInfo;
import org.nuxeo.ecm.webengine.model.WebObject;

@WebObject(type = "seamComponent")
public class SeamComponentWO extends NuxeoArtifactWebObject {


/*    @Override
    @GET
    @Produces("text/html")
    public Object doViewAggregated() throws Exception {
        return doGet(); // no aggregated view for Seam Components
    }
*/
    @GET
    @Produces("text/html")
    @Path(value = "introspection")
    public Object doGet() throws Exception {
        return getView("view").arg("seamComponent", getTargetComponentInfo());    }

    public SeamComponentInfo getTargetComponentInfo() {
        return getSnapshotManager().getSnapshot(getDistributionId(), ctx.getCoreSession()).getSeamComponent(nxArtifactId);
    }

    @Override
    public NuxeoArtifact getNxArtifact() {
        return getTargetComponentInfo();
    }

}

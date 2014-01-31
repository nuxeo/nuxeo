package org.nuxeo.segment.io.web;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.segment.io.SegmentIO;

@WebObject(type = "segmentIOScriptResource")
@Path("/segmentIO")
public class SegmentIOScriptResource extends ModuleRoot {

    @GET
    public Object script(@QueryParam("login") String login) {

        SegmentIO segmentIO = Framework.getLocalService(SegmentIO.class);

        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put("writeKey", segmentIO.getWriteKey());

        NuxeoPrincipal principal = (NuxeoPrincipal) getContext().getPrincipal();

        if (login!=null && principal.getName().equals(login)) {
            ctx.put("principal", principal);
        }
        return getView("script").args(ctx);
    }
}

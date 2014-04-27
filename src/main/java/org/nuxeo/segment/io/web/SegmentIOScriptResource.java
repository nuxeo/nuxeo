package org.nuxeo.segment.io.web;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.segment.io.SegmentIO;
import org.nuxeo.segment.io.SegmentIOUserFilter;

import com.github.segmentio.models.Providers;

@WebObject(type = "segmentIOScriptResource")
@Path("/segmentIO")
public class SegmentIOScriptResource extends ModuleRoot {

    protected static final Log log = LogFactory.getLog(SegmentIOScriptResource.class);

    @GET
    public Object anonymous() {
        // here the security context won't be initialized
        // so there is no need to try to find the user
        return buildScript(null);
    }

    @GET
    @Path("user/{login}")
    public Object signed(@PathParam("login")
    String login) {
        return buildScript(login);
    }

    protected String buildJsonProvidersOptions() {
        SegmentIO segmentIO = Framework.getLocalService(SegmentIO.class);
        Providers providers = segmentIO.getProviders();
        StringBuffer json = new StringBuffer("{");
        for (String pname : providers.keySet()) {
            json.append(pname);
            json.append(" : ");
            json.append(providers.get(pname).toString());
            json.append(" , ");
        }
        json.append("}");
        return json.toString();
    }

    protected String buildJsonBlackListedLogins() {
        SegmentIO segmentIO = Framework.getLocalService(SegmentIO.class);

        SegmentIOUserFilter filters = segmentIO.getUserFilters();
        StringBuffer json = new StringBuffer("[");
        if (filters != null) {
            if (!filters.isEnableAnonymous()) {
                String anonymous = filters.getAnonymousUserId();
                if (anonymous != null) {
                    json.append("'");
                    json.append(anonymous);
                    json.append("',");
                }
            }
            for (String login : filters.getBlackListedUsers()) {
                json.append("'");
                json.append(login);
                json.append("',");
            }
        }
        json.append("]");
        return json.toString();
    }

    protected Object buildScript(String login) {

        SegmentIO segmentIO = Framework.getLocalService(SegmentIO.class);

        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put("writeKey", segmentIO.getWriteKey());

        NuxeoPrincipal principal = (NuxeoPrincipal) getContext().getPrincipal();

        if (principal != null) {
            if (login == null) {
                ctx.put("principal", principal);
            } else if (principal.getName().equals(login)) {
                ctx.put("principal", principal);
            }
        }
        ctx.put("providers", buildJsonProvidersOptions());
        ctx.put("blackListedLogins", buildJsonBlackListedLogins());

        return getView("script").args(ctx);
    }
}

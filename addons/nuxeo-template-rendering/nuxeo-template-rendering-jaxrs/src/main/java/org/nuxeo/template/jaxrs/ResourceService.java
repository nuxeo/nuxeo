package org.nuxeo.template.jaxrs;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

/**
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
public class ResourceService extends AbstractResourceService {

    public ResourceService(CoreSession session) {
        super(session);
    }

    @GET
    @Path("name")
    @Produces(MediaType.TEXT_PLAIN)
    public String getName() {
        return super.getName();
    }

    @Context
    protected HttpServletRequest request;

    public String getRoot() throws Exception {
        CoreSession session = getCoreSession();
        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);
        List<TemplateSourceDocument> templates = tps.getAvailableTemplates(
                session, null);
        StringBuffer sb = new StringBuffer();

        sb.append("[");
        for (TemplateSourceDocument t : templates) {
            sb.append("{");
            sb.append("\"label\":" + "\"" + t.getLabel() + "\",");
            sb.append("\"name\":" + "\"" + t.getName() + "\",");
            sb.append("\"id\":" + "\"" + t.getId() + "\"");
            sb.append("},");
        }

        String result = sb.toString();
        result = result.substring(0, result.length() - 2) + "]";

        return result;
    }
}

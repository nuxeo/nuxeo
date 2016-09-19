package org.nuxeo.ecm.restapi.server.jaxrs.targetplatforms;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.platform.ui.web.restAPI.BaseNuxeoRestlet;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.restlet.data.Request;
import org.restlet.data.Response;

import java.io.IOException;

public class TargetPlatformsRestlet extends BaseNuxeoRestlet {
    @Override
    public void handle(Request request, Response response) {
        HttpServletResponse res = BaseNuxeoRestlet.getHttpResponse(response);
        HttpServletRequest req = BaseNuxeoRestlet.getHttpRequest(request);
        if (req == null || res == null || res.isCommitted()) {
            return;
        }

        try {
            res.sendRedirect(VirtualHostHelper.getBaseURL(req) + "api/v1/target-platforms/public");
        } catch (IOException e) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}

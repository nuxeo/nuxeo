package org.nuxeo.wss.handlers.fakews;

import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.servlet.WSSResponse;

public class FakeLists implements FakeWSHandler {

    public void handleRequest(FakeWSRequest request, WSSResponse response)
            throws WSSException {

        response.addRenderingParameter("request", request);

        if ("http://schemas.microsoft.com/sharepoint/soap/GetListCollection".equals(request.getAction())) {

            response.setRenderingTemplateName("GetListCollection.ftl");
        }



    }

}

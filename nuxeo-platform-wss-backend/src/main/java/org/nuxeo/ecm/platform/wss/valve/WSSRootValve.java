package org.nuxeo.ecm.platform.wss.valve;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WSSRootValve extends ValveBase {

    protected static final Log log = LogFactory.getLog(WSSRootValve.class);

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {

        String testValve = request.getParameter("testValve");

        if (testValve!=null) {
            log.info("Valve activated");
        }

    }

}

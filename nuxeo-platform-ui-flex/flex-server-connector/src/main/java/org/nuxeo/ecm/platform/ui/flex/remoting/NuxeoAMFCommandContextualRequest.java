package org.nuxeo.ecm.platform.ui.flex.remoting;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.servlet.ContextualHttpServletRequest;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

import flex.messaging.messages.CommandMessage;
import flex.messaging.messages.Message;

public class NuxeoAMFCommandContextualRequest extends
        ContextualHttpServletRequest {

    Message response;
    CommandMessage command;
    HttpServletRequest request;

    public NuxeoAMFCommandContextualRequest(HttpServletRequest servletRequest,CommandMessage commandMessage) {
        super(servletRequest);
        command = commandMessage;
        request=servletRequest;
    }


    @Override
    public void process() throws Exception {
        Principal principal = request.getUserPrincipal();
        if (principal!=null)
        {
            if (principal instanceof NuxeoPrincipal) {
                NuxeoPrincipal nuxeoUser = (NuxeoPrincipal) principal;
                Contexts.getEventContext().set("flexUser", nuxeoUser);
            }
        }

    }

    public Message processCommandMessage() throws ServletException, IOException {
        run();
        return response;
    }
}

package org.nuxeo.ecm.platform.ui.flex.remoting;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.ConversationPropagation;
import org.jboss.seam.core.Manager;
import org.jboss.seam.servlet.ContextualHttpServletRequest;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

import com.exadel.flamingo.service.seam.util.FlamingoUtils;

import flex.messaging.messages.AcknowledgeMessage;
import flex.messaging.messages.Message;
import flex.messaging.messages.RemotingMessage;

public class NuxeoAMFContextualRequest extends ContextualHttpServletRequest {

    protected static final String HEADER_CONVERSATION_ID = "conversationId";

    protected RemotingMessage remotingMessage;

    protected HttpServletRequest request;

    protected Message response;

    public NuxeoAMFContextualRequest(HttpServletRequest request) {
        super(request);
        this.request=request;
    }

    public NuxeoAMFContextualRequest(HttpServletRequest request,
            RemotingMessage remotingMessage) {
        super(request);
        this.remotingMessage = remotingMessage;
        this.request=request;
    }

    @Override
    public void process() throws Exception {

        String targetName = (remotingMessage.getSource() != null && remotingMessage.getSource().length() > 0) ? remotingMessage.getSource()
                : remotingMessage.getDestination();

        String methodName = remotingMessage.getOperation();
        Object[] args = (Object[]) remotingMessage.getBody();

        // Make type translation here
        Object result = FlamingoUtils.makeCall(targetName, methodName, args);

        response = new AcknowledgeMessage(remotingMessage);

        if (Manager.instance().isLongRunningConversation()) {
            response.getHeaders().put(HEADER_CONVERSATION_ID,
                    Manager.instance().getCurrentConversationId());
        } else {
            response.getHeaders().put(HEADER_CONVERSATION_ID, null);
        }
        response.setBody(result);
    }

    @Override
    protected void restoreConversationId() {
        ConversationPropagation.instance().setConversationId(
                String.valueOf(remotingMessage.getHeader(HEADER_CONVERSATION_ID)));

        Principal principal = request.getUserPrincipal();
        if (principal!=null)
        {
            if (principal instanceof NuxeoPrincipal) {
                NuxeoPrincipal nuxeoUser = (NuxeoPrincipal) principal;

                if (Contexts.isEventContextActive())
                    Contexts.getEventContext().set("flexUser", nuxeoUser);
            }
        }
    }

    @Override
    protected void handleConversationPropagation() {
    }

    public Message getResponse() {
        return response;
    }
}

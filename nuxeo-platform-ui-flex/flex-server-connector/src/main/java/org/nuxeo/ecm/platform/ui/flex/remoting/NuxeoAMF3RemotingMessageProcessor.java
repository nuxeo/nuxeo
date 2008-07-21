package org.nuxeo.ecm.platform.ui.flex.remoting;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exadel.flamingo.service.seam.amf.process.AMF3RemotingMessageProcessor;

import flex.messaging.messages.ErrorMessage;
import flex.messaging.messages.Message;
import flex.messaging.messages.RemotingMessage;

public class NuxeoAMF3RemotingMessageProcessor extends
        AMF3RemotingMessageProcessor {

    protected static final String HEADER_CONVERSATION_ID = "conversationId";

    protected HttpServletRequest servletRequest;

    private static final Log log = LogFactory.getLog(NuxeoAMF3RemotingMessageProcessor.class);

    public NuxeoAMF3RemotingMessageProcessor(HttpServletRequest servletRequest) {
        super(servletRequest);
        this.servletRequest=servletRequest;
    }

    public Message process(RemotingMessage remotingMessage) {

        Message response = null;
        try {
            NuxeoAMFContextualRequest contextualHttpServletRequest = new NuxeoAMFContextualRequest(servletRequest,remotingMessage);
            contextualHttpServletRequest.run();
            response = contextualHttpServletRequest.getResponse();

        } catch (Exception e) {
            log.error("Could not process remoting message: " + remotingMessage, e);
            response = new ErrorMessage(remotingMessage, e);
        }

        return response;
    }
}

package org.nuxeo.ecm.platform.ui.flex.remoting;

import javax.servlet.http.HttpServletRequest;

import com.exadel.flamingo.service.seam.amf.process.AMF3MessageProcessor;

import flex.messaging.messages.CommandMessage;
import flex.messaging.messages.Message;
import flex.messaging.messages.RemotingMessage;

public class NuxeoAMF3MessageProcessor extends AMF3MessageProcessor {

    protected HttpServletRequest servletRequest;

    public NuxeoAMF3MessageProcessor(HttpServletRequest servletRequest) {
        super(servletRequest);
        this.servletRequest=servletRequest;
    }

    @Override
    public Message process(Message amf3Message) {

        Message result = null;

        if (amf3Message instanceof CommandMessage) {
            result = new NuxeoAMF3CommandMessageProcessor(servletRequest).process((CommandMessage) amf3Message);
        } else if (amf3Message instanceof RemotingMessage) {
            result = new NuxeoAMF3RemotingMessageProcessor(servletRequest).process((RemotingMessage) amf3Message);
        }

        return result;
    }

}

package org.nuxeo.ecm.platform.ui.flex.remoting;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exadel.flamingo.flex.messaging.util.UUIDUtil;
import com.exadel.flamingo.service.seam.amf.process.AMF3CommandMessageProcessor;

import flex.messaging.messages.AcknowledgeMessage;
import flex.messaging.messages.CommandMessage;
import flex.messaging.messages.ErrorMessage;
import flex.messaging.messages.Message;

public class NuxeoAMF3CommandMessageProcessor extends AMF3CommandMessageProcessor {

    HttpServletRequest httpRequest;

    private static final Log log = LogFactory.getLog(NuxeoAMF3CommandMessageProcessor.class);

    public NuxeoAMF3CommandMessageProcessor(HttpServletRequest servletRequest) {
        super(servletRequest);
        httpRequest=servletRequest;
    }

    public Message process(final CommandMessage request) {

        Message response = null;
        try {
            response = new NuxeoAMFCommandContextualRequest(httpRequest,request).processCommandMessage();
        } catch (Exception e) {
            log.error("Could not process security operation: " + request, e);
            response = new ErrorMessage(request, e);
        }

        if (response == null) {
            response = new AcknowledgeMessage(request);
            // For SDK 2.0.1_Hotfix2.
            if (request.isSecurityOperation()) {
                response.setBody("success");
            }
        }

        // For SDK 2.0.1_Hotfix2.
        if ("nil".equals(request.getHeader(Message.DS_ID_HEADER))) {
            response.getHeaders().put(Message.DS_ID_HEADER,
                    UUIDUtil.randomUUID());
        }

        return response;
    }

}

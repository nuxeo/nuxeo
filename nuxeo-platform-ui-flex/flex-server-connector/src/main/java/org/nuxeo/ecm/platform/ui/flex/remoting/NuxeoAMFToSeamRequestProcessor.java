package org.nuxeo.ecm.platform.ui.flex.remoting;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.exadel.flamingo.flex.amf.AMF0Message;
import com.exadel.flamingo.flex.messaging.amf.io.AMF0Deserializer;
import com.exadel.flamingo.flex.messaging.amf.io.AMF0Serializer;

public class NuxeoAMFToSeamRequestProcessor {

    private static final NuxeoAMFToSeamRequestProcessor singleton = new NuxeoAMFToSeamRequestProcessor();

    public static NuxeoAMFToSeamRequestProcessor instance() {
        return singleton;
    }

    /**
     * Provides processing of request in a full set of Seam contexts.
     *
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @throws javax.servlet.ServletException If error occur
     * @throws java.io.IOException If error occur
     */
    public void process(final HttpServletRequest request,
            final HttpServletResponse response) throws ServletException,
            IOException {

        AMF0Deserializer deserializer = new AMF0Deserializer(new DataInputStream(request.getInputStream()));
        AMF0Message amf0Request = deserializer.getAMFMessage();

        NuxeoAMF0MessageProcessor amf0MessageProcessor = new NuxeoAMF0MessageProcessor(new NuxeoAMF3MessageProcessor(request));
        AMF0Message amf0Response = amf0MessageProcessor.process(amf0Request);

        response.setContentType("application/x-amf");
        AMF0Serializer serializer = new AMF0Serializer(new DataOutputStream(
                response.getOutputStream()));
        serializer.serializeMessage(amf0Response);
    }

}

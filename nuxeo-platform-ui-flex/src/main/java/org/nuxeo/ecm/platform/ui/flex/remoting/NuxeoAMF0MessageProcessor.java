package org.nuxeo.ecm.platform.ui.flex.remoting;

import java.util.Iterator;
import java.util.List;

import com.exadel.flamingo.flex.amf.AMF0Body;
import com.exadel.flamingo.flex.amf.AMF0Message;
import com.exadel.flamingo.flex.amf.AMF3Object;
import com.exadel.flamingo.flex.amf.process.IAMF3MessageProcessor;
import com.exadel.flamingo.flex.messaging.util.UUIDUtil;

import flex.messaging.messages.ErrorMessage;
import flex.messaging.messages.Message;

/**
 *
 * Simple duplication of Granite code to unable easier debugging
 *
 * @author tiry
 *
 */
public class NuxeoAMF0MessageProcessor {

    protected IAMF3MessageProcessor amf3MessageProcessor;

    public NuxeoAMF0MessageProcessor(IAMF3MessageProcessor amf3MessageProcessor) {
        this.amf3MessageProcessor = amf3MessageProcessor;
    }

    public AMF0Message process(AMF0Message amf0RequestMessage) {

        AMF0Message amf0ResponseMessage = new AMF0Message();
        amf0ResponseMessage.setVersion(amf0RequestMessage.getVersion());

        ErrorMessage loginError = null;
        String dsId = null;
        for (Iterator<AMF0Body> bodies = amf0RequestMessage.getBodies(); bodies.hasNext();) {
            AMF0Body requestBody = bodies.next();


            Object msg = ((List<?>) requestBody.getValue()).get(0);

            //ClassCastException: Cannot cast flex.messaging.messages.CommandMessage (id=187) to flex.messaging.messages.CommandMessage
            Message amf3RequestMessage =null;

            if (msg instanceof Message) {
                // dummy if : just for debug
                amf3RequestMessage = (Message) msg;
            }
            else
            {
                Class requestMessageClass = msg.getClass();
                ClassLoader cl1 = requestMessageClass.getClassLoader();
                Class [] ifaces1 = requestMessageClass.getInterfaces();
                Class expectedClass = Message.class;
                ClassLoader cl2 = expectedClass.getClassLoader();
                amf3RequestMessage = (Message) msg;
            }

            Message amf3ResponseMessage = null;
            if (loginError == null) {
                amf3ResponseMessage = amf3MessageProcessor.process(amf3RequestMessage);

                if ((amf3ResponseMessage instanceof ErrorMessage) && ((ErrorMessage) amf3ResponseMessage).loginError()) {
                    loginError = (ErrorMessage) amf3ResponseMessage;
                }

                // For SDK 2.0.1_Hotfix2+ (LCDS 2.5+).
                if ("nil".equals(amf3ResponseMessage.getHeader(Message.DS_ID_HEADER))) {
                    amf3ResponseMessage.getHeaders().put(
                            Message.DS_ID_HEADER,
                            (dsId == null ? (dsId = UUIDUtil.randomUUID()) : dsId));
                }
            } else {
                amf3ResponseMessage = loginError.copy(amf3RequestMessage);
            }
            AMF3Object data = new AMF3Object(amf3ResponseMessage);
            AMF0Body responseBody = new AMF0Body(
                    getResponseTarget(requestBody, amf3ResponseMessage), "", data, AMF0Body.DATA_TYPE_AMF3_OBJECT);
            amf0ResponseMessage.addBody(responseBody);
        }

        return amf0ResponseMessage;
    }

    protected static String getResponseTarget(AMF0Body requestBody, Message responseMessage) {
        if (responseMessage instanceof ErrorMessage) {
            return requestBody.getResponse() + "/onStatus";
        }
        return requestBody.getResponse() + "/onResult";
    }
}

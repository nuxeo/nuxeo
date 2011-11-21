/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.nuxeo.ecm.automation.client.RemoteException;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshalling;

/**
 * @author matic
 *
 */
public class ExceptionMarshaller implements JsonMarshaller<RemoteException> {

    @Override
    public String getType() {
        return "exception";
    }

    @Override
    public Class<RemoteException> getJavaType() {
        return RemoteException.class;
    }


    public static RemoteException readException(String content) throws Exception {
        JsonParser jp = JsonMarshalling.getFactory().createJsonParser(content);
        jp.nextToken(); // skip {
        return _read(jp);
    }

    @Override
    public RemoteException read(JsonParser jp) throws Exception {
        return _read(jp);
    }

    public static RemoteException _read(JsonParser jp) throws Exception {
        int status = 0;
        String type = null, message = null;
        Throwable cause = null;
        JsonToken tok = jp.nextToken();
        while (tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            tok = jp.nextToken();
            if ("status".equals(key)) {
                if (tok == JsonToken.VALUE_NUMBER_INT) {
                    status = jp.getIntValue();
                } else {
                    status = Integer.parseInt(jp.getText());
                }
            } else if ("type".equals(key)) {
                type = jp.getText();
            } else if ("message".equals(key)) {
                message = jp.getText();
            } else if ("cause".equals(key)) {
                cause = jp.readValueAs(Throwable.class);
            } 
            tok = jp.nextToken();
        }
        return new RemoteException(status, type, message, cause);
    }

    @Override
    public void write(JsonGenerator jg, RemoteException value) throws Exception {
        throw new UnsupportedOperationException();
    }

}

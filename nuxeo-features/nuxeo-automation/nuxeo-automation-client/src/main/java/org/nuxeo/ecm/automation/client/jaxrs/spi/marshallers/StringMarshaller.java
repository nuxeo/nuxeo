/*
 * Copyright (c) 2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo
 */
package org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;

/**
 * Marshaller for the default ObjectCodec for the java String class instances.
 *
 * @author ogrisel
 * @since 5.7
 */
public class StringMarshaller implements JsonMarshaller<String> {

    @Override
    public String getType() {
        return "string";
    }

    @Override
    public Class<String> getJavaType() {
        return String.class;
    }

    @Override
    public String read(JsonParser jp) throws Exception {
        jp.nextToken();
        jp.nextToken();
        return jp.getText();
    }

    @Override
    public void write(JsonGenerator jg, Object value) throws Exception {
        // wrap as a complex object to pass through the string input micro
        // parsing used for backward compatibility with OperationInput.
        jg.writeStartObject();
        jg.writeStringField("entity-type", getType());
        jg.writeStringField("value", value.toString());
        jg.writeEndObject();
    }

}

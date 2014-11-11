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

import java.util.Date;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;
import org.nuxeo.ecm.automation.client.model.DateParser;

/**
 * Marshaller for the default ObjectCodec for the java Date class instances.
 *
 * @author ogrisel
 * @since 5.7
 */
public class DateMarshaller implements JsonMarshaller<Date> {

    @Override
    public String getType() {
        return "date";
    }

    @Override
    public Class<Date> getJavaType() {
        return Date.class;
    }

    @Override
    public Date read(JsonParser jp) throws Exception {
        jp.nextToken();
        jp.nextToken();
        return DateParser.parseW3CDateTime(jp.getText());
    }

    @Override
    public void write(JsonGenerator jg, Object value) throws Exception {
        jg.writeStartObject();
        jg.writeStringField("entity-type", getType());
        jg.writeStringField("value", DateParser.formatW3CDateTime((Date) value));
        jg.writeEndObject();
    }

}

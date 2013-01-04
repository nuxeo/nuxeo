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
 * Marshaller for the default ObjectCodec for the java Number abstract class
 * instances.
 *
 * @author ogrisel
 * @since 5.7
 */
public class NumberMarshaller implements JsonMarshaller<Number> {

    @Override
    public String getType() {
        return "number";
    }

    @Override
    public Class<Number> getJavaType() {
        return Number.class;
    }

    @Override
    public Number read(JsonParser jp) throws Exception {
        jp.nextToken();
        jp.nextToken();
        return jp.readValueAs(Number.class);
    }

    @Override
    public void write(JsonGenerator jg, Object value) throws Exception {
        Number number = (Number) value;
        if (number instanceof Double || number instanceof Float) {
            jg.writeNumber(number.doubleValue());
        } else {
            jg.writeNumber(number.longValue());
        }
    }

}

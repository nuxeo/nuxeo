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
 * Marshaller for the default ObjectCodec for the java Boolean class instances.
 *
 * @author ogrisel
 * @since 5.7
 */
public class BooleanMarshaller implements JsonMarshaller<Boolean> {

    @Override
    public String getType() {
        return "boolean";
    }

    @Override
    public Class<Boolean> getJavaType() {
        return Boolean.class;
    }

    @Override
    public Boolean read(JsonParser jp) throws Exception {
        jp.nextToken();
        jp.nextToken();
        return jp.readValueAs(Boolean.class);
    }

    @Override
    public void write(JsonGenerator jg, Object value) throws Exception {
        // No need to escape
        jg.writeBoolean((Boolean) value);
    }

}

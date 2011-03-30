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

import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;

import net.sf.json.JSONObject;

/**
 * @author matic
 *
 */
public class PrimitiveMarshaller implements JsonMarshaller {

    @Override
    public String getType() {
        return "primitive";
    }

    @Override
    public Object read(JSONObject json) {
        String type = json.getString("type");
        if ("string".equals(type)) {
            return json.getString("value");
        }
        if ("boolean".equals(type)) {
            return json.getBoolean("value");
        }
        if ("integer".equals(type)) {
            return json.getInt("value");
        }
        if ("long".equals(type)) {
           return json.getLong("value");
        }
        if ("double".equals(type)) {
            return json.getDouble("value");
        }
        throw new UnsupportedOperationException(type + " is not a primitive type ");
   }

}

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

import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.client.jaxrs.model.DateUtils;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;

public class DateMarshaller implements JsonMarshaller {

    @Override
    public String getType() {
        return "date";
    }

    @Override
    public Object read(JSONObject json) {
        String value = json.getString("value");
        return DateUtils.parseDate(value);
    }

}

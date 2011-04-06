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

import java.util.Date;

import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.client.jaxrs.model.DateUtils;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;

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
    public String getReference(Date data) {
        return DateUtils.formatDate(data);
    }

    @Override
    public Date read(JSONObject json) {
        String value = json.getString("date");
        return DateUtils.parseDate(value);
    }


    @Override
    public void write(JSONObject object, Date date) {
        object.put("date", DateUtils.formatDate(date));
    }
}

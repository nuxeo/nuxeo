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
package org.nuxeo.ecm.automation.server.jaxrs.io.marshallers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.server.jaxrs.io.JsonMarshaller;


/**
 * @author matic
 *
 */
public class DateMarshaller implements JsonMarshaller<Date> {

    protected static final String TYPE = "date";
    
    @Override
    public String getType() {
        return TYPE;
    }
    
    @Override 
    public Class<Date> getJavaType() {
        return Date.class;
    }
    
    protected SimpleDateFormat newFormat() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");
    }
    
    @Override
    public Date resolveReference(String ref) {
       try {
            return newFormat().parse(ref);
        } catch (ParseException e) {
           throw new IllegalArgumentException("bad date format " + ref, e);
        }
     }

    @Override
    public String newReference(Date value) {
        return newFormat().format(value);
    }
    
    @Override
    public Date read(JSONObject json) {
        String data = (String)json.get(TYPE);
        try {
            return newFormat().parse(data);
        } catch (ParseException e) {
           throw new IllegalArgumentException("bad date format " + data, e);
        }
    }

    @Override
    public void write(JSONObject json, Object date) {
        String data = newFormat().format(date);
        json.put(TYPE, data);
    }
}

/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     tdelprat
 */
package org.nuxeo.ecm.automation.core.impl.adapters;

import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.TypeAdaptException;
import org.nuxeo.ecm.automation.TypeAdapter;
import org.nuxeo.ecm.automation.core.util.Properties;

public class JSONObjectToProperties implements TypeAdapter {

    @Override
    public Object getAdaptedValue(OperationContext ctx, Object objectToAdapt)
            throws TypeAdaptException {

        JSONObject json = (JSONObject) objectToAdapt;
        Map<String, String> map = new HashMap<String, String>();

        for (Object key : json.keySet()) {
            Object value = json.get(key);
            String strValue="";
            if (value instanceof JSONArray) {
                JSONArray array = (JSONArray) value;
                for (int i = 0; i<array.size(); i++) {
                    strValue = strValue + array.getString(i);
                    if (i<array.size()-1) {
                        strValue=strValue + ",";
                    }
                }
            } else {
                strValue = value.toString();
            }
            map.put((String)key, strValue);
        }
        return new Properties(map);
    }
}

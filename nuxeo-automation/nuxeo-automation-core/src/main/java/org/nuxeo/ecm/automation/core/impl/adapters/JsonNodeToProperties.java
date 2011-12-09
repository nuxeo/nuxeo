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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.TypeAdaptException;
import org.nuxeo.ecm.automation.TypeAdapter;
import org.nuxeo.ecm.automation.core.util.Properties;

public class JsonNodeToProperties implements TypeAdapter {

    @Override
    public Object getAdaptedValue(OperationContext ctx, Object objectToAdapt)
            throws TypeAdaptException {

        JsonNode json = (JsonNode) objectToAdapt;
        Map<String, String> map = new HashMap<String, String>();


        Iterator<Entry<String, JsonNode>> it = json.getFields();
        while (it.hasNext()) {
            Entry<String, JsonNode> entry = it.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            if (value.isArray()) {
                int size = value.size();
                if (size == 0) {
                    map.put(key, null);
                }
                else if (size == 1) {
                    map.put(key, value.get(0).getValueAsText());
                } else {
                    StringBuilder buf = new StringBuilder(size*32);
                    buf.append(value.get(0).getValueAsText());
                    for (int i=1; i<size; i++) {
                        buf.append(',').append(value.get(i).getValueAsText());
                    }
                    map.put(key, buf.toString());
                }
            } else {
                map.put(key, value.getValueAsText());
            }
        }
        return new Properties(map);
    }

}

/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Helper to set/get variables on a document that are stored in a facet.
 *
 * @since 5.6
 */
public class GraphVariablesUtil {

    private GraphVariablesUtil() {
    }

    protected static SchemaManager getSchemaManager() {
        return Framework.getService(SchemaManager.class);
    }

    private static JsonFactory getFactory() {
        JsonFactoryManager jsonFactoryManager = Framework.getService(JsonFactoryManager.class);
        return jsonFactoryManager.getJsonFactory();
    }

    /**
     * @since 7.2
     */
    public static Map<String, Serializable> getVariables(DocumentModel doc, String facetProp, boolean mapToJSON) {
        String facet = (String) doc.getPropertyValue(facetProp);
        Map<String, Serializable> map = new LinkedHashMap<String, Serializable>();
        if (StringUtils.isBlank(facet)) {
            return map;
        }
        CompositeType type = getSchemaManager().getFacet(facet);
        if (type == null) {
            return map;
        }
        boolean hasFacet = doc.hasFacet(facet);
        for (Field f : type.getFields()) {
            String name = f.getName().getLocalName();
            Serializable value = hasFacet ? doc.getPropertyValue(name) : null;
            if (value instanceof Calendar) {
                if (mapToJSON) {
                    value = DateParser.formatW3CDateTime(((Calendar) value).getTime());
                } else {
                    value = ((Calendar) value).getTime();
                }
            } else if (value instanceof Object[] && mapToJSON) {
                try {
                    Object[] objects = (Object[]) value;
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    JsonGenerator jg = getFactory().createJsonGenerator(out);
                    jg.writeStartArray();
                    for (Object object : objects) {
                        jg.writeString(type.encode(object));
                    }
                    jg.writeEndArray();
                    jg.flush();
                    jg.close();
                    value = out.toString("UTF-8");
                } catch (IOException e) {
                    throw new NuxeoException(e);
                }
            }
            if (mapToJSON) {
                map.put(name, value != null ? value.toString() : null);
            } else {
                map.put(name, value);
            }
        }
        return map;
    }

    public static Map<String, Serializable> getVariables(DocumentModel doc, String facetProp) {
        return getVariables(doc, facetProp, false);
    }

    public static void setVariables(DocumentModel doc, String facetProp, Map<String, Serializable> map) {
        setVariables(doc, facetProp, map, true);
    }

    /**
     * @since 7.2
     */
    public static void setVariables(DocumentModel doc, String facetProp, Map<String, Serializable> map, final boolean save) {
        if (map.containsKey(DocumentRoutingConstants._MAP_VAR_FORMAT_JSON)
                && (Boolean) map.get(DocumentRoutingConstants._MAP_VAR_FORMAT_JSON)) {
            Map<String, String> vars = new HashMap<String, String>();
            map.remove(DocumentRoutingConstants._MAP_VAR_FORMAT_JSON);
            for (String key : map.keySet()) {
                if (map.get(key) != null && !(map.get(key) instanceof String)) {
                    throw new NuxeoException(
                            "Trying to decode JSON variables: The parameter 'map' should contain only Strings as it contains the marker '_MAP_VAR_FORMAT_JSON' ");
                }
                vars.put(key, (String) map.get(key));
            }
            GraphVariablesUtil.setJSONVariables(doc, facetProp, vars, save);
        } else {
            String facet = null;
            try {
                facet = (String) doc.getPropertyValue(facetProp);
            } catch (PropertyNotFoundException e) {
                facet = facetProp;
            }
            if (StringUtils.isBlank(facet)) {
                return;
            }
            CompositeType type = getSchemaManager().getFacet(facet);
            if (type == null) {
                return;
            }
            boolean hasFacet = doc.hasFacet(facet);
            for (Field f : type.getFields()) {
                String name = f.getName().getLocalName();
                if (!map.containsKey(name)) {
                    continue;
                }
                Serializable value = map.get(name);
                if (value == null && !hasFacet) {
                    continue;
                }
                if (!hasFacet) {
                    doc.addFacet(facet);
                    hasFacet = true;
                }
                doc.setPropertyValue(name, value);
            }
            if (save) {
                CoreSession session = doc.getCoreSession();
                session.saveDocument(doc);
            }
        }
    }

    /**
     * Sets the variables of the workflow based on their JSON representation (especially for scalar lists).
     *
     * @param doc
     * @param facetProp
     * @param map
     * @param save
     * @since 5.9.3, 5.8.0-HF10
     */
    public static void setJSONVariables(DocumentModel doc, String facetProp, Map<String, String> map) {
        setJSONVariables(doc, facetProp, map, true);
    }


    /**
     * @since 7.2
     */
    public static void setJSONVariables(DocumentModel doc, String facetProp, Map<String, String> map, final boolean save) {
        // normally the variables in the map don't contain the schema prefix
        Properties jsonProperties = new Properties();
        String facet = null;
        try {
            facet = (String) doc.getPropertyValue(facetProp);
        } catch (PropertyNotFoundException e) {
            facet = facetProp;
        }
        if (StringUtils.isBlank(facet)) {
            return;
        }
        CompositeType type = getSchemaManager().getFacet(facet);
        if (type == null) {
        }
        boolean hasFacet = doc.hasFacet(facet);
        for (Field f : type.getFields()) {
            String name = f.getName().getLocalName();
            if (!map.containsKey(name)) {
                continue;
            }
            String value = map.get(name);
            if (value == null && !hasFacet) {
                continue;
            }
            if (!hasFacet) {
                doc.addFacet(facet);
                hasFacet = true;
            }
            jsonProperties.put(name, value);
        }
        CoreSession session = doc.getCoreSession();
        try {
            DocumentHelper.setJSONProperties(session, doc, jsonProperties);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        if (save) {
            session.saveDocument(doc);
        }
    }

}

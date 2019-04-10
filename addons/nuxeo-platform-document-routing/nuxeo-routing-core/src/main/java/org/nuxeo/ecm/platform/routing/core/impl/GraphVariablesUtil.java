/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper to set/get variables on a document that are stored in a facet.
 *
 * @since 5.6
 */
public class GraphVariablesUtil {

    private GraphVariablesUtil() {
    }

    protected static SchemaManager getSchemaManager() {
        return Framework.getLocalService(SchemaManager.class);
    }

    private static JsonFactory getFactory() {
        JsonFactoryManager jsonFactoryManager = Framework.getLocalService(JsonFactoryManager.class);
        return jsonFactoryManager.getJsonFactory();
    }

    /**
     * @since 7.2
     */
    public static Map<String, Serializable> getVariables(DocumentModel doc, String facetProp, boolean mapToJSON) {
        try {
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
                }
                if (mapToJSON) {
                    map.put(name, value != null ? value.toString() : null);
                } else {
                    map.put(name, value);
                }
            }
            return map;
        } catch (ClientException | IOException e) {
            throw new ClientRuntimeException(e);
        }
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
                    throw new ClientRuntimeException(
                            "Trying to decode JSON variables: The parameter 'map' should contain only Strings as it contains the marker '_MAP_VAR_FORMAT_JSON' ");
                }
                vars.put(key, (String) map.get(key));
            }
            GraphVariablesUtil.setJSONVariables(doc, facetProp, vars, save);
        } else {
            try {
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
            } catch (ClientException e) {
                throw new ClientRuntimeException(e);
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
        try {
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
            DocumentHelper.setJSONProperties(session, doc, jsonProperties);
            if (save) {
                session.saveDocument(doc);
            }
        } catch (IOException | ClientException e) {
            throw new ClientRuntimeException(e);
        }

    }

}
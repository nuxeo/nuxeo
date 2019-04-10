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

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Field;
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

    public static Map<String, Serializable> getVariables(DocumentModel doc,
            String facetProp) {
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
                Serializable value = hasFacet ? doc.getPropertyValue(name)
                        : null;
                map.put(name, value);
            }
            return map;
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public static void setVariables(DocumentModel doc, String facetProp,
            Map<String, Serializable> map) {
        try {
            String facet = (String) doc.getPropertyValue(facetProp);
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
            CoreSession session = doc.getCoreSession();
            session.saveDocument(doc);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

}

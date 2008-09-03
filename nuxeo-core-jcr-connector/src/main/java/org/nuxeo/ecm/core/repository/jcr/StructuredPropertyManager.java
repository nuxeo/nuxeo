/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;

import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.types.Type;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class StructuredPropertyManager {

    private static final Map<String, PropertyAccessor> registry = new HashMap<String, PropertyAccessor>();

    // Utility class.
    private StructuredPropertyManager() {
    }

    static {
        registerPropertyAccessor(BlobPropertyAccessor.TYPE, new BlobPropertyAccessor());
    }

    public static PropertyAccessor getPropertyAccessor(Property property) {
        return registry.get(property.getType().getName());
    }

    public static PropertyAccessor getPropertyAccessor(String property) {
        return registry.get(property);
    }

    public static void registerPropertyAccessor(String type, PropertyAccessor accessor) {
        registry.put(type, accessor);
    }

    public static void registerPropertyAccessor(Type type, PropertyAccessor accessor) {
        registry.put(type.getName(), accessor);
    }


    public static boolean write(Node node, Property property) throws Exception {
        Type type = property.getType();
        PropertyAccessor accessor = registry.get(type.getName());
        if (accessor != null) {
            accessor.write(node, property);
            return true;
        }
        return false;
    }

    public static boolean read(Node node, Property property) throws Exception {
        Type type = property.getType();
        PropertyAccessor accessor = registry.get(type.getName());
        if (accessor != null) {
            accessor.read(node, property);
            return true;
        }
        return false;
    }

}

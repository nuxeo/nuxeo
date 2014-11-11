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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.types;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TypeRegistry {

    private static final Log log = LogFactory.getLog(TypeRegistry.class);

    final Map<String, Type> types = new HashMap<String, Type>();


    public synchronized void addType(Type type) {
        if (log.isDebugEnabled()) {
            log.debug("Registering type: " + type);
        }
        String id = type.getId();
        // do not add twice a type
        if (!types.containsKey(id)) {
            types.put(id, type);
        }
    }

    public synchronized boolean hasType(String id) {
        return types.containsKey(id);
    }

    public synchronized Type removeType(String id) {
        if (log.isDebugEnabled()) {
            log.debug("Unregistering type: " + id);
        }
        return types.remove(id);
    }

    public synchronized Collection<Type> getTypes() {
        return Collections.unmodifiableCollection(types.values());
    }

    public Type getType(String id) {
        return types.get(id);
    }

}

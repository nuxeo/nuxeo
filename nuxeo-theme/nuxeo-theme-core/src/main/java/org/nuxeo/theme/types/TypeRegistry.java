/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.Registrable;

public final class TypeRegistry implements Registrable {

    private static final Log log = LogFactory.getLog(TypeRegistry.class);

    private final Map<String, Type> registry = new HashMap<String, Type>();

    private final Map<TypeFamily, List<String>> typeNames = new HashMap<TypeFamily, List<String>>();

    public synchronized void register(final Type type) {
        String typeName = type.getTypeName();
        TypeFamily typeFamily = type.getTypeFamily();
        String key = computeKey(typeFamily, typeName);
        if (registry.containsKey(key)) {
            log.warn("**** Overriding " + typeFamily + ": " + typeName);
        }
        registry.put(key, type);
        if (!typeNames.containsKey(typeFamily)) {
            typeNames.put(typeFamily, new ArrayList<String>());
        }
        typeNames.get(typeFamily).add(typeName);
        log.debug("Registered " + typeFamily + ": " + typeName);
    }

    public synchronized void unregister(final Type type) {
        String typeName = type.getTypeName();
        TypeFamily typeFamily = type.getTypeFamily();
        String key = computeKey(typeFamily, typeName);
        registry.remove(key);
        typeNames.get(typeFamily).remove(typeName);
        log.debug("Unregistered " + typeFamily + ": " + typeName);
    }

    public Type lookup(final TypeFamily typeFamily, final String name) {
        String key = computeKey(typeFamily, name);
        return registry.get(key);
    }

    public Type lookup(final TypeFamily typeFamily, final String ... names) {
        for(String name : names) {
            if(name == null) {
                continue;
            }
            String key = computeKey(typeFamily, name);
            Type type = registry.get(key);
            if(type != null) {
                return type;
            }
        }
        return null;

    }

    public List<String> getTypeNames(final TypeFamily typeFamily) {
        if (!typeNames.containsKey(typeFamily)) {
            return new ArrayList<String>();
        }
        return typeNames.get(typeFamily);
    }

    public List<Type> getTypes(final TypeFamily typeFamily) {
        List<Type> types = new ArrayList<Type>();
        if (typeNames.containsKey(typeFamily)) {
            for (String typeName : typeNames.get(typeFamily)) {
                types.add(lookup(typeFamily, typeName));
            }
        }
        return types;
    }

    private static String computeKey(final TypeFamily family, final String name) {
        return String.format("%s/%s", family, name);
    }

    public synchronized void clear() {
        Collection<Type> objects = new ArrayList<Type>();
        for (Type t : registry.values()) {
            objects.add(t);
        }
        for (Type t : objects) {
            unregister(t);
        }
        objects.clear();
        objects = null;
    }

}

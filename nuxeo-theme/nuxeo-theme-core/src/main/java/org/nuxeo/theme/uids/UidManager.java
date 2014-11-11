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

package org.nuxeo.theme.uids;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.nuxeo.theme.Registrable;

public final class UidManager implements Registrable {

    private final Map<Integer, Identifiable> registry = new HashMap<Integer, Identifiable>();

    public Object getObjectByUid(Integer uid) {
        return registry.get(uid);
    }

    public synchronized int register(final Identifiable object) {
        int uid = getFreeUid();
        registry.put(uid, object);
        object.setUid(uid);
        return uid;
    }

    public synchronized void unregister(final Identifiable object) {
        int uid = object.getUid();
        registry.remove(uid);
        object.setUid(null);
    }

    private synchronized int getFreeUid() {
        Random generator = new Random();
        generator.setSeed(0L);
        int uid;
        do {
            uid = generator.nextInt();
        } while (uid <= 0 || registry.containsKey(uid));
        return uid;
    }

    public synchronized void clear() {
        Collection<Identifiable> objects = new ArrayList<Identifiable>();
        for (Identifiable o : registry.values()) {
            objects.add(o);
        }
        for (Identifiable o : objects) {
            unregister(o);
        }
        objects = null;
        registry.clear();
    }

    public Collection<Integer> listUids() {
        return registry.keySet();
    }

}

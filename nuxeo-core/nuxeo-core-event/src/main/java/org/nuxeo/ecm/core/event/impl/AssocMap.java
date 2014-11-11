/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A map used to track duplicates.
 * <p>
 * This class is not synchronized on read; this means you need to populate the
 * map before using it.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AssocMap {

    protected final Map<String, Set<String>> peers = new HashMap<String, Set<String>>();


    public synchronized void put(String x, String y) {
        Set<String> px = peers.get(x);
        Set<String> py = peers.get(y);
        if (px == py) {
            return; // already associated
        }
        if (px == null) {
            py.add(x);
            peers.put(x, py);
        } else if (py == null) {
            px.add(y);
            peers.put(y, px);
        } else { // both members are already in relation with other members
            Set<String> set = new HashSet<String>();
            set.addAll(px);
            set.addAll(py);
            for (String key : set.toArray(new String[set.size()])) {
                peers.put(key, set);
            }
        }
    }

    public boolean isPeerOf(String x, String y) {
        Set<String> set = peers.get(x);
        return set != null && set.contains(y);
    }

}
